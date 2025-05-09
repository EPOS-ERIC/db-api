# EPOS Database API (DB-API)

## Overview
EPOS DB-API is a Java library that encapsulates all the logic for accessing and managing the EPOS metadata catalog. The library is designed with a plug-and-play approach, making it easy to integrate into any component that needs to interact with the EPOS database.

## Version
Current version: **3.2.3**

## Installation

### Maven
Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.epos-eu.ics-c</groupId>
    <artifactId>db-api</artifactId>
    <version>3.2.3</version>
</dependency>
```

After adding the dependency, reload your Maven project to download the library.

## Environment Configuration

The DB-API requires specific environment variables to function properly. These variables control database connections and persistence behavior.

### Required Environment Variables

#### Persistence Configuration
| Variable | Description | Default |
|----------|-------------|---------|
| `PERSISTENCE_NAME` | Name of the persistence unit to use | `EPOSDataModel` |
| `CONNECTION_POOL_MAX_SIZE` | Maximum number of connections that can be opened with the database | - |
| `CONNECTION_POOL_MIN_SIZE` | Minimum number of connections that must remain open with the database | - |
| `CONNECTION_POOL_INIT_SIZE` | Initial number of connections to open at startup | - |

#### Database Connection
You can configure the database connection using either a connection string or individual parameters:

**Option 1: Connection String**
| Variable | Description | Example |
|----------|-------------|---------|
| `POSTGRESQL_CONNECTION_STRING` | Full database connection URL | `jdbc:postgresql://localhost:5432/cerif` |

**Option 2: Individual Parameters**
| Variable | Description | Example |
|----------|-------------|---------|
| `POSTGRESQL_HOST` | URL where the database is reachable | `localhost:5432` |
| `POSTGRESQL_DBNAME` | Name of the database | `cerif` |
| `POSTGRESQL_USERNAME` | Username for database connection | `epos_user` |
| `POSTGRESQL_PASSWORD` | Password for database connection | `password` |

## Core Classes

The DB-API is built around a central interface and implementation classes for different entity types.

### EPOSDataModel Interface

The `EPOSDataModel<T>` interface provides a standard set of methods for interacting with entities in the database:

```java
public interface EPOSDataModel<T> {
    T getByUid(String uid);
    List<T> getAll();
    T save(T entity);
    T update(T entity);
    void delete(T entity);
    void deleteByUid(String uid);
    // Additional methods may be available
}
```

### Implementation Classes

The library provides specific implementations for each EPOS Data Model entity:

| Implementation Class | Entity Type | Description |
|----------------------|-------------|-------------|
| `DistributionDBAPI` | `Distribution` | Handles Distribution entities |
| `WebserviceDBAPI` | `WebService` | Handles WebService entities |
| `DatasetDBAPI` | `Dataset` | Handles Dataset entities |
| `PersonDBAPI` | `Person` | Handles Person entities |
| `OrganizationDBAPI` | `Organization` | Handles Organization entities |
| `SoftwareApplicationDBAPI` | `SoftwareApplication` | Handles Software Application entities |
| `SoftwareSourceCodeDBAPI` | `SoftwareSourceCode` | Handles Software Source Code entities |

## Usage Examples

### Basic Usage Pattern

The general usage pattern follows these steps:
1. Create an instance of the appropriate implementation class
2. Use the interface methods to interact with the database

### Example: Working with Distribution Entities

```java
// Create the implementation instance
EPOSDataModel<Distribution> distributionDB = new DistributionDBAPI();

// Create a new Distribution
Distribution distribution = new Distribution();
distribution.setUid("my-unique-id");
distribution.setTitle(List.of("Main Title", "Alternative Title"));
distribution.setDescription(List.of("This is a description of the distribution"));
distribution.setLicense("https://creativecommons.org/licenses/by/4.0/");

// Save the Distribution to the database
distributionDB.save(distribution);

// Retrieve a Distribution by its unique ID
Distribution retrievedDist = distributionDB.getByUid("my-unique-id");

// Update a Distribution
retrievedDist.setTitle(List.of("Updated Title"));
distributionDB.update(retrievedDist);

// Get all Distributions
List<Distribution> allDistributions = distributionDB.getAll();

// Delete a Distribution
distributionDB.delete(retrievedDist);
// Or delete by UID
distributionDB.deleteByUid("my-unique-id");
```

### Example: Working with WebService Entities

```java
// Create the implementation instance
EPOSDataModel<WebService> webServiceDB = new WebserviceDBAPI();

// Create a new WebService
WebService webService = new WebService();
webService.setUid("service-123");
webService.setName(List.of("Example Web Service"));
webService.setEndpoint("https://api.example.com/service");

// Save the WebService to the database
webServiceDB.save(webService);

// Retrieve and work with the WebService
WebService retrievedService = webServiceDB.getByUid("service-123");
```

## Connection Pooling

The DB-API uses connection pooling to efficiently manage database connections. This improves performance by reusing existing database connections rather than creating new ones for each database operation.

Connection pooling parameters (`CONNECTION_POOL_MAX_SIZE`, `CONNECTION_POOL_MIN_SIZE`, and `CONNECTION_POOL_INIT_SIZE`) should be tuned based on your application's requirements and traffic patterns.

## Entity Relationships

The EPOS data model includes relationships between different entity types. These relationships are automatically handled by the DB-API when you use the appropriate methods.

For example, when retrieving a `Distribution` entity that is related to a `Dataset`, the relationship is automatically resolved and the related `Dataset` is populated.

## Error Handling

The DB-API uses a consistent error handling approach. Database errors are wrapped in appropriate exceptions that provide meaningful information about the cause of the error.

It's recommended to include error handling in your code:

```java
try {
    EPOSDataModel<Distribution> distributionDB = new DistributionDBAPI();
    Distribution distribution = distributionDB.getByUid("non-existent-id");
} catch (Exception e) {
    // Handle the exception appropriately
    System.err.println("Error retrieving distribution: " + e.getMessage());
}
```

## Best Practices

1. **Connection Management**:
    - Configure appropriate connection pool sizes to match your application's needs
    - Always close or release resources when no longer needed

2. **Transaction Management**:
    - For operations involving multiple entities, consider using a transaction to ensure data consistency

3. **Performance**:
    - For bulk operations, batch your requests when possible
    - Use specific query methods rather than retrieving all entities and filtering in application code

## License

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.


## Support

For issues, questions, or contributions, please visit the [GitHub repository](https://github.com/epos-eu/db-api).