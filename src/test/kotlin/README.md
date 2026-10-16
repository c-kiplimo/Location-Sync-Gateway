# Test Suite Documentation

This directory contains comprehensive tests for the Location Sync Gateway application, following the test pyramid approach with unit, integration, and end-to-end tests.

## Test Structure

```
src/test/kotlin/com/locationsync/gateway/
├── service/
│   ├── ai/
│   │   ├── QualityScoringServiceTest.kt         # Unit tests for quality scoring
│   │   └── validator/
│   │       ├── CoordinatesValidatorTest.kt       # Unit tests for coordinates validation
│   │       └── AddressValidatorTest.kt           # Unit tests for address validation
│   ├── ConflictDetectionServiceTest.kt          # Unit tests for conflict detection
│   └── UberallDataServiceTest.kt                # Unit tests for Uberall integration
├── integration/
│   ├── KafkaIntegrationTest.kt                  # Integration tests with Kafka (Testcontainers)
│   ├── DatabaseIntegrationTest.kt               # Integration tests with PostgreSQL
│   └── EndToEndTest.kt                          # Full stack integration tests
└── util/
    └── TestFixtures.kt                          # Test data builders and fixtures
```

## Test Categories

### 1. Unit Tests (70% of tests)

**Location**: `src/test/kotlin/com/locationsync/gateway/service/`

Fast, isolated tests for individual components with mocked dependencies.

**Examples:**
- `CoordinatesValidatorTest` - Tests coordinate validation logic
- `AddressValidatorTest` - Tests address completeness and format
- `QualityScoringServiceTest` - Tests quality score calculation
- `ConflictDetectionServiceTest` - Tests conflict detection with mocked services

**Key Features:**
- Use MockK for mocking dependencies
- Use Kotest matchers for assertions
- Fast execution (< 1 second per test)
- No external dependencies (no database, no Kafka)

**Example:**
```kotlin
@Test
fun `should give perfect score for valid coordinates`() {
    val coords = Coordinates(latitude = -1.286389, longitude = 36.817223)
    val issues = mutableListOf<QualityIssue>()

    val score = validator.validate(coords, issues)

    score shouldBe 100
    issues.shouldBeEmpty()
}
```

### 2. Integration Tests (25% of tests)

**Location**: `src/test/kotlin/com/locationsync/gateway/integration/`

Tests with real infrastructure (database, Kafka) using Testcontainers.

**Examples:**
- `KafkaIntegrationTest` - Tests Kafka producer/consumer pipeline
- `DatabaseIntegrationTest` - Tests repository operations with real PostgreSQL
- `CacheIntegrationTest` - Tests Redis caching layer

**Key Features:**
- Use Testcontainers for infrastructure
- Test actual integration points
- Verify data flow through system
- Slower execution (2-5 seconds per test)

**Example:**
```kotlin
@MicronautTest
@Testcontainers
class KafkaIntegrationTest {
    @Container
    val kafka: KafkaContainer = KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    )

    @Test
    fun `should publish and consume location snapshot event`() = runBlocking {
        val event = createTestEvent()
        producerService.sendSnapshot(event)
        delay(2000)
        // Verify message was consumed
    }
}
```

### 3. End-to-End Tests (5% of tests)

**Location**: `src/test/kotlin/com/locationsync/gateway/integration/EndToEndTest.kt`

Full system tests covering complete workflows from ingestion to resolution.

**Key Features:**
- Tests complete use cases
- Verifies all components work together
- Slowest execution (10-30 seconds per test)

## Test Utilities

### TestFixtures

**Location**: `src/test/kotlin/com/locationsync/gateway/util/TestFixtures.kt`

Provides reusable test data and builders:

**Pre-built Fixtures:**
```kotlin
// Perfect quality snapshot
val snapshot = TestFixtures.createPerfectSnapshot(locationId = 1001L)

// Minimal snapshot
val minimal = TestFixtures.createMinimalSnapshot()

// Poor quality snapshot
val poorQuality = TestFixtures.createPoorQualitySnapshot()

// Temporary closure snapshot
val closure = TestFixtures.createTemporaryClosureSnapshot()
```

**Coordinate Fixtures:**
```kotlin
val nairobi = TestFixtures.nairobiCoordinates()
val mombasa = TestFixtures.mombasaCoordinates()
val kisumu = TestFixtures.kisumuCoordinates()
```

**Address Fixtures:**
```kotlin
val kenyanAddr = TestFixtures.kenyanAddress()
val usAddr = TestFixtures.usAddress()
```

**Builder Pattern:**
```kotlin
val customSnapshot = snapshotPayload {
    locationId(1001L)
    name("Custom Location")
    coordinates(null)
    phone("+254712345678")
}
```

**Batch Data:**
```kotlin
// Create 100 test snapshots for load testing
val batch = TestFixtures.createBatchSnapshots(count = 100)
```

## Running Tests

### Run All Tests
```bash
./gradlew test

# Or on Windows
gradlew.bat test
```

### Run Specific Test Class
```bash
./gradlew test --tests CoordinatesValidatorTest
```

### Run Tests by Category
```bash
# Unit tests only
./gradlew test --tests "com.locationsync.gateway.service.*"

# Integration tests only
./gradlew test --tests "com.locationsync.gateway.integration.*"
```

### Run with Coverage
```bash
./gradlew test jacocoTestReport

# Open coverage report
open build/reports/jacoco/test/html/index.html
```

### Run in Continuous Mode
```bash
./gradlew test --continuous
```

## Test Configuration

**Location**: `src/test/resources/application-test.yml`

Test-specific configuration:
- Uses Testcontainers for database and Kafka
- Disables schedulers
- Mocks external services (Google OAuth, Uberall API)
- Enables debug logging
- Disables metrics collection

## Writing New Tests

### Unit Test Template

```kotlin
@DisplayName("MyService Tests")
class MyServiceTest {

    private lateinit var dependency: Dependency
    private lateinit var service: MyService

    @BeforeEach
    fun setup() {
        dependency = mockk()
        service = MyService(dependency)
    }

    @Nested
    @DisplayName("Feature X")
    inner class FeatureX {

        @Test
        fun `should do something when condition is met`() {
            // Given
            val input = createTestInput()
            every { dependency.method(any()) } returns expectedValue

            // When
            val result = service.processInput(input)

            // Then
            result shouldBe expectedResult
            verify { dependency.method(input) }
        }
    }
}
```

### Integration Test Template

```kotlin
@MicronautTest
@Testcontainers
@DisplayName("MyService Integration Tests")
class MyServiceIntegrationTest {

    @Container
    val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15")

    @Inject
    lateinit var service: MyService

    @Test
    fun `should persist data correctly`() = runBlocking {
        // Given
        val data = createTestData()

        // When
        service.save(data)

        // Then
        val retrieved = service.find(data.id)
        retrieved shouldBe data
    }
}
```

## Test Best Practices

### 1. Naming Convention
- Test methods use backticks with descriptive sentences
- Format: `should [expected behavior] when [condition]`
- Examples:
  - ✅ `should return empty list when all fields match`
  - ✅ `should detect phone number conflict`
  - ❌ `testPhoneConflict`

### 2. AAA Pattern
Always use Arrange-Act-Assert pattern:
```kotlin
@Test
fun `should calculate quality score`() {
    // Arrange (Given)
    val payload = createPerfectSnapshot()

    // Act (When)
    val score = service.calculateQualityScore(payload)

    // Assert (Then)
    score.overallScore shouldBe 100
}
```

### 3. Nested Test Groups
Use `@Nested` to group related tests:
```kotlin
@Nested
@DisplayName("Valid Coordinates")
inner class ValidCoordinates {
    // Tests for valid coordinate scenarios
}

@Nested
@DisplayName("Invalid Coordinates")
inner class InvalidCoordinates {
    // Tests for invalid coordinate scenarios
}
```

### 4. Test Fixtures
Use TestFixtures instead of duplicating test data:
```kotlin
// ❌ Bad - duplicated data
val coords = Coordinates(-1.286389, 36.817223)

// ✅ Good - reusable fixture
val coords = TestFixtures.nairobiCoordinates()
```

### 5. Assertions
Use Kotest matchers for readable assertions:
```kotlin
// ❌ Bad
assertEquals(100, score)

// ✅ Good
score shouldBe 100
issues.shouldBeEmpty()
result.shouldNotBeNull()
list shouldHaveSize 3
value shouldBeGreaterThan 50
```

### 6. Mocking
Use MockK for dependency mocking:
```kotlin
val mockService = mockk<QualityScoringService>()

// Stubbing
coEvery { mockService.calculateScore(any()) } returns mockScore

// Verification
coVerify(exactly = 1) { mockService.calculateScore(payload) }

// Relaxed mocks for simple cases
val relaxedMock = mockk<Service>(relaxed = true)
```

### 7. Async Testing
Use `runBlocking` for suspend functions:
```kotlin
@Test
fun `should handle async operations`() = runBlocking {
    val result = suspendingFunction()
    result shouldNotBe null
}
```

### 8. Test Isolation
Each test should be independent:
```kotlin
@BeforeEach
fun setup() {
    // Fresh setup for each test
    service = MyService()
}

@AfterEach
fun cleanup() {
    // Clean up resources
}
```

## Test Coverage Goals

- **Overall**: 80% line coverage
- **Service Layer**: 85% coverage
- **Domain Logic**: 90% coverage
- **Controllers**: 75% coverage

**Current Coverage:**
```bash
./gradlew test jacocoTestReport
```

View report at: `build/reports/jacoco/test/html/index.html`

## Continuous Integration

Tests run automatically on:
- Every commit (pre-commit hook)
- Pull requests (GitHub Actions)
- Merge to main branch

**CI Configuration**: `.github/workflows/test.yml`

## Troubleshooting

### Testcontainers Issues

**Problem**: Docker not available
```
Solution: Ensure Docker is running
docker info
```

**Problem**: Port conflicts
```
Solution: Testcontainers uses random ports automatically
No manual configuration needed
```

### Kafka Tests Timeout

**Problem**: Kafka tests taking too long
```kotlin
// Increase timeout for slow environments
delay(5000) // instead of delay(2000)
```

### Database Tests Fail

**Problem**: Schema not initialized
```yaml
# Enable Flyway in test config
flyway:
  datasources:
    default:
      enabled: true
```

## Performance Testing

For load testing and performance benchmarks:
```bash
# Run JMH benchmarks
./gradlew jmh

# K6 load tests (requires Docker)
make load-test
```

## References

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Kotest Matchers](https://kotest.io/docs/assertions/matchers.html)
- [MockK Documentation](https://mockk.io/)
- [Testcontainers](https://www.testcontainers.org/)
- [Micronaut Testing](https://docs.micronaut.io/latest/guide/#testing)

---

**Note:** Always run tests before committing code. Maintain test coverage above 80%.
