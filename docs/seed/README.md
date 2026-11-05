# Demo seed

Creates a small, believable dataset through the public HTTP API (no SQL, no code seeding).

```bash
docker compose -f docker-compose.dev.yaml up -d      # backend on :8080
python3 docs/seed/seed_demo.py

# other target
GRADER_API=http://localhost:8080/api python3 docs/seed/seed_demo.py

# show every request/response code
SEED_DEBUG=1 python3 docs/seed/seed_demo.py
```

## What you get

| Account | Role | Password |
| :--- | :--- | :--- |
| `lan.pham` | LECTURER | `demo1234` |
| `minh.tran`, `an.nguyen`, `thu.vo`, `duc.hoang` | STUDENT | `demo1234` |

- **CS201 — Data Structures**, owned by `lan.pham`, all four students enrolled.
- **LAB 3 — Linked list** (`PUBLISHED`, 3 attempts, 5 test cases: 2 public + 3 private).
- **LAB 4 — Binary search tree** (`DRAFT`, so the lecturer dashboard has a draft).
- One submission per student so the review queue has variety:
  `thu.vo` → `ACCEPTED` (10.0), `minh.tran` → wrong output, `an.nguyen` → wrong output,
  `duc.hoang` → `COMPILE_ERROR`.

The script is idempotent: it reuses existing users / course / assignments by name and skips
students that already submitted. It attaches `lan.pham` to an existing `CS201` instead of
failing if that course code already exists in your database.

## Requirements it relies on

- `POST /api/users` accepts a plain `passwordHash` and BCrypt-encodes it (`UserService`).
- `POST /api/courses/{id}/members` enrols a student (added with the lecturer screens).
- Test cases carry `timeLimitMs` / `memoryLimitKib`; the entity default is **4 ms**, which
  would make every submission `TIME_LIMIT_EXCEEDED`, so the seed sets `2000` ms explicitly.
