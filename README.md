# SmartHospital Backend API Documentation

A production-grade hospital management backend built with Spring Boot 4, featuring JWT authentication, role-based access control, real-time notifications via WebSocket, RabbitMQ integration for durability, and comprehensive medical data management.

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [Complete API Reference](#complete-api-reference)
5. [Security & Authentication](#security--authentication)
6. [Development & Troubleshooting](#development--troubleshooting)
7. [Project Structure (Updated)](#project-structure-updated)

## Overview
SmartHospital backend supports authentication, appointment workflows, medical records, prescriptions, medication catalog, chat, notifications, and admin reporting.

## Prerequisites
Before you begin, ensure you have installed:

- **Java 21** - [Download](https://www.oracle.com/java/technologies/downloads/#java21)
- **Maven 3.6+** - [Download](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose** - [Download](https://www.docker.com/products/docker-desktop)
- **Git** - [Download](https://git-scm.com/download)

## Quick Start

### 1. Clone and Navigate to Backend

```bash
cd SmartHospital/backend
```

### 2. Set Up Environment Variables

Create a `.env` file in the `backend` root directory with the following configuration:

```env
# Database Configuration
# PostgreSQL Container Configuration
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_postgres_password
POSTGRES_DB=SmartHospital_DB

# PgAdmin Configuration
PGADMIN_EMAIL=admin@local.dev
PGADMIN_PASSWORD=your_pgadmin_password

# Email Configuration (Gmail)
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_app_specific_password

# Frontend URL (for CORS)
FRONTEND_URL=http://localhost:3000

# RabbitMQ Configuration
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# MinIO Configuration (File Storage)
MINIO_USER=minioadmin
MINIO_PASSWORD=minioadmin

# JWT Secret
JWT_SECRET=your_super_secret_jwt_key_change_this_in_production
```

> **Note:** For Gmail's `MAIL_PASSWORD`, use an [App-Specific Password](https://myaccount.google.com/apppasswords) instead of your regular Gmail password.

When running with Docker Compose, set `POSTGRES_HOST` to the PostgreSQL service name (default: `postgres`) and `POSTGRES_PORT` to `5432` so the backend can resolve the database container on the same network.

### 3. Start Services with Docker Compose

Start all infrastructure services (RabbitMQ, Redis, MinIO, PostgreSQL, PgAdmin):

```bash
docker compose -f compose.yaml up -d
```

This will start:
- **RabbitMQ**: Message broker (ports 5672, 15672)
- **Redis**: Caching layer (port 6379)
- **MinIO**: File storage (ports 9000, 9001)
- **PostgreSQL**: Relational database (port 5432)
- **PgAdmin**: PostgreSQL UI (port 5050)

Verify services are running:

```bash
docker compose -f compose.yaml ps
```

### Docker-first Tutorial (recommended for local dev)

Follow these steps to bring up infrastructure, register the DB in PgAdmin, and run the backend. Do NOT copy secrets into the repository — use a local `.env` and keep it out of version control.

1. Create a local `.env` in `backend/` (do not commit). Use [`.env.example`](.env.example) as a template and fill your values.

2. Start infrastructure:

```bash
docker compose -f compose.yaml up -d
```

3. Wait for Postgres to become healthy. The Compose file includes a healthcheck; you can run `docker compose ps` or:

```bash
docker compose -f compose.yaml logs -f postgres
```

4. Open PgAdmin at `http://localhost:5050` and register a new server:
- Host: `postgres` (use this service name when the backend runs inside Compose)
- Port: `5432`
- Maintenance DB / Username / Password: values from your local `.env`

5. Run the backend:

```bash
./mvnw spring-boot:run
```

The application is configured with `spring.jpa.hibernate.ddl-auto: update` and the file `init-db.sql` is mounted into the Postgres container; it will run automatically only when the DB volume is empty (first initialization). If you need to re-run initialization, remove the `postgres_data` volume and restart the `postgres` service.

6. If the backend starts before Postgres is ready, the property `spring.jpa.defer-datasource-initialization: true` helps, but you may still see connection failures on first startup. Use a retry/wait script or simply restart the backend after Postgres is healthy.

Security note: Never paste real credentials or long secrets into `README.md`. Keep `.env` private and add it to `.gitignore`.

### 4. Access PgAdmin (Database UI)

Open PgAdmin:

- URL: http://localhost:5050
- Email: value of `PGADMIN_EMAIL` in `.env`
- Password: value of `PGADMIN_PASSWORD` in `.env`

Add your PostgreSQL server in PgAdmin:

- Host name/address: `postgres`
- Port: `5432`
- Maintenance database: value of `POSTGRES_DB` (example: `SmartHospital_DB`)
- Username: value of `POSTGRES_USER`
- Password: value of `POSTGRES_PASSWORD`

### 5. Database Initialization

The file `init-db.sql` is mounted to PostgreSQL at startup and is executed automatically on first initialization of the PostgreSQL data volume.

If you need to re-run `init-db.sql`, reset only the PostgreSQL volume:

```bash
docker compose -f compose.yaml down
docker volume rm backend_postgres_data
docker compose -f compose.yaml up -d postgres
```

### 6. Build the Application

```bash
# Using Maven wrapper (recommended)
./mvnw clean package

# Or if using Maven directly
mvn clean package
```

### 7. Run the Application

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or run the packaged JAR
java -jar target/SmartHospital-0.0.1-SNAPSHOT.jar
```

The application will start on **http://localhost:8080**

## API Documentation

Once the application is running, access the Swagger UI documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## Service Endpoints

| Service | URL | Purpose |
|---------|-----|---------|
| Backend API | http://localhost:8080 | Main application |
| RabbitMQ Management | http://localhost:15672 | Message broker UI |
| PostgreSQL | localhost:5432 | Database service |
| PgAdmin | http://localhost:5050 | PostgreSQL UI |
| Redis | localhost:6379 | Caching service |
| MinIO Console | http://localhost:9001 | File storage UI |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |

---

## Complete API Reference

### 1. Authentication Endpoints (/auth)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | /login | PUBLIC | Authenticate user, issue JWT tokens |
| POST | /register | PUBLIC | Register new patient with optional files (avatar, medical records) |
| POST | /logout | PATIENT, DOCTOR, ADMIN | Blacklist access token, clear refresh cookie |
| POST | /refresh-token | PATIENT, DOCTOR, ADMIN | Rotate access and refresh tokens |
| POST | /send-otp | PUBLIC | Send OTP email for password reset |
| POST | /verify-otp | PUBLIC | Verify OTP, return reset token |
| POST | /reset-password | PUBLIC | Reset password with token |

**Authentication Flow**:
```bash
# 1. Register
curl -X POST http://localhost:8080/auth/register \
  -F "email=patient@hospital.com" \
  -F "password=SecurePass123!" \
  -F "name=John Patient"

# 2. Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"patient@hospital.com","password":"SecurePass123!"}'

# 3. Use token in subsequent requests
curl -X GET http://localhost:8080/user/patient/user-profile/view \
  -H "Authorization: Bearer <access-token>"
```

---

### 2. Appointment Endpoints (/appointment)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /patient/getAppointments | PATIENT | Get patient's appointments (paginated, searchable) |
| GET | /doctor/getAppointments | DOCTOR | Get doctor's appointments (paginated, searchable) |
| POST | /createAppointment | PATIENT | Book appointment by timeslot |
| POST | /cancelAppointment | PATIENT, DOCTOR | Cancel appointment with reason |
| POST | /rescheduleAppointment | PATIENT, DOCTOR | Reschedule to new date/time |
| POST | /acceptAppointment | DOCTOR | Doctor accepts pending appointment |
| GET | /available-doctors | PATIENT, ADMIN | Get available doctors for date+timeslot |

**Appointment Workflow**:
```bash
# 1. Find available doctors
curl -X GET "http://localhost:8080/appointment/available-doctors?date=2026-04-27&time=10:00:00" \
  -H "Authorization: Bearer <token>"

# 2. Book appointment
curl -X POST http://localhost:8080/appointment/createAppointment \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"appointmentDate":"2026-04-27","appointmentTime":"10:00:00","notes":"Checkup"}'

# 3. Doctor accepts (doctor receives WebSocket notification first)
curl -X POST http://localhost:8080/appointment/acceptAppointment \
  -H "Authorization: Bearer <doctor-token>" \
  -H "Content-Type: application/json" \
  -d '{"appointmentId":"<id>"}'
```

---

### 3. Medical Record Endpoints (/medical-records)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /{recordId} | PATIENT, DOCTOR | Get single record (if owner) |
| GET | /my | PATIENT, DOCTOR | Get user's records (patient: own, doctor: assigned) |
| POST | / | DOCTOR | Create new medical record for patient |
| PUT | /{recordId} | DOCTOR | Edit record (doctor in charge only) |
| DELETE | /{recordId}/soft-delete | DOCTOR | Mark record as deleted (soft) |
| DELETE | /{recordId}/hard-delete | DOCTOR | Permanently delete record (hard) |

**Medical Record Lifecycle**:
- Doctor creates record for patient
- Doctor can edit treatment notes, diagnoses, attachments
- Soft delete hides from queries; hard delete removes permanently
- Editing medical record does NOT affect prescriptions

---

### 4. Prescription Endpoints (/prescriptions)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /my | PATIENT | Get patient's prescriptions |
| GET | /{prescriptionId} | PATIENT, DOCTOR | Get prescription (if involved) |
| POST | / | DOCTOR | Create prescription for patient + select medications |
| PUT | /{prescriptionId} | DOCTOR | Edit prescription (can change medications, notes) |
| DELETE | /{prescriptionId}/soft-delete | DOCTOR | Mark prescription as deleted (soft) |
| DELETE | /{prescriptionId}/hard-delete | DOCTOR | Permanently delete prescription (hard) |

**Prescription Example**:
```bash
# Create prescription
curl -X POST http://localhost:8080/prescriptions \
  -H "Authorization: Bearer <doctor-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId":"patient_123",
    "medicationIds":["med_001","med_002"],
    "notes":"Take once daily with food"
  }'

# Edit prescription
curl -X PUT http://localhost:8080/prescriptions/pres_123 \
  -H "Authorization: Bearer <doctor-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "medicationIds":["med_001"],
    "notes":"Take twice daily"
  }'
```

---

### 5. Medication Endpoints (/medications)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | / | PATIENT, DOCTOR, ADMIN | List all medications (catalog) |
| GET | /{medicationId} | PATIENT, DOCTOR, ADMIN | Get single medication |
| POST | / | ADMIN | Create new medication |
| PUT | /{medicationId} | ADMIN | Update medication details |
| DELETE | /{medicationId}/soft-delete | ADMIN | Mark medication as deleted (soft) |
| DELETE | /{medicationId}/hard-delete | ADMIN | Permanently delete medication (hard) |

---

### 6. Patient Management Endpoints (/user)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /getPatients | ADMIN | List patients (paginated) |
| POST | /patient/user-profile/edit | PATIENT | Edit own profile + avatar upload |
| GET | /patient/user-profile/view | PATIENT | View own profile |
| DELETE | /patient/{patientId}/soft-delete | ADMIN | Mark patient as deleted (soft) |
| DELETE | /patient/{patientId}/hard-delete | ADMIN | Permanently delete patient (hard) |

---

### 7. Doctor Management Endpoints (/user)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /getDoctors | ADMIN | List doctors (paginated) |
| POST | /doctor/user-profile/edit | DOCTOR | Edit own profile + avatar upload |
| GET | /doctor/user-profile/view | DOCTOR | View own profile |
| DELETE | /doctor/{doctorId}/soft-delete | ADMIN | Mark doctor as deleted (soft) |
| DELETE | /doctor/{doctorId}/hard-delete | ADMIN | Permanently delete doctor (hard) |
| POST | /doctor/import | ADMIN | Bulk import doctors from Excel/XLSX |

**Doctor Excel Import Format**:
```
fullname | email | password | phonenumber | identitynumber | gender | dateofbirth | address | workinghours | availabilitystatus | specialization | departmentname
Dr. John | john@hosp.com | Pass123! | 1234567890 | DOC123 | MALE | 1980-01-15 | Main St | 09:00-17:00 | AVAILABLE | Cardiology | Cardiology
```

```bash
curl -X POST http://localhost:8080/user/doctor/import \
  -H "Authorization: Bearer <admin-token>" \
  -F "file=@doctors_import.xlsx"
```

---

### 8. Admin Report Endpoints (/admin)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /reports/daily | ADMIN | Daily statistics (date parameter required) |
| GET | /reports/monthly | ADMIN | Monthly statistics (year, month parameters) |
| GET | /reports/yearly | ADMIN | Yearly statistics (year parameter) |

**Report Response**:
```json
{
  "totalAppointments": 150,
  "completedAppointments": 120,
  "cancelledAppointments": 20,
  "pendingAppointments": 10,
  "totalPatients": 500,
  "activePatients": 480,
  "totalDoctors": 50,
  "activeDoctors": 48
}
```

---

### 9. Chat Endpoints (/chat)

| Method | Path | Protocol | Role | Description |
|--------|------|----------|------|-------------|
| GET | /getAllChats | HTTP | PATIENT, DOCTOR | Get all conversations |
| GET | /getChatHistory | HTTP | PATIENT, DOCTOR | Get message history with user |
| GET | /isOnline | HTTP | PATIENT, DOCTOR | Check if user is online |
| - | /send | WebSocket | PATIENT, DOCTOR | Send message (real-time) |
| - | /markAsRead | WebSocket | PATIENT, DOCTOR | Mark messages as read |
| - | /markAsDelivered | WebSocket | PATIENT, DOCTOR | Mark messages as delivered |
| - | /markAsSent | WebSocket | PATIENT, DOCTOR | Mark messages as sent |

**WebSocket Connection**:
```javascript
// Connect
const token = localStorage.getItem('accessToken');
const ws = new WebSocket(`ws://localhost:8080/ws?token=${token}`);

// Send message
ws.send(JSON.stringify({
  destination: '/app/send',
  body: JSON.stringify({
    recipientId: 'doctor_123',
    content: 'Hello doctor'
  })
}));

// Mark as read
ws.send(JSON.stringify({
  destination: '/app/markAsRead',
  body: JSON.stringify({
    otherUserId: 'doctor_123'
  })
}));
```

---

### 10. Notification Endpoints (/notifications)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /my | PATIENT, DOCTOR | Get user's notifications |
| GET | /my/unread-count | PATIENT, DOCTOR | Get unread notification count |
| PATCH | /{notificationId}/read | PATIENT, DOCTOR | Mark notification as read |
| PATCH | /my/read-all | PATIENT, DOCTOR | Mark all as read |
| PATCH | /{notificationId}/unread | PATIENT, DOCTOR | Mark as unread |

**Notification Sources**:
- Appointment created/cancelled/rescheduled → doctor notified
- Chat messages → recipient notified
- Delivered via WebSocket + optional RabbitMQ queue (feature-flagged)

---

## Security & Authentication

### JWT Flow
1. **Access Token** (short-lived, default 15 min)
   - Used for API requests: `Authorization: Bearer <access-token>`
   - Stateless validation
  - Expires automatically

2. **Refresh Token** (long-lived, default 7 days)
   - Stored in HttpOnly, Secure, SameSite=Strict cookie
   - Can be rotated and revoked
   - Stored in Redis for validation

3. **Token Blacklist** (on logout)
   - Access tokens blacklisted in Redis
   - TTL = token expiry time
   - Prevents token reuse after logout

### RabbitMQ Integration (Optional)

Enable in `application.yml`:
```yaml
app:
  notifications:
    rabbit:
      enabled: true  # Set to false to disable; WebSocket still works
```

**When Enabled**:
- Appointment notifications queued for durability
- OTP emails queued with 5-min expiry
- Retries on broker unavailable
- WebSocket still works as fallback

### Role-Based Access Control

| Feature | ADMIN | DOCTOR | PATIENT |
|---------|-------|--------|---------|
| Login/Register | ✓ | ✓ | ✓ |
| View own profile | ✓ | ✓ | ✓ |
| Edit own profile | ✓ | ✓ | ✓ |
| List users | ✓ | - | - |
| Delete users | ✓ | - | - |
| Create appointments | - | - | ✓ |
| Accept appointments | ✓ (any) | ✓ (own) | ✓ (own) |
| Create medical records | - | ✓ | - |
| Edit medical records | - | ✓ (assigned) | - |
| View medical records | - | ✓ (assigned) | ✓ (own) |
| Create prescriptions | - | ✓ | - |
| Manage medications | ✓ | - | - |
| Send chat messages | ✓ | ✓ | ✓ |
| Generate reports | ✓ | - | - |

---

## Error Handling

### Response Format
```json
{
  "status": 200,
  "message": "Operation successful",
  "data": { /* response payload */ }
}
```

### HTTP Status Codes
- **200 OK** - Successful GET/POST/PUT/PATCH
- **201 Created** - Resource created
- **400 Bad Request** - Invalid input, validation error
- **401 Unauthorized** - Missing/invalid JWT token
- **403 Forbidden** - Valid token but insufficient permissions
- **404 Not Found** - Resource doesn't exist
- **409 Conflict** - Resource already exists (e.g., duplicate email)
- **500 Internal Server Error** - Unexpected error

---

## File Upload (MinIO)

**Avatar Upload**:
- Endpoint: `POST /user/{role}/user-profile/edit`
- Field: `avatarFile` (multipart/form-data)
- Storage: MinIO bucket `avatar/{userId}/{filename}`
- Max size: Configured in application.yml

**Medical Records Upload**:
- Endpoint: `POST /auth/register` (on signup) or medical records controller
- Field: `medicalRecordFiles[]` (multipart/form-data)
- Storage: MinIO bucket `medical-records/{userId}/{filename}`

**Example**:
```bash
curl -X POST http://localhost:8080/user/patient/user-profile/edit \
  -H "Authorization: Bearer <token>" \
  -F "fullName=John Doe" \
  -F "avatarFile=@profile.jpg"
```

---

## Data Models

### User Roles
```
ADMIN    - System administrator
DOCTOR   - Healthcare provider
PATIENT  - Patient
```

### Appointment Status
```
PENDING     - Awaiting doctor acceptance
SCHEDULED   - Confirmed
COMPLETED   - Finished
CANCELLED   - Cancelled by patient/doctor
RESCHEDULED - Rescheduled
```

### Medical Data Lifecycle
- **Soft Delete**: `isDeleted=true`, `deletedAt=timestamp`
  - Hidden from queries
  - Can be restored (not implemented, but field available)

- **Hard Delete**: Permanently removed from database
  - Cannot be recovered
  - Use with caution

---

## Development & Troubleshooting

### Common Issues

**Port 8080 Already in Use**:
```bash
# Change port in application.yml
server:
  port: 8081
```

**Database Connection Error**:
```bash
# Verify PostgreSQL is running
docker compose ps postgres

# Check logs
docker compose logs postgres
```

**Redis Connection Issues**:
```bash
# Test Redis
docker exec redis redis-cli ping
# Should return PONG
```

**RabbitMQ Issues**:
```bash
# Check RabbitMQ is running
docker compose ps rabbitmq

# Access RabbitMQ management
# URL: http://localhost:15672
# User: guest / Pass: guest
```

### Build Commands
```bash
./mvnw clean compile     # Compile only
./mvnw clean package     # Build JAR
./mvnw test              # Run tests
./mvnw spring-boot:run   # Run application
```

---

## Project Structure (Updated)

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/SmartHospital/
│   │   │   ├── config/
│   │   │   │   ├── jwt/
│   │   │   │   └── websocket/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   │   ├── admin/
│   │   │   │   ├── appointment/
│   │   │   │   ├── auth/
│   │   │   │   ├── bot/
│   │   │   │   ├── chat/
│   │   │   │   ├── doctor/
│   │   │   │   ├── medical/
│   │   │   │   ├── messaging/
│   │   │   │   ├── notification/
│   │   │   │   ├── patient/
│   │   │   │   ├── storage/
│   │   │   │   ├── token/
│   │   │   │   └── user/
│   │   │   ├── repository/
│   │   │   ├── model/
│   │   │   ├── dtos/
│   │   │   ├── enums/
│   │   │   └── helper/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── mailTemplate/
│   │       └── static/
│   └── test/
├── compose.yaml
├── init-db.sql
├── pom.xml
└── README.md
```

## Support

- Spring Boot: https://spring.io/projects/spring-boot
- Spring Security: https://spring.io/projects/spring-security
- PostgreSQL: https://www.postgresql.org/docs/
- RabbitMQ: https://www.rabbitmq.com/documentation.html
- Redis: https://redis.io/documentation
- MinIO: https://docs.min.io/

## Testing with the static HTML pages

Quick test harnesses are included in `src/main/resources/static/` to exercise appointments, notifications and chat without the frontend app.

- Files:
  - `src/main/resources/static/appointment-booking-test.html` (patient booking + notifications)
  - `src/main/resources/static/appointment-approval-test.html` (doctor appointment management + notifications)
  - `src/main/resources/static/chat-test.html` (chat UI for realtime messaging)

How to use:
1. Start infrastructure and backend as usual (Docker Compose + Spring Boot):
```bash
docker compose -f compose.yaml up -d
./mvnw spring-boot:run
```

2. Open the test pages in your browser:
  - http://localhost:8080/appointment-booking-test.html
  - http://localhost:8080/appointment-approval-test.html
  - http://localhost:8080/chat-test.html

3. Patient booking flow (booking-test):
  - Paste a valid PATIENT JWT into the `JWT token (PATIENT)` textarea (raw token or `Bearer <token>`). The page will auto-connect, load your appointments and notifications.
  - Pick an appointment date (today or later), then click a 30-minute time slot. The page automatically requests available doctors for that date+slot and will auto-select the first available doctor.
  - Click `Create appointment` to submit. The doctor will receive a realtime notification and the notification center (stored via `/notifications/*`) will update.

4. Doctor flow (approval-test):
  - Paste a valid DOCTOR JWT into the `JWT token (DOCTOR)` textarea. The page will auto-load the doctor's appointments and connect to notifications.
  - Use `Accept`, `Cancel` or `Reschedule` actions; those actions refresh the appointment list and send notifications to patients/doctors as applicable.

5. Notifications & realtime:
  - Notification REST endpoints used by the pages: `/notifications/my`, `/notifications/my/unread-count`, `/notifications/{id}/read`, `/notifications/my/read-all`, `/notifications/{id}/unread`.
  - WebSocket endpoint: `/ws` (STOMP over SockJS). The test pages load SockJS/STOMP client libraries dynamically from CDN when needed.
  - If realtime messages do not arrive, verify the backend is running and RabbitMQ is available (if RabbitMQ-backed delivery is enabled).

Troubleshooting:
- Ensure the JWT used has the correct role (PATIENT vs DOCTOR) and is not expired.
- Open the browser console for SockJS/STOMP errors and network requests.
- If doctor availability doesn't load, confirm the backend `/appointment/available-doctors` endpoint is reachable and the token is valid.

These pages are intended for local/manual testing and debugging only. They are simple static helpers and not part of the production frontend.
