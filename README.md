# Grader — Hệ thống chấm bài lập trình tự động

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-4-FF6600?logo=rabbitmq&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Job-326CE5?logo=kubernetes&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)

Sinh viên nộp bài Java, hệ thống **đưa bài nộp vào hàng đợi** rồi chấm **bất đồng bộ** trong một
**Kubernetes Job cô lập** cho từng bài, kết quả trả về theo từng test case.

> Đây là đồ án môn học / dự án nghiên cứu cá nhân. Trọng tâm là phần **hạ tầng** (RabbitMQ,
> Kubernetes, Docker) và **luồng xử lý bất đồng bộ**, không phải một sản phẩm hoàn chỉnh.

## Điểm nhấn kỹ thuật

- **Bất đồng bộ & chịu lỗi**: bài nộp được publish vào RabbitMQ, worker tiêu thụ theo cơ chế
  at-least-once và kiểm tra `gradingRun.getStatus() == QUEUED` trước khi chấm, nên message bị
  redeliver **không gây chấm lặp**.
- **Cô lập mã không tin cậy**: mỗi bài nộp là một Kubernetes Job + ConfigMap riêng, tên job
  `grading-<submissionId>-<gradingRunId>`, chạy trong namespace `grader`.
- **Không deadlock khi đọc output**: dùng `ExecutorService` đọc song song `stdout` / `stderr`
  của process con, tránh buffer đầy làm tiến trình chấm bị treo.
- **Phân loại lỗi rõ ràng**: `COMPILE_ERROR`, `TIME_LIMIT_EXCEEDED`, `OUTPUT_LIMIT_EXCEEDED`,
  `RUNTIME_ERROR`, vượt giới hạn bộ nhớ (peak RSS) và `SYSTEM_ERROR`.

## Mục lục

- [Kiến trúc](#kiến-trúc)
- [Thành phần](#thành-phần)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Chạy nhanh với Docker Compose](#chạy-nhanh-với-docker-compose)
- [Chạy từng service khi phát triển](#chạy-từng-service-khi-phát-triển)
- [Kiểm thử](#kiểm-thử)
- [API chính](#api-chính)
- [Vòng đời một bài nộp](#vòng-đời-một-bài-nộp)
- [Xử lý lỗi của runner](#xử-lý-lỗi-của-runner)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Tài liệu thiết kế](#tài-liệu-thiết-kế)
- [Hạn chế đã biết và roadmap](#hạn-chế-đã-biết-và-roadmap)

## Kiến trúc

```
   React (Vite) ──▶ Nginx (80) ──/api/──▶ grader-backend (Spring Boot, 8080)
                                              │ publish {submissionId, gradingRunId}
                                              ▼
                                        RabbitMQ
                                  submission.exchange / submission.queue
                                              │ consume
                                              ▼
                                        grader-worker (Spring Boot)
                                              │ tạo Job + ConfigMap (request.json)
                                              ▼
                                  Kubernetes, namespace "grader"
                                  Job: grading-<submissionId>-<gradingRunId>
                                  image: grader-test-job:dev
                                              │
                                              ▼
                                        grader-runner
                                  biên dịch + chạy test case → in JSON kết quả

   PostgreSQL ◀── backend / worker / kết quả chấm
```

## Thành phần

| Thư mục | Mô tả | Công nghệ |
|---|---|---|
| `/` (root) | REST API, xác thực JWT, quản lý khóa học / bài tập / bài nộp, publish message | Java 21, Spring Boot 4.1.1, Spring Security, Spring AMQP, JPA |
| `grader-worker/` | Consumer của `submission.queue`; tạo Kubernetes Job + ConfigMap cho từng bài nộp | Spring Boot 4.1.1, fabric8 Kubernetes client |
| `grader-runner/` | Chạy bên trong Job: đọc `/input/request.json`, biên dịch, chạy test case, in JSON kết quả | Java 21, `ProcessBuilder`, Jackson |
| `frontend/` | Giao diện React, build rồi phục vụ qua Nginx (reverse proxy `/api/`) | React 19, TypeScript 5.7, Vite 6 |
| `docs/` | Use case (PlantUML), ERD, spec, Postman collection e2e | — |

## Yêu cầu môi trường

- Java 21 (đã kèm Maven Wrapper `./mvnw`, không cần cài Maven)
- Docker + Docker Compose
- Một cluster Kubernetes truy cập được (dự án đang dùng `kind`) và image `grader-test-job:dev`
  đã được nạp vào cluster, namespace `grader`
- Node.js 22+ (chỉ cần khi phát triển frontend)

## Chạy nhanh với Docker Compose

```bash
# 1. Tạo file biến môi trường (file .env KHÔNG được commit)
cp .env.example .env

# 2. Khởi động toàn bộ stack: API, worker, frontend, PostgreSQL, RabbitMQ, pgAdmin
docker compose -f docker-compose.dev.yaml up --build
```

| Service | Địa chỉ |
|---|---|
| Frontend | http://localhost |
| Backend API | http://localhost:8080 |
| RabbitMQ Management | http://localhost:15672 (`guest` / `guest`) |
| pgAdmin | http://localhost:5050 |
| PostgreSQL | `localhost:5432` (db: `grader`) |

Dữ liệu mẫu: xem `docs/seed/`.

## Chạy từng service khi phát triển

```bash
# Backend (cổng 8080, cần PostgreSQL + RabbitMQ đang chạy)
./mvnw spring-boot:run

# Worker (consumer RabbitMQ, tạo Kubernetes Job)
cd grader-worker && ./mvnw spring-boot:run

# Frontend (Vite dev server, proxy /api/ về backend)
cd frontend && npm install && npm run dev
```

## Kiểm thử

```bash
./mvnw test                        # backend: controller + service test
cd grader-worker && ./mvnw test    # worker: grading service test
cd frontend && npm run build       # type-check + build production
```

`grader-runner` hiện **chưa có test tự động** (xem mục roadmap).

## API chính

24 endpoint trên 8 controller, tất cả (trừ `/api/auth/login`) đều yêu cầu JWT.

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/auth/login` | Đăng nhập, trả access/refresh token |
| GET/POST | `/api/users` | Quản lý người dùng (admin) |
| GET/PATCH/DELETE | `/api/courses`, `/api/courses/{id}` | Danh sách / tạo / sửa / xóa khóa học |
| GET/POST | `/api/courses/{id}/members` | Thành viên khóa học |
| GET/POST | `/api/courses/{courseId}/assignments` | Bài tập của khóa học |
| GET/PATCH/DELETE | `/api/assignments/{assignmentId}` | Chi tiết / sửa / xóa bài tập |
| GET/POST/PATCH/DELETE | `/api/assignments/{assignmentId}/tests` | Test case (ẩn với sinh viên), kèm `/tests/stats` |
| POST | `/api/assignments/{assignmentId}/submissions` | Nộp bài (đẩy vào hàng đợi) |
| GET | `/api/assignments/{assignmentId}/submissions` | Danh sách bài nộp |
| GET | `/api/submissions/{submissionId}` | Chi tiết bài nộp |
| GET | `/api/submissions/{submissionId}/tests` | Kết quả từng test case |

Kiểm thử end-to-end: import `docs/postman/grader_e2e_collection.json` vào Postman.

## Vòng đời một bài nộp

1. Client gọi `POST /api/assignments/{id}/submissions` kèm JWT.
2. Backend lưu `Submission` + `GradingRun` (trạng thái `QUEUED`) và publish
   `{submissionId, gradingRunId}` vào `submission.exchange`.
3. `grader-worker` consume `submission.queue`, kiểm tra grading run còn `QUEUED` rồi mới xử lý.
4. Worker tạo ConfigMap `request.json` và Job `grading-<submissionId>-<gradingRunId>` trong
   namespace `grader`.
5. Pod chạy `grader-runner`: đọc `/input/request.json`, biên dịch, chạy từng test case và in
   JSON kết quả ra `stdout`.
6. Worker đọc kết quả, ghi `submission_test`, cập nhật trạng thái submission và grading run
   (`FINISHED` / `FAILED` kèm `finishedAt`).

## Xử lý lỗi của runner

| Trạng thái | Nguyên nhân |
|---|---|
| `COMPILE_ERROR` | Code không biên dịch được |
| `TIME_LIMIT_EXCEEDED` | Biên dịch quá 4s, hoặc chạy quá `timeLimitMs` (mặc định 4000ms) |
| `OUTPUT_LIMIT_EXCEEDED` | In quá nhiều ra `stdout` |
| `RUNTIME_ERROR` | Exit code khác 0 |
| Vượt giới hạn bộ nhớ | Peak RSS (đo bằng `/usr/bin/time -f %M`) vượt `memoryLimitKib` |
| `SYSTEM_ERROR` | Lỗi hệ thống khi đọc kết quả |

Ở tầng worker, mọi exception khi chấm đều được ghi nhận: submission chuyển `SYSTEM_ERROR`,
grading run chuyển `FAILED` kèm `finishedAt` trước khi exception được ném lại.

## Cấu trúc thư mục

```
.
├── src/                    # backend Spring Boot: auth, user, course, assignment, submission, test
├── grader-worker/          # consumer RabbitMQ + tạo Kubernetes Job
├── grader-runner/          # chương trình chạy trong Job: biên dịch & chấm
├── frontend/               # React + Vite + Nginx
├── docs/
│   ├── specs/              # đặc tả use case (create_course, create_submission, grading_run, k8s…)
│   ├── usecase/            # sơ đồ use case PlantUML
│   ├── database.puml       # ERD
│   ├── postman/            # collection e2e
│   └── seed/               # script dữ liệu mẫu
├── docker-compose.dev.yaml
└── Dockerfile.backend.yaml
```

## Tài liệu thiết kế

- [`docs/Scope.md`](docs/Scope.md) — phạm vi và mục tiêu dự án
- [`docs/database.puml`](docs/database.puml) — ERD (8 bảng)
- [`docs/usecase/`](docs/usecase) — sơ đồ use case
- [`docs/specs/`](docs/specs) — đặc tả chi tiết từng luồng (submission, grading run, k8s)

## Hạn chế đã biết và roadmap

- **Chưa theo dõi trạng thái Kubernetes Job.** `KubernetesJobService` chỉ tạo Job rồi trả về;
  nếu pod bị `OOMKilled` / evict / image pull lỗi thì submission có thể kẹt ở `RUNNING`.
  Hướng khắc phục: watch Job status qua fabric8 `waitUntilCondition(...)` và thêm
  `activeDeadlineSeconds`.
- **Chưa có dead-letter queue** cho message không xử lý được.
- **Chưa có CI/CD** cho repo này.
- Chỉ hỗ trợ Java; cô lập ở mức container, chưa sandbox ở mức syscall.
- `grader-runner` chưa có test tự động.

Các hạng mục trên được theo dõi ở tab **Issues** của repo.

## Liên hệ

Nguyễn Trung Kiên — nguyentrungkienhp202@gmail.com
