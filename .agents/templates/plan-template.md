# Development Plan Template for AI Agent

## Purpose

Template for AI Agent to implement features with sufficient context and self-validation capabilities to achieve working code through iterative refinement.

## Core technologies & Versions
- Java
- Spring
- MyBatis for database access
- Maven

## Core Principles

1. **Context is King**: Include ALL necessary documentation, examples, and caveats
2. **Validation Loops**: Provide executable tests/builds the AI can run and fix
3. **Information Dense**: Use keywords and patterns from the codebase
4. **Progressive Success**: Start simple, validate, then enhance

---

## Goal

[What needs to be built - be specific about the end state and desires]

## Why

- [Business value and user impact]
- [Integration with existing features]
- [Problems this solves and for whom]

## What

[User-visible behavior and technical requirements]

### Success Criteria

- [ ] [Specific measurable outcomes]

## All Needed Context

### Documentation & References (list all context needed to implement the feature)

```yaml
# MUST READ - Include these in your context window
- url: [Spring Boot Documentation URL]
  why: [Specific sections/annotations you'll need]

- file: [src/main/java/io/imunity/example/ExampleService.java]
  why: [Pattern to follow, gotchas to avoid]

- doc: [MyBatis Documentation URL]
  section: [Specific section about mapper patterns]
  critical: [Key insight that prevents common errors]

- docfile: [docs/file.md]
  why: [docs that the user has pasted in to the project]
```

### Current Codebase Structure (run `tree -I 'target|node_modules|*.class' -L 3` in the root)

```bash
# Current project structure
```

### Desired Codebase Structure with files to be added and responsibility of file

```bash
# New files and their responsibilities
src/main/java/io/imunity/
├── [feature-name]/
│   ├── [FeatureName]Controller.java     # REST endpoints
│   ├── [FeatureName]Service.java        # Business logic
│   ├── [FeatureName]Repository.java     # Data access (if needed)
│   ├── [FeatureName].java               # Domain entity
│   ├── [FeatureName]CreateRequest.java
│   └── [FeatureName]Response.java
```

### Known Gotchas of our codebase

```java
// CRITICAL: Spring Boot requires Java 21
// Example: Use constructor injection, not @Autowired fields
// Example: MyBatis mappers must be in correct package structure
// Example: We use package-by-feature, not package-by-layer
// Example: Always use Records for DTOs, not classes with getters/setters
// Example: Transaction boundaries should be at service layer
// Example: Use SLF4J for logging, not System.out.println
```

### Data Migration Strategy

**CRITICAL**: Before proceeding, answer the following questions. If the answer to any of them is yes, you MUST include a data migration plan in the "Implementation Blueprint" section.

- Does this change involve adding, removing, or renaming a field in a serialized Java object stored in a BLOB? (e.g., `OAuthToken`)
  - **Answer**: [Yes/No]
- Does this change involve modifying the data type of a field in a serialized Java object?
  - **Answer**: [Yes/No]
- Does this change involve creating, modifying, or deleting a database table or column (DDL)?
  - **Answer**: [Yes/No]

If a data migration is needed, refer to the `AGENTS.md` file for detailed instructions on how to perform both "Data Schema Migration (BLOBs)" and "DDL Migration". Your plan must include:
- The new schema version number.
- The creation of `InDBContentsUpdater` and/or `JsonDumpUpdate` classes.
- Updates to `AppSchemaVersions.java` and `AppDataSchemaVersion.java`.
- A dedicated testing plan for the migration logic.


## Implementation Blueprint

### Data Models and Structure

Create the core data models ensuring type safety and consistency.

```java
// Examples:
// - Domain entities (MyBatis)
// - DTOs using Records
// - Request/Response objects
// - Validation annotations

public record FeatureCreateRequest(
    String name,
    FeatureDetails details
) {}

public record FeatureResponse(
    Long id,
    String name,
    LocalDateTime createdAt
) {}
```

### List of tasks to be completed to fulfill the Development Plan in the order they should be completed

```yaml
Task 1:
CREATE src/main/java/io/imunity/[feature]/[Feature].java:
  - MIRROR pattern from: src/main/java/io/imunity/similar/Similar.java
  - USE Java Records for immutable data
  - INCLUDE proper validation annotations

MODIFY src/main/java/io/imunity/existing/ExistingService.java:
  - FIND pattern: "public class ExistingService"
  - INJECT after constructor
  - PRESERVE existing method signatures
  - USE constructor injection pattern

Task 2:
CREATE src/main/java/io/imunity/[feature]/[Feature]Controller.java:
  - FOLLOW REST controller patterns from existing controllers
  - USE @RestController and @RequestMapping
  - IMPLEMENT proper error handling
  - INCLUDE Swagger annotations

...(...)

Task N:
...
```

### Per task pseudocode as needed added to each task

```java
// Task 1: Service Implementation
// Pseudocode with CRITICAL details - don't write entire code
@Service
public class FeatureService 
{
    private final FeatureRepository repository;
    
    // PATTERN: Always use constructor injection (see guidelines.md)
    public FeatureService(FeatureRepository repository) 
    {
        this.repository = repository;
    }
    
    public FeatureResponse createFeature(FeatureCreateRequest request) 
    {
        // GOTCHA: Always validate input first
        validateRequest(request);  // throws ValidationException
        
        // PATTERN: Use existing retry patterns for external calls
        Feature feature = Feature.builder()
            .name(request.name())
            .createdAt(LocalDateTime.now())
            .build();
            
        // CRITICAL: MyBatis requires specific mapper patterns
        Feature saved = repository.save(feature);
        
        // PATTERN: Always use Records for responses
        return new FeatureResponse(saved.id(), saved.name(), saved.createdAt());
    }
}
```

### Integration Points

```yaml
ROUTES:
  - add to: src/main/java/io/imunity/[feature]/FeatureController.java
  - pattern: "@RequestMapping('/api/v1/features')"

DEPENDENCIES:
  - add to: pom.xml if new dependencies needed
  - pattern: Spring starter dependencies
```

## Validation Loop

### Level 1: Compilation & Style

```bash
# Run these FIRST - fix any errors before proceeding
mvn clean compile                        # Compilation check
mvn checkstyle:check                     # Code style validation (if configured)
mvn spotbugs:check                       # Static analysis (if configured)

# Expected: BUILD SUCCESS. If errors, READ the error and fix.
```

### Level 2: Unit Tests - each new feature/file/function use existing test patterns

```java
// CREATE src/test/java/io/imunity/[feature]/FeatureServiceTest.java
@ExtendWith(MockitoExtension.class)
class FeatureServiceTest 
{
    @Mock
    private FeatureRepository repository;
    
    @InjectMocks
    private FeatureService service;
    
    @Test
    void shouldCreateFeatureSuccessfully() 
    {
        // Given
        var request = new FeatureCreateRequest("Test Feature", validDetails());
        var expectedFeature = Feature.builder().id(1L).name("Test Feature").build();
        when(repository.save(any(Feature.class))).thenReturn(expectedFeature);
        
        // When
        var result = service.createFeature(request);
        
        // Then
        assertThat(result.name()).isEqualTo("Test Feature");
        verify(repository).save(any(Feature.class));
    }
    
    @Test
    void shouldThrowValidationExceptionForInvalidInput() 
    {
        // Given
        var invalidRequest = new FeatureCreateRequest("", null);
        
        // When & Then
        assertThatThrownBy(() -> service.createFeature(invalidRequest))
            .isInstanceOf(ValidationException.class);
    }
    
    // Helper method to build test data
    private FeatureCreateRequest.FeatureDetails validDetails() 
    {
        return FeatureCreateRequest.FeatureDetails.builder()
            .description("Valid description")
            .build();
    }
}
```

```bash
# Run and iterate until passing:
mvn test -Dtest=FeatureServiceTest
# If failing: Read error, understand root cause, fix code, re-run
```

### Level 3: Integration Test

```java
// CREATE src/test/java/io/imunity/[feature]/FeatureControllerIntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class FeatureControllerIntegrationTest 
{
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateFeatureViaRestApi() 
    {
        // Given
        var request = new FeatureCreateRequest("Integration Test Feature", validDetails());
        
        // When
        var response = restTemplate.postForEntity("/api/v1/features", request, FeatureResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().name()).isEqualTo("Integration Test Feature");
    }
}
```

```bash
# Run integration tests:
mvn test -Dtest=*IntegrationTest
# If failing: Check application logs for stack trace
```

## Final Validation Checklist

- [ ] All tests pass: `mvn test`
- [ ] No compilation errors: `mvn clean compile`
- [ ] No checkstyle violations: `mvn checkstyle:check`
- [ ] Error cases handled gracefully
- [ ] Logs are informative but not verbose (use SLF4J)
- [ ] Database schema modified and entities created if needed
- [ ] Documentation updated if needed

---

## Anti-Patterns to Avoid

- ❌ Don't use field injection (@Autowired on fields) - use constructor injection
- ❌ Don't skip validation because "it should work"
- ❌ Don't ignore failing tests - fix them
- ❌ Don't use package-by-layer structure - use package-by-feature
- ❌ Don't catch generic Exception - be specific
- ❌ Don't use Lombok - use Java Records for DTOs
- ❌ Don't use System.out.println
- ❌ Don't put business logic in controllers - keep it in services
- ❌ Don't ignore the existing code patterns - follow them consistently
