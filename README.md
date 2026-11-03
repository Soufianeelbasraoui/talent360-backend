# Talent360 — AI-Powered Recruitment Platform (Backend)

Backend REST API for **Talent360**, an intelligent talent acquisition and candidate matching platform powered by Spring Boot 3, Spring AI, and MySQL.

---

## 🚀 Overview

Talent360 streamlines the hiring workflow with AI-assisted resume parsing, job description generation, and automated candidate-to-job matching scores. The backend is designed as a secure, modular, production-ready REST service.

### Key Capabilities
- **Authentication & Security**: Stateless JWT authentication with refresh token rotation (HTTP-only cookies), role-based access control (`ROLE_ADMIN`, `ROLE_RECRUITER`, `ROLE_CANDIDATE`).
- **AI Integrations (Spring AI)**:
  - **CV Parser**: Extracts structured candidate profiles, skills, education, and experience from PDF/DOCX resumes.
  - **Job Description Generator**: Generates comprehensive job descriptions from concise role criteria and technical requirements.
  - **Candidate Matching**: Computes match scores and explanations comparing candidate profiles against job requirements.
- **Job & Application Management**: Full CRUD for job postings, departments, and applications with a multi-stage status pipeline (`APPLIED`, `SCREENING`, `INTERVIEW`, `OFFER`, `REJECTED`, `HIRED`).
- **OpenAPI / Swagger UI**: Built-in interactive API documentation with SpringDoc.

---

## 🛠️ Tech Stack

- **Java**: 21 (LTS)
- **Framework**: Spring Boot 3.3.5
- **Security**: Spring Security 6 + JJWT 0.12.6
- **Persistence**: Spring Data JPA + Hibernate + MySQL 8.0
- **AI Engine**: Spring AI 1.0.0-M3 (OpenAI integration)
- **Document Processing**: Apache PDFBox 3.0.3, Apache POI 5.3.0
- **Object Mapping**: MapStruct 1.6.2 & Lombok
- **API Documentation**: SpringDoc OpenAPI 2.6.0 (Swagger UI)
- **Containerization**: Docker & Docker Compose

---

## 📁 Project Structure

```text
talent360/
├── src/
│   ├── main/
│   │   ├── java/com/talent360/
│   │   │   ├── config/          # Security, JWT, CORS, OpenAPI, AI configurations
│   │   │   ├── controller/      # REST API Controllers
│   │   │   ├── dto/             # Request & Response Data Transfer Objects
│   │   │   ├── entity/          # JPA Entities (User, Candidate, Job, Application, etc.)
│   │   │   ├── exception/       # Global exception handling & custom exceptions
│   │   │   ├── mapper/          # MapStruct DTO-to-Entity mappers
│   │   │   ├── repository/      # Spring Data JPA repositories
│   │   │   └── service/         # Business logic & AI service implementations
│   │   └── resources/
│   │       ├── application.yml  # Base configuration & profiles
│   │       └── application-*.yml
│   └── test/                    # Unit and integration tests
├── docker-compose.yml           # MySQL + Backend local orchestration
├── Dockerfile                   # Multi-stage production container build
├── pom.xml                      # Maven dependencies and build definition
└── README.md
```

---

## ⚙️ Prerequisites

- **Java 21 JDK** installed ([Eclipse Temurin](https://adoptium.net/) or Oracle JDK)
- **Maven 3.9+** (or use the included `./mvnw`)
- **MySQL 8.0+** (or Docker for running MySQL in a container)
- **OpenAI API Key** (for Spring AI resume extraction and matching features)

---

## 🚀 Getting Started

### 1. Clone & Configure Environment

Copy the example environment file and set your credentials:

```bash
cp .env.example .env
```

Edit `.env` with your settings:

```dotenv
SPRING_PROFILES_ACTIVE=dev

# Database Configuration
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/talent360?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

# JWT Security
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# Spring AI (OpenAI)
OPENAI_API_KEY=sk-proj-your-openai-key
OPENAI_MODEL=gpt-4o-mini

# CORS (Frontend URL)
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

### 2. Run Database via Docker (Optional)

If you don't have MySQL installed locally:

```bash
docker compose up -d mysql
```

### 3. Build & Run Locally

```bash
# Using Maven wrapper (Windows PowerShell)
.\mvnw clean spring-boot:run

# Using Maven wrapper (Linux / macOS)
./mvnw clean spring-boot:run
```

The server will start on `http://localhost:8080`.

---

## 📖 API Documentation

Once the application is running, access Swagger UI in your browser:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Core Endpoints

| Group | Method | Endpoint | Description |
|-------|--------|----------|-------------|
| **Auth** | `POST` | `/api/auth/register` | Register a new user |
| | `POST` | `/api/auth/login` | Authenticate & get JWT |
| | `POST` | `/api/auth/refresh` | Refresh access token (HTTP-only cookie) |
| | `POST` | `/api/auth/logout` | Invalidate current session |
| **Candidates** | `GET` | `/api/candidates/me` | Current authenticated candidate profile |
| | `POST` | `/api/candidates/cv` | Upload and parse CV with AI |
| | `PUT` | `/api/candidates/me` | Update candidate profile details |
| **Jobs** | `GET` | `/api/jobs` | Search & filter active job openings |
| | `POST` | `/api/jobs` | Create a job posting (Recruiter/Admin) |
| | `POST` | `/api/jobs/generate-jd` | Generate job description using AI |
| **Applications**| `POST` | `/api/applications` | Apply for a job |
| | `GET` | `/api/applications/job/{id}` | Recruiter pipeline / Kanban stages |
| | `PATCH` | `/api/applications/{id}/status`| Update applicant stage |
| **Admin** | `GET` | `/api/admin/users` | Manage platform users |
| | `GET` | `/api/admin/analytics` | Recruitment metrics & stats |

---

## 🐳 Docker Deployment

To build and run the complete backend stack (MySQL + Spring Boot) with Docker Compose:

```bash
docker compose up -d --build
```

---

## 🧪 Testing

Run automated tests:

```bash
.\mvnw test
```

