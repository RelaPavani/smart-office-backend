# Smart Office Management System — Backend

Backend foundation for the Smart Office Management System, developed with **Java 21 LTS** and **Spring Boot 3.3.x**.

---

## 📋 Technology Stack

- **Language:** Java 21 LTS
- **Framework:** Spring Boot 3.3.5
- **Build Tool:** Apache Maven 3.9.9
- **Security:** Spring Security 6 (Stateless, BCrypt, preparation for JWT)
- **Data Persistence:** Spring Data JPA / Hibernate
- **Database:** MySQL (Configured for Aiven Cloud with SSL/TLS)
- **Validation:** Jakarta Bean Validation (Hibernate Validator)
- **Utilities:** Project Lombok, Java JWT (JJWT 0.12.6)
- **Monitoring:** Spring Boot Actuator

---

## 🏛️ Project Architecture

```
com.smartoffice
│
├── config                 # Application & Spring MVC configurations
├── controller             # REST API controllers (thin layer)
├── dto                    # Data Transfer Objects
│   ├── request            # Input request payloads with validation
│   └── response           # Output response payloads (ApiResponse, ErrorResponse, HealthResponse)
├── entity                 # JPA domain entities (to be expanded in Prompt 2)
├── repository             # Spring Data JPA repositories (to be expanded in Prompt 2)
├── service                # Business service interfaces
│   └── impl               # Service implementations
├── security               # Spring Security configuration and filters
├── exception              # Global exception handling and custom domain exceptions
├── util                   # Shared constants and utility helpers
└── SmartOfficeBackendApplication.java  # Main application entry point
```

---

## ⚙️ Environment Configuration

The application is fully externalized and configured via environment variables. Do **not** hardcode credentials or commit secrets into version control.

### Required Environment Variables

| Variable | Description | Default (Local fallback) |
|---|---|---|
| `PORT` | Application HTTP port | `8080` |
| `DB_HOST` | MySQL database host (e.g., Aiven cloud host) | `localhost` |
| `DB_PORT` | MySQL database port | `3306` |
| `DB_NAME` | MySQL database name | `smart_office` |
| `DB_USERNAME` | MySQL database username | `root` |
| `DB_PASSWORD` | MySQL database password | *(empty)* |
| `DB_SSL_MODE` | MySQL SSL/TLS mode (`REQUIRED` for Aiven) | `PREFERRED` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins (comma separated) | `http://localhost:3000,http://localhost:5173` |
| `JWT_SECRET` | 256-bit secret key for HMAC-SHA algorithms | *(Default development key)* |
| `JWT_EXPIRATION_MS` | JWT token validity in milliseconds | `86400000` (24 hours) |

### Setup Local Environment File

1. Copy `.env.example` to `.env`:
   ```powershell
   Copy-Item .env.example .env
   ```
2. Update the database connection parameters in `.env` with your Aiven Cloud MySQL credentials once available.

---

## 🚀 Building and Running

### Prerequisites
- **Java Development Kit (JDK):** Version 21 LTS
- **Apache Maven:** Version 3.9+
- **Git**

### Build the Project
```powershell
mvn clean compile
```

### Run Tests
```powershell
mvn test
```

### Package Application
```powershell
mvn clean package -DskipTests=false
```

### Run Application
```powershell
mvn spring-boot:run
```
Or run the packaged JAR:
```powershell
java -jar target/smart-office-backend-0.0.1-SNAPSHOT.jar
```

---

## 🏥 Health Endpoint

- **Endpoint:** `GET /api/health`
- **Access:** Public (no authentication required)
- **Response Format:**
  ```json
  {
    "status": "UP",
    "service": "Smart Office Management System",
    "timestamp": "2026-10-08T04:08:00",
    "version": "1.0.0"
  }
  ```

---

## 🛡️ Security & CORS Policy

- **Session Management:** Stateless (`SessionCreationPolicy.STATELESS`)
- **CSRF:** Disabled for RESTful APIs
- **CORS:** Configured for local React development servers (`http://localhost:3000` and `http://localhost:5173`) and configurable via `CORS_ALLOWED_ORIGINS` for production Google Cloud deployments.
- **Endpoints:**
  - `/api/health` — Public
  - `/actuator/health` — Public
  - All other API endpoints — Require authentication (ready for JWT in subsequent phases).
