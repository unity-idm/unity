# Repository Guidelines

## Project Structure & Module Organization
- Source: Java modules under `*/src/main/java`; resources in `*/src/main/resources`
- Tests: `*/src/test/java` per module; cross‑module tests live in `integration-tests/`
- Packaging: `distribution/` assembles runnable bundles; artifacts land in `*/target/`
- Notable modules: `engine`, `engine-api`, `storage(-api)`, `std-plugins`, `rest`, `rest-admin`, `saml`, `oauth`, `vaadin-*`, `web-upman`, `unity-rest-api`, `documentation`, `console`

## Build, Test, and Development Commands
- Full build with unit tests: `mvn -T 1C clean install -Dunity.selenium.opts=--headless=new -Dgpg.skip=true` (parallel)
- Full build without unit tests: `mvn -T 1C clean install -DskipTests -Dgpg.skip=true` (parallel)
- Module build: `mvn -pl engine -am package -Dgpg.skip=true` (builds `engine` and dependencies)
- Unit tests: `mvn test` or `mvn -Dtest=ClassNameTest test -Dgpg.skip=true` (runs `*Test`)
- Integration tests: `mvn -pl integration-tests -am test -Dgpg.skip=true` (Selenium/JUnit; requires browser drivers/headless env)
- Whenever you are running full build w/ or w/o tests, redirect the output of the build to a file. In case the build fails, examine an output file to find the root cause, and address the issue. This is to optimize context window usage. Delete the output file when it is no longer needed.

## Coding Style & Naming Conventions & Gudeslines
- Language: Java 21; use tab indentation, make formatting consistent with current project files; UTF‑8 encoding
- Maximum line length: 120 characters
- Naming: classes `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`
- New packages: use `io.imunity`
- Imports: organize and avoid wildcards; keep visibility minimal
- Remove unused imports
- Extract larger blocks of code into well-named focused methods to enhance readability, maintainability, and testability of your code base
- The name of a function or a class should be inversely proportional to the size of the scope that contains it
- A variable name should be proportional to the size of the scope that contains it
- Favor private access modifiers, if not possible, then use default 'package' access. Fallback to public only in the last resort.
- Use Java Records (`record`) for immutable data transfer objects (DTOs)
- Favor immutability for objects where appropriate, especially for DTOs and configuration
- Include comments only for complex logic
- Strongly prefer a **package-by-feature** structure over package-by-layer

  **Example:** **PREFER THIS (Package-by-Feature)**
    ```
    io.imunity
      ├── posts                    # Feature: Posts
      │   ├── PostController.java  # Controller for Posts
      │   ├── PostService.java     # Service logic for Posts
      │   ├── PostRepository.java  # Data access for Posts
      │   ├── Post.java            # Domain/Entity for Posts
      │   └── dto                  # Data Transfer Objects specific to Posts
      │       ├── PostCreateRequest.java
      │       └── PostSummaryResponse.java
      │
      ├── users                    # Feature: Users
      │   ├── UserController.java
      │   ├── UserService.java
      │   ├── UserRepository.java
      │   └── User.java
      │
      └── common                   # Optional: Truly shared utilities/config
          └── exception
              └── ResourceNotFoundException.java
    ```
  **AVOID THIS (Package-by-Layer):**
    ```
    com.example.application
    ├── controller
    │   ├── PostController.java
    │   └── UserController.java
    │
    ├── service
    │   ├── PostService.java
    │   └── UserService.java
    │
    ├── repository
    │   ├── PostRepository.java
    │   └── UserRepository.java
    │
    └── model (or domain/entity)
        ├── Post.java
        └── User.java
    ```

## Testing Guidelines
- Frameworks: JUnit 5, AssertJ, Mockito; Selenium for UI/integration in `integration-tests/`
- Locations: unit tests beside code in each module; browser‑level tests only in `integration-tests/`
- Coverage: JaCoCo configured; add tests for new logic and regressions
- Naming: unit tests `*Test`; prefer descriptive test method names
- Examples: run a single class `mvn -Dtest=MyServiceTest test`
- Test Method Naming: Use the "should" prefix for test method names followed by a descriptive name of what the test is verifying. For example, use `shouldReturnUserWhenValidIdProvided()` instead of `testGetUserById()`
- Test Structure: Use the given/when/then pattern (BDD style) for structuring test methods instead of Arrange/Act/Assert.
- All new services, parsers, and transformation logic must have comprehensive test coverage
- All tests must pass before considering implementation complete
- Use parameterized tests for testing multiple scenarios
- In the case of parameterized tests, make the input argument a record with builder. The goal is to have the definition of Arguments cristal clear in terms of readability
- No Java Reflections in Tests: Avoid using Java reflections in autogenerated unit tests. Instead, use proper constructor injection, builder patterns, or factory methods to create test objects. Reflections make tests harder to understand, maintain, and can break when code changes
- Testing is not optional — it's a mandatory part of every development

## Commit & Pull Request Guidelines
- Commits: prefix with issue key when applicable (e.g., `UY-1527: fix NPE`); concise, imperative mood
- PRs: include summary, linked issues, affected modules, test notes; add screenshots for Vaadin/UI changes
- CI hygiene: lint locally, ensure `mvn -T 1C -DskipTests=false verify` passes before requesting review

## Security & Configuration Tips
- Do not commit secrets or local configs; prefer environment variables and example files
- Git ignores: `target/`, IDE metadata, and test logs are already excluded; keep it that way
- Vaadin: frontend is built via Maven plugin; no manual `npm` steps required unless developing Vaadin components

## Database Migration

When implementing features, it is crucial to consider whether the changes require a database migration. There are two types of migrations in this project: DDL migrations and data schema migrations for data stored in BLOBs.

### DDL Migration

DDL (Data Definition Language) migrations are required when there are changes to the database schema, such as creating, modifying, or deleting tables or columns.

**When is a DDL migration needed?**

*   Adding a new table.
*   Adding a new column to an existing table.
*   Modifying the data type of a column.
*   Renaming a table or a column.
*   Deleting a table or a column.

**How to perform a DDL migration:**

1.  **Create a new migration script:**
    *   Open the `storage/src/main/resources/pl/edu/icm/unity/store/rdbms/mapper/migration.xml` file.
    *   Add a new `<update>` element with a unique ID that follows the pattern `updateSchema-XXX-YY`, where `XXX` is the new schema version and `YY` is the script number.
    *   Write the SQL statements for the schema changes within the `<update>` element.
2.  **Update the schema version:**
    *   After the DDL changes, add another `<update>` element to update the schema version in the `UVOS_FLAG` table. The ID of this update should be `updateSchema-XXX-ZZ`, where `ZZ` is the next script number. The content should be `UPDATE UVOS_FLAG SET VAL = 'XXX';`.
3.  **Update the application's schema version:**
    *   Open the `storage/src/main/java/pl/edu/icm/unity/store/export/AppSchemaVersions.java` file and add a new enum value for the new schema version (e.g., `V_SINCE_4_1_0(21, "4.1.0")`). The first argument is the integer schema version, and the second is the human-readable application version.
    *   Open the `storage/src/main/java/pl/edu/icm/unity/store/AppDataSchemaVersion.java` file and update the `CURRENT` constant to the new version.

### Data Schema Migration (BLOBs)

Data schema migrations are required when there are changes to the structure or semantics of the Java objects that are serialized and stored in BLOBs.

**When is a data schema migration needed?**

*   Adding, removing, or renaming a field in a serialized Java object.
*   Changing the data type of a field in a serialized Java object.
*   Changing the serialization format (e.g., from XML to JSON).

**How to perform a data schema migration:**

There are two scenarios for data schema migration: in-place migration and backup-based migration. To avoid code duplication, the common migration logic should be externalized into a helper class.

1.  **Create a helper class:**
    *   Create a new Java class (e.g., `UpdateHelperToX_Y.java`) in the same package as the updaters.
    *   This class should contain the static methods that perform the actual data transformations.

#### In-Place Migration

This migration is performed when the application is started with an existing database that has an older schema version.

1.  **Determine the new version number:**
    *   The version numbers are sequential integers. To get a new version number, you need to look at the existing version numbers and choose the next one in the sequence.
    *   Check the `storage/src/main/java/pl/edu/icm/unity/store/export/AppSchemaVersions.java` file to see the latest version number.
    *   Increment the latest version number by one.
2.  **Create a new `InDBContentsUpdater` implementation:**
    *   Create a new Java class that implements the `pl.edu.icm.unity.store.migration.InDBContentsUpdater` interface.
    *   The class should be placed in a package that indicates the schema version it is migrating to (e.g., `storage/src/main/java/pl/edu/icm/unity/store/migration/to4_1/`).
    *   The class name should indicate the schema version it is migrating from (e.g., `InDBUpdateFromSchema20`).
3.  **Implement the `getUpdatedVersion()` method:**
    *   This method should return the schema version that the updater is migrating *from*. This is the previous version number before your changes.
4.  **Implement the `update()` method:**
    *   This method should use the helper class to perform the data migration. This typically involves:
        *   Retrieving the data from the database.
        *   Deserializing the data into Java objects.
        *   Calling the helper methods to transform the objects.
        *   Serializing the objects back to the BLOB format.
        *   Updating the data in the database.
5.  **Register the new updater:**
    *   The new updater needs to be registered with the `ContentsUpdater` class. This is done automatically by Spring's dependency injection, as long as the new class is annotated with `@Component`.
6.  **Update the application's schema version:**
    *   Open the `storage/src/main/java/pl/edu/icm/unity/store/export/AppSchemaVersions.java` file and add a new enum value for the new schema version (e.g., `V_SINCE_4_1_0(21, "4.1.0")`).
    *   Open the `storage/src/main/java/pl/edu/icm/unity/store/AppDataSchemaVersion.java` file and update the `CURRENT` constant to the new version.

#### Backup-based Migration

This migration is performed when restoring a database from a JSON dump.

1.  **Create a new `JsonDumpUpdate` implementation:**
    *   Create a new Java class that implements the `pl.edu.icm.unity.store.export.JsonDumpUpdate` interface.
    *   The class should be placed in the same package as the `InDBContentsUpdater` implementation for the same schema version.
2.  **Implement the `getUpdatedVersion()` method:**
    *   This method should return the schema version that the updater is migrating *from*.
3.  **Implement the `update()` method:**
    *   This method takes an `InputStream` (the JSON dump) and should return a modified `InputStream`.
    *   The implementation should read the JSON dump, use the helper class to transform the data in the JSON objects, and then write the modified JSON to a new `InputStream`.
4.  **Register the new updater:**
    *   The new updater needs to be registered. This is done automatically by Spring's dependency injection, as long as the new class is annotated with `@Component`.

By following these guidelines, you can ensure that database migrations are handled correctly and that the database schema remains consistent with the application's data model.