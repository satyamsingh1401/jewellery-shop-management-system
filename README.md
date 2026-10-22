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
│   │   ├── java/com/company/jewellery/
│   │   │   ├── JewelleryShopApplication.java
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
