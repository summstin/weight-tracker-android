# Weight Tracker (CS-499 Capstone Artifact)

Android app (Kotlin, SQLite) originally built in CS-360 in Spring 2026. This branch contains the **Software Design and Engineering** enhancement, with a focus on structure, security and testability.

## What this branch changes

**Layered structure**
- `data`: `WeightRepository` interface and `SqliteWeightRepository`, plus `DatabaseHelper`
- `domain`: `WeightService`, `AuthService`, `GoalTracker`, `InputValidator` and a `GoalNotifier` interface (pure Kotlin, no Android classes)
- `ui`: `WeightViewModel` (coroutines and `StateFlow`), `WeightAdapter` (RecyclerView with `DiffUtil`), `SmsGoalNotifier`
- `ServiceLocator` builds the objects in one place so every screen shares one database connection

**Security**
- Passwords are stored as salted PBKDF2-HMAC-SHA256 hashes (600,000 iterations, per-user random salt, constant-time comparison), not plain text
- Weights and goals belong to a user (`user_id`); every query that reads or changes an entry includes the user id, so one user cannot see or edit another's data
- Input validation: strict ISO dates that are not in the future, weights between 20 and 1000 lbs, username, password and phone number rules
- SMS permission is requested only when the user adds a phone number for goal texts

**Data and migration**
- Weights are stored as numbers (`REAL`) instead of text
- Foreign keys with `ON DELETE CASCADE`, and an index on `(user_id, date)`
- A real version 1 to version 2 migration that keeps existing users, weights and the saved goal and hashes old plain-text passwords, instead of dropping the tables

**Testing**
- JUnit tests for `WeightService`, `AuthService`, `GoalTracker`, `InputValidator` and `PasswordHasher`
- `FakeWeightRepository` and `FakeGoalNotifier` let the service logic run without a database or an emulator

## Branches
- `software-design`: this branch
- `algorithms-and-data-structures`: builds on this branch with analytics, goal-crossing detection and a benchmark

## Run the tests
Open the project in Android Studio and run the tests in `app/src/test` as local unit tests.
