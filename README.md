# Weight Tracker (CS-499 Capstone Artifact)

Android app (Kotlin, SQLite) originally built in CS-360 in Spring 2026. This branch contains the **Algorithms and Data Structures** enhancement, built on top of the Software Design enhancement.

## What this branch adds
- `WeightAnalytics`: cleaning (skip invalid, dedupe by date, sort), O(n) rolling average, goal-crossing detection, range summaries
- `DateRangeIndex`: sorted list with binary search, O(log n + k) per query
- Goal check in `WeightService.addEntry` now detects skipping past the goal
- Summary line on the main screen (7-entry average, last 30 days)
- JUnit tests and an instrumented benchmark comparing binary search with an indexed SQLite query

## Benchmark (emulator)
| Entries | Memory setup | Memory per query | SQLite per query |
|---|---|---|---|
| 1,000 | 72.1 ms | 54.6 µs | 107.7 µs |
| 10,000 | 42.8 ms | 16.0 µs | 67.2 µs |
| 100,000 | 363.1 ms | 14.7 µs | 83.1 µs |

## Branches
- `software-design`: layered architecture, security and validation
- `algorithms-and-data-structures`: this branch (includes the above)

## Run the tests
Open in Android Studio. Run the `domain` tests as local unit tests, and `RangeBenchmarkTest` on an emulator.
