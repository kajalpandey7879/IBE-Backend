# Internet Booking Engine Backend

A Spring Boot backend for a multi-tenant Internet Booking Engine (IBE) with PostgreSQL, GraphQL, and configurable tenant-driven behavior.

This service is designed to support multiple hotel brands or tenants from a single backend, while allowing each tenant to control parts of the booking experience through configuration stored in the database.

---

## 1. Project Overview

This backend powers the booking flow for a configurable hotel booking platform.

Current focus areas include:

* multi-tenant tenant configuration
* property metadata per tenant
* property image support
* calendar and pricing APIs
* GraphQL-based frontend integration

The system follows a hybrid approach:

* **stable platform fields** are exposed as typed GraphQL fields
* **tenant-specific configurable fields** are stored in `jsonb` and selectively exposed

This keeps the API predictable without sacrificing tenant-level flexibility.

---

## 2. Tech Stack

* **Java 21**
* **Spring Boot**
* **Spring Data JPA / Hibernate**
* **Spring GraphQL**
* **PostgreSQL**
* **Maven**
* **Lombok**
* **GraphQL Java Extended Scalars**

---

## 3. Key Concepts

### Multi-tenancy

Each tenant represents a hotel brand or client.

A tenant has:

* core metadata such as tenant name and domain
* a JSON configuration blob stored in PostgreSQL `jsonb`
* associated properties

### Configurability

Not every field should become a hardcoded Java class.

This backend treats fields in two buckets:

#### Stable fields

These are platform-level fields that are expected to remain consistent across tenants and are exposed as typed DTO fields.
Examples:

* `tenantLogo`
* `maxRooms`
* `accessibility`
* `maxCapacityPerRoom`
* `properties`
  
---

## 3. Current Module Scope

### Tenant configuration flow

The backend currently supports fetching tenant configuration and related properties for frontend rendering.

Example use case:

* frontend route contains a tenant identifier
* backend fetches tenant config from database
* backend fetches properties linked to tenant
* backend returns a frontend-ready response

### Calendar flow

The backend currently supports:

* fetching calendar day pricing for a property
* fetching minimum price for a selected date range

These APIs are now being exposed through GraphQL queries instead of REST-only endpoints.

---

## 4. API Design Philosophy

This project uses **GraphQL as a contract layer**, not as a raw database mirror.

That means:

* database `jsonb` is not blindly dumped to the frontend
* stable business fields are flattened and typed
* dynamic configuration stays flexible where necessary
* relational data is merged into the response where appropriate

This approach gives:

* better frontend type safety
* cleaner contracts
* easier long-term evolution
* less schema churn for tenant-specific configuration

---

## 5. GraphQL Contract (Current Direction)

### Tenant configuration query

The frontend consumes tenant configuration through GraphQL.

Typical response shape:

* `tenantLogo`
* `maxRooms`
* `accessibility`
* `maxCapacityPerRoom`
* `guests`
* `properties`

### Calendar queries

The backend exposes pricing data through GraphQL queries for:

* property calendar view
* minimum price in a date range

### Scalars in use

Custom GraphQL scalars are used for:

* `JSON`
* `Date`
* `BigDecimal`

---

## 6. Package Structure

A typical structure for the backend is:

```text
src/main/java/com/example/ibe
├── config
├── controller
├── dto
├── entity
├── graphql
├── repository
├── service
└── IbeApplication.java
```

### Recommended responsibilities

* `config` → Spring configuration, GraphQL scalar wiring, CORS
* `controller` → legacy or transitional REST endpoints
* `dto` → API-facing response and request objects
* `entity` → JPA entities mapped to PostgreSQL tables
* `graphql` → GraphQL query resolvers
* `repository` → Spring Data JPA repositories
* `service` → business logic and response shaping

---

## 7. Running the Project Locally

### Prerequisites

Make sure the following are installed:

* Java 21
* Maven
* PostgreSQL

### Database setup

Create a PostgreSQL database and configure datasource properties in `application.yml`.

Typical settings include:

* database URL
* username
* password
* Hibernate DDL mode

### Environment-specific config (local vs prod)

This project now uses Spring profiles so you do not need to edit datasource values every time.

* `application.properties` keeps shared settings and sets default profile to `local`.
* `application-local.properties` contains local DB defaults and GraphiQL enabled.
* `application-prod.properties` reads DB credentials from environment variables and disables GraphiQL.

Use profile selection via environment variable:

```bash
# local (also default)
SPRING_PROFILES_ACTIVE=local

# production
SPRING_PROFILES_ACTIVE=prod
```

Production environment variables required:

* `SPRING_DATASOURCE_URL`
* `SPRING_DATASOURCE_USERNAME`
* `SPRING_DATASOURCE_PASSWORD`

### Seed data

The project currently supports initializing schema and inserting seed data for local development.

Local development flow generally includes:

* creating tables if they do not already exist
* inserting seed data safely
* using idempotent inserts where appropriate

### Start the application

Run:

```bash
mvn clean spring-boot:run
```

Or build first:

```bash
mvn clean package
java -jar target/*.jar
```

---

## 8. GraphQL Endpoint

By default, the GraphQL endpoint is:

```text
POST /graphql
```

If GraphiQL is enabled, the in-browser explorer is available at:

```text
/graphiql
```

---
