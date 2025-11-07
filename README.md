# SE333 Assignment 5 - Unit, Mocking and Integration Testing

[![SE333 CI](https://github.com/BobDaGecko/se333-assignment5/actions/workflows/SE333_CI.yml/badge.svg)](https://github.com/BobDaGecko/se333-assignment5/actions/workflows/SE333_CI.yml)

**NOTE:** Portions of this project were completed with assistance of generative AI. This includes compiling documentation and fixing configuration issues within the code base related to dependencies.

## Project Overview

This project demonstrates testing practices including:

- **Unit Testing**: Testing individual components in isolation using mocks and stubs
- **Integration Testing**: Testing how multiple components work together with real database connections
- **Continuous Integration**: Automated testing and quality checks using GitHub Actions
- **Static Analysis**: Code quality verification using Checkstyle
- **Code Coverage**: Test coverage analysis using JaCoCo

The project contains two main packages:

1. **Barnes & Noble Package**: A book purchasing system with database and process interfaces
2. **Amazon Package**: A shopping cart system with pricing rules and database persistence

## Testing Approach

### Part 1: Barnes & Noble Tests

- **Specification-based tests**: Test the behavior according to specifications (boundary values, null handling, multiple scenarios)
- **Structural-based tests**: Test internal implementation details (method calls, data flow, branching)

### Part 2: GitHub Actions CI/CD

- Automated testing on every push to main branch
- Static analysis with Checkstyle (non-blocking)
- Code coverage reporting with JaCoCo
- Artifacts uploaded for both reports

### Part 3: Amazon Package Tests

- **Integration Tests**: Test complete workflows with real database interactions
- **Unit Tests**: Test isolated components using mocks for external dependencies

## CI/CD Workflow

The GitHub Actions workflow automatically:

1. Runs Checkstyle for code quality analysis
2. Executes all JUnit tests
3. Generates JaCoCo code coverage reports
4. Uploads both Checkstyle and JaCoCo reports as artifacts

## Build Status

All tests pass successfully with comprehensive coverage across both packages.

## Running Tests Locally

```bash
mvn clean test
```

## Viewing Coverage Reports

After running tests, view the JaCoCo report:

```bash
open target/site/jacoco/index.html
```

## Project Structure

```
se333-assignment5/
├── .github/
│   └── workflows/
│       └── SE333_CI.yml       # GitHub Actions workflow
├── src/
│   ├── main/java/org/example/
│   │   ├── Amazon/            # Shopping cart system
│   │   │   ├── Cost/          # Pricing rules
│   │   │   └── ...
│   │   └── Barnes/            # Book purchasing system
│   └── test/java/org/example/
│       ├── AmazonIntegrationTest.java
│       ├── AmazonUnitTest.java
│       └── BarnesAndNobleTest.java
├── pom.xml                     # Maven configuration
├── .gitignore
└── README.md
```

## Notes

- All commits follow the required naming convention
- Tests achieve high coverage (aiming for 100% where possible)
- Both specification-based and structural-based tests are clearly labeled with `@DisplayName`
- Database is properly reset between integration tests to ensure isolation
