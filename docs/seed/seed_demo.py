#!/usr/bin/env python3
"""
Seed the grader with a demo dataset using only the public HTTP API.

Usage:
    docker compose -f docker-compose.dev.yaml up -d      # backend on :8080
    python3 docs/seed/seed_demo.py

Creates:
    - lecturer  lan.pham                                  / demo1234
    - students  minh.tran, an.nguyen, thu.vo, duc.hoang    / demo1234
    - course    CS201 Data Structures (lecturer owns it, all four students enrolled)
    - LAB 3 "Linked list"  PUBLISHED, 3 attempts, 2 public + 3 private tests
    - LAB 4 "Binary search tree" DRAFT (so the lecturer dashboard shows a draft)

The script is idempotent: it looks up existing users/courses/assignments by name
and reuses them instead of failing on the unique constraints.
"""

import json
import os
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

BASE = os.environ.get("GRADER_API", "http://localhost:8080/api")
DEBUG = os.environ.get("SEED_DEBUG") == "1"
PASSWORD = "demo1234"

LECTURER = "lan.pham"
STUDENTS = ["minh.tran", "an.nguyen", "thu.vo", "duc.hoang"]
COURSE = {
    "code": "CS201",
    "courseName": "Data Structures",
    "description": "Lists, trees, hashing, and the cost of each.",
}

ECHO_SOURCE = """import java.io.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String line;
    while ((line = br.readLine()) != null) {
      System.out.println(line);
    }
  }
}
"""

WRONG_SOURCE = """public class Main {
  public static void main(String[] args) {
    System.out.println("nope");
  }
}
"""

BROKEN_SOURCE = """public class Main {
  public static void main(String[] args) { syntax error }
}
"""

# Passes every seeded test: echoes input, prints EMPTY when there is no input.
GOOD_SOURCE = """import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    List<String> lines = new ArrayList<>();
    String line;
    while ((line = br.readLine()) != null) {
      lines.add(line);
    }
    if (lines.isEmpty()) {
      System.out.println("EMPTY");
      return;
    }
    for (String l : lines) {
      System.out.println(l);
    }
  }
}
"""

TESTS = [
    {"name": "sample-append", "input": "hello\nworld\n", "expectedOutput": "hello\nworld", "isPublic": True,
     "timeLimitMs": 2000, "memoryLimitKib": 262144},
    {"name": "sample-reverse", "input": "a\nb\n", "expectedOutput": "a\nb", "isPublic": True,
     "timeLimitMs": 2000, "memoryLimitKib": 262144},
    {"name": "edge-empty", "input": "", "expectedOutput": "EMPTY", "isPublic": False,
     "timeLimitMs": 2000, "memoryLimitKib": 262144},
    {"name": "duplicates", "input": "4\n4\n", "expectedOutput": "4\n4", "isPublic": False,
     "timeLimitMs": 2000, "memoryLimitKib": 262144},
    {"name": "stress", "input": "x\n" * 10, "expectedOutput": ("x\n" * 10).strip(), "isPublic": False,
     "timeLimitMs": 2000, "memoryLimitKib": 262144},
]


def call(method, path, body=None, token=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as res:
            raw = res.read().decode()
            if DEBUG:
                print(f"    {method} {path} -> {res.status}", file=sys.stderr)
            return res.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as err:
        raw = err.read().decode()
        if DEBUG:
            print(f"    {method} {path} -> {err.code} {raw[:160]}", file=sys.stderr)
        try:
            return err.code, (json.loads(raw) if raw else None)
        except json.JSONDecodeError:
            return err.code, raw


def ensure_user(username, role):
    """Requires SEED_TOKEN (a lecturer/admin JWT) once endpoint authorization is enabled."""
    token = os.environ.get("SEED_TOKEN")
    status, users = call("GET", "/users", token=token)
    if status != 200:
        sys.exit(
            f"cannot list users ({status}). Start the backend with authorization disabled, or\n"
            "export SEED_TOKEN=<a lecturer/admin access token> and run again."
        )
    for user in users:
        if user["username"] == username:
            print(f"  = user {username:<12} id={user['id']} ({user['role']})")
            return user["id"]
    status, user = call(
        "POST", "/users", {"username": username, "role": role, "passwordHash": PASSWORD}, token
    )
    if status not in (200, 201):
        sys.exit(f"cannot create user {username}: {status} {user}")
    print(f"  + user {username:<12} id={user['id']} ({user['role']})")
    return user["id"]


def login(username):
    status, res = call("POST", "/auth/login", {"username": username, "password": PASSWORD})
    if status != 200:
        sys.exit(f"login failed for {username}: {status} {res}")
    return res["accessToken"]


def ensure_course(lecturer_id, token):
    # Authenticated listing shows the lecturer's own courses; fall back to the public
    # listing when endpoint authorization is disabled for local development.
    status, courses = call("GET", "/courses", token=token)
    if status != 200:
        status, courses = call("GET", "/courses")
    for course in courses or []:
        if course["code"] == COURSE["code"]:
            print(f"  = course {COURSE['code']} id={course['id']}")
            call("POST", f"/courses/{course['id']}/members", {"userId": lecturer_id}, token)
            return course["id"]
    status, course = call("POST", "/courses", {"userId": lecturer_id, **COURSE}, token)
    if status not in (200, 201):
        sys.exit(f"cannot create course: {status} {course}")
    print(f"  + course {COURSE['code']} id={course['id']}")
    return course["id"]


def ensure_assignment(course_id, lecturer_id, code, title, status_name, description, days_open, days_close, token):
    status, assignments = call("GET", f"/courses/{course_id}/assignments", token=token)
    if status != 200:
        sys.exit(f"cannot list assignments ({status}): {assignments}")
    for assignment in assignments or []:
        if assignment["code"] == code:
            print(f"  = assignment {code} id={assignment['id']} ({assignment['assignmentStatus']})")
            return assignment["id"]

    now = datetime.now(timezone.utc).replace(tzinfo=None)
    body = {
        "userId": lecturer_id,
        "code": code,
        "title": title,
        "assignmentStatus": status_name,
        "description": description,
        "submissionLimit": 3,
        "openingTime": (now + timedelta(days=days_open)).isoformat(timespec="seconds"),
        "closingTime": (now + timedelta(days=days_close)).isoformat(timespec="seconds"),
        "allowedLanguages": ["JAVA"],
    }
    status, assignment = call("POST", f"/courses/{course_id}/assignments", body, token)
    if status not in (200, 201):
        sys.exit(f"cannot create assignment {code}: {status} {assignment}")
    print(f"  + assignment {code} id={assignment['id']} ({assignment['assignmentStatus']})")
    return assignment["id"]


def ensure_tests(assignment_id, token):
    status, existing = call("GET", f"/assignments/{assignment_id}/tests", token=token)
    have = {test.get("name") for test in existing or []}
    missing = [test for test in TESTS if test["name"] not in have]
    for test in missing:
        status, created = call("POST", f"/assignments/{assignment_id}/tests", test, token)
        if status not in (200, 201):
            sys.exit(f"cannot create test {test['name']}: {status} {created}")
    if missing:
        print(f"  + {len(missing)} test cases created (total now {len(have) + len(missing)})")
    else:
        print(f"  = {len(have)} test cases already present")


def submit(assignment_id, token, source, label):
    # Student listing is scoped to the caller, so this also keeps the seed idempotent.
    status, mine = call("GET", f"/assignments/{assignment_id}/submissions", token=token)
    if status == 200 and mine:
        print(f"  = {label} already has {len(mine)} submission(s)")
        return
    status, res = call("POST", f"/assignments/{assignment_id}/submissions", {"sourceCode": source}, token)
    if status not in (200, 201):
        print(f"  ! submission skipped for {label}: {status} {res}")
        return
    print(f"  + submission #{res['submissionId']} by {label} -> {res['status']}")


def main():
    print(f"Seeding {BASE}")
    lecturer_id = ensure_user(LECTURER, "LECTURER")
    student_ids = {name: ensure_user(name, "STUDENT") for name in STUDENTS}
    lecturer_token = login(LECTURER)

    course_id = ensure_course(lecturer_id, lecturer_token)
    for username, user_id in student_ids.items():
        status, members = call(
            "POST", f"/courses/{course_id}/members", {"userId": user_id}, lecturer_token
        )
        if status in (200, 201):
            print(f"  + enrolled {username}")
        elif status == 409:
            print(f"  = {username} already enrolled")
        else:
            sys.exit(f"cannot enroll {username}: {status} {members}")

    lab3 = ensure_assignment(
        course_id, lecturer_id, "LAB 3", "Linked list", "PUBLISHED",
        "Implement a singly linked list supporting APPEND, PREPEND, DELETE, REVERSE and PRINT.",
        days_open=-2, days_close=7, token=lecturer_token)
    ensure_tests(lab3, lecturer_token)
    ensure_assignment(
        course_id, lecturer_id, "LAB 4", "Binary search tree", "DRAFT",
        "Insert, search, height and in-order traversal for a BST. Draft: visible to staff only.",
        days_open=1, days_close=14, token=lecturer_token)

    tokens = {name: login(name) for name in STUDENTS}
    submit(lab3, tokens["minh.tran"], ECHO_SOURCE, "minh.tran")
    submit(lab3, tokens["thu.vo"], GOOD_SOURCE, "thu.vo")
    submit(lab3, tokens["an.nguyen"], WRONG_SOURCE, "an.nguyen")
    submit(lab3, tokens["duc.hoang"], BROKEN_SOURCE, "duc.hoang")

    print("\nDone. Sign in at http://localhost:3000 (or :80 via compose):")
    print(f"  lecturer  {LECTURER} / {PASSWORD}")
    print("  students  " + ", ".join(STUDENTS) + f" / {PASSWORD}")
    print(f"  course CS201 id={course_id}, assignment LAB 3 id={lab3}")


if __name__ == "__main__":
    main()
