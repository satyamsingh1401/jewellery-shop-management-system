# Jewellery Shop Management System

## Overview

The Jewellery Shop Management System is a modular monolithic application designed to manage jewellery shop operations, including product management, inventory, suppliers, customers, sales, billing, payments, and reporting.

The project is developed collaboratively by a team of 10 members using Java, Spring Boot, MySQL, Maven, Git, and GitHub.

## Technology Stack

- **Backend:** Java, Spring Boot
- **Persistence:** Spring Data JPA, Hibernate
- **Database:** MySQL
- **Build Tool:** Maven
- **Security:** Spring Security
- **API Testing:** Postman
- **Version Control:** Git and GitHub
- **Messaging:** Apache Kafka, where required by the approved architecture

## Architecture

The application follows a modular monolithic architecture. All business modules are part of a single Spring Boot application and share the application's infrastructure.

## Modules

1. Authentication and User Management
2. Product Management
3. Inventory Management
4. Supplier Management
5. Customer Management
6. Sales Management
7. Billing and Invoice Management
8. Testing and Quality Assurance
9. Payment Management
10. Reports and Dashboard

## Prerequisites

- Compatible JDK
- Maven
- MySQL
- Git
- IntelliJ IDEA&#x20;
- Postman

## Getting Started

1. Clone this repository.
2. Create a local MySQL database.
3. Configure database credentials using environment variables or a local, untracked configuration file.
4. Run database migrations.
5. Build and test the application with `mvn clean verify`.
6. Start the application using `mvn spring-boot:run`.
```
## Project Structure
jewellery-shop-management/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/myhourly/jewellery/
│   │   │   ├── JewelleryShopManagementSystemApplication.java
│   │   │   ├── common/
│   │   │   │   ├── exception/
│   │   │   │   └── response/
│   │   │   ├── product/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── enums/
│   │   │   │   ├── mapper/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   ├── metalrate/
│   │   │   ├── inventory/
│   │   │   ├── customer/
│   │   │   ├── order/
│   │   │   ├── billing/
│   │   │   ├── payment/
│   │   │   ├── notification/
│   │   │   ├── reporting/
│   │   │   └── security/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/company/jewellery/
└── README.md
```
## Contribution Guidelines

- Create a feature branch for each task.
- Follow the agreed package structure and coding standards.
- Write and run tests before pushing code.
- Submit a pull request for review.
- Do not push directly to the protected `main` branch.
- Do not commit passwords, secrets, or production data.

## Team

Developed by a 10-member team, coordinated by the project team lead.

## Project Status

Under development.

## Payment integration branch

`feature/payment-management-billing` includes the latest Billing and JWT Security
code and the Order module. The Order module also brings its Customer dependency.
These integrations are local to the feature branch; merging to `main` requires
the team lead's approval.

- Payment creation, reading, confirmation, failure and cancellation allow ADMIN
  and SALES_STAFF. Refunds allow ADMIN only.
- Log in with `POST /api/auth/login`, then send the returned `accessToken` as
  `Authorization: Bearer <token>`. Payment `createdBy` uses the authenticated email.
- Confirmation/refunds update Invoice and Order payment statuses in the same
  transaction. If the Order update fails, all changes roll back.
- The public Order `payment-status` PATCH has been removed. Payment status is
  updated through the Payment flow.

Run `./mvnw clean verify` for unit, controller and integration tests. Full-context
tests activate the `test` profile with an isolated in-memory H2 database and
test-only authentication settings; MySQL and real login credentials are not
needed. Integration tests cover actual JWT login, Payment/Billing/Order status
changes and committed database state after rollback. MySQL/Postman smoke testing
is a separate runtime check.

For a normal local MySQL run, use environment variables `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, `JWT_SECRET` (a Base64-encoded key of at least 32 random bytes),
`JWT_EXPIRATION` (milliseconds) and `CORS_ALLOWED_ORIGINS`. Existing private
`application-local.properties` overrides are still imported. On a database with
no administrator, also set `INITIAL_ADMIN_NAME`, `INITIAL_ADMIN_EMAIL` and
`INITIAL_ADMIN_PASSWORD`. Keep their values private; never reuse the test
configuration for normal runs. Start with `./mvnw spring-boot:run`.
