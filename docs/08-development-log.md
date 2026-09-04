# StockFlow - V2 Development Log

## How to use this document

This file records why important changes were made, which files or modules were affected and how each step was validated. Its purpose is to make the project understandable after a long break and to provide enough context to continue development in another conversation without reconstructing prior decisions.

## Stage 0 - V2 baseline

### Starting point

The uploaded project is on the `main` branch. The last committed V1 revision is:

```text
6315edc feat: improve API documentation, errors and controller tests
```

The project Maven version at that revision is `1.0.0`. The existing Surefire reports record 49 tests, with 0 failures, 0 errors and 0 skipped tests.

No Git tag existed in the uploaded repository.

### Line ending observation

The uploaded ZIP initially appeared to contain 11 modified files. A Git comparison with end-of-line whitespace ignored showed no semantic differences. The changes were caused exclusively by LF and CRLF conversion.

V2 therefore introduces `.gitattributes` so source-controlled text files use predictable line endings regardless of the developer operating system.

### Version strategy

The V1 commit is preserved with tag:

```text
v1.0.0
```

V2 development starts from that exact commit on branch:

```text
v2
```

The Maven project version becomes:

```text
2.0.0-SNAPSHOT
```

The final V2 release will change the Maven version to `2.0.0` and receive a corresponding Git tag.

### Documentation introduced

| File | Purpose |
|---|---|
| `docs/00-project-overview.md` | Provides a fast mental model of the system |
| `docs/01-architecture.md` | Explains layers, request flow, transactions and target organization |
| `docs/07-v2-roadmap.md` | Defines V2 scope and stopping point |
| `docs/08-development-log.md` | Records the reasoning and validation behind each development stage |

### Validation

This stage changes no Java behavior or database schema. Validation consists of checking Git differences, validating that only intentional documentation/versioning files changed and running the existing test suite in the developer environment before the first V2 commit.

### Next stage

Stage 1 will refactor Product API contracts. `ProductRequest` is currently reused for create and update even though `ProductService.update()` modifies only `name` and `minimumStock`. The V2 contract will make writable fields explicit and tests will be updated before any broader package reorganization.
