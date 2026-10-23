# Thapar Grade Predictor Platform (thapar-gpf) - Deployment & Operations Guide

## Overview
This platform is a privacy-first academic grade prediction and relative percentile estimation system designed for students of **Thapar Institute of Engineering and Technology (TIET)**.

---

## 1. Java Spring Boot Deployment Guide

### System Requirements
- **Java Runtime**: JDK 17 or JDK 21 LTS (OpenJDK / Eclipse Temurin / Amazon Corretto)
- **Build Tool**: Apache Maven 3.8+ (or Maven Wrapper `./mvnw`)
- **Database**: PostgreSQL 14+, MySQL 8+, or embedded H2 database for dev/testing
- **Memory**: Minimum 512MB RAM (1GB+ recommended for production)

---

### Local Build & Execution

#### 1. Clone & Build Jar
```bash
# Package application into a standalone executable JAR
mvn clean package -DskipTests
```

#### 2. Run Application
```bash
# Run with default profile (H2 embedded database)
java -jar target/thapar-gpf-1.0.0.jar

# Or run with Maven Spring Boot plugin directly
mvn spring-boot:run
```
The application will be live at: `http://localhost:8080`

---

### Production Configuration (`application-prod.properties`)

Configure environment variables or create `application-prod.properties`:

```properties
# Server Port
server.port=8080

# PostgreSQL Production Database
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:thapargpf}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:secret}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Security & Email Configuration
app.security.institute-domain=thapar.edu
app.prediction.min-submissions=100

# Optional Mail Dispatch (for Unlock Notifications)
spring.mail.host=${SMTP_HOST:smtp.gmail.com}
spring.mail.port=${SMTP_PORT:587}
spring.mail.username=${SMTP_USERNAME:}
spring.mail.password=${SMTP_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

Run in production mode:
```bash
java -Dspring.profiles.active=prod -jar target/thapar-gpf-1.0.0.jar
```

---

### Docker Deployment

Create a `Dockerfile` in the project root:

```dockerfile
# Build Stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run Docker container:
```bash
# Build Docker image
docker build -t thapar-gpf:latest .

# Run container with PostgreSQL link
docker run -d -p 8080:8080 \
  -e DB_HOST=postgres-host \
  -e DB_NAME=thapargpf \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=yourpassword \
  --name thapar-gpf-app thapar-gpf:latest
```

---

### Cloud Platform Deployments

#### A. Deploying to Render / Railway / Heroku
1. Push your repository to GitHub.
2. In the cloud dashboard (Render/Railway), create a new **Web Service**.
3. Choose the repository and select **Java** environment.
4. Set Build Command: `mvn clean package -DskipTests`
5. Set Start Command: `java -Dserver.port=$PORT -jar target/*.jar`
6. Provision a managed **PostgreSQL** database on the platform and link `DATABASE_URL`.

#### B. Deploying to AWS EC2 / Linux VPS (Systemd Service)
Create `/etc/systemd/system/thapar-gpf.service`:
```ini
[Unit]
Description=Thapar Grade Predictor Platform (Spring Boot)
After=syslog.target network.target

[Service]
User=ubuntu
ExecStart=/usr/bin/java -jar /opt/thapar-gpf/thapar-gpf-1.0.0.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```
Enable and start the service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable thapar-gpf
sudo systemctl start thapar-gpf
```

---

## 2. Privacy & Security Implementation Standards

### Strict Zero-Knowledge Privacy Standard
1. **Admins Cannot Access Student Marks**:
   - The database queries accessible to administrators are strictly constrained to aggregate summary statistics.
   - No REST endpoint or Thymeleaf view returns individual student marks to administrative users.
2. **Student-Only Scope**:
   - Only the student who uploaded the marks can retrieve them (`user.getId().equals(currentUser.getId())`).
3. **One-Time Submission & Locking**:
   - Once submitted, records lock automatically (`isLocked = true`).
   - If a student needs modification, an **Unlock Request** is approved by an administrator without exposing marks. Once unlocked, the student updates marks privately.
4. **Aggregate Polls & Analytics**:
   - Poll responses are anonymized: only response counts, average scores, and distribution deciles are exposed.

---

## 3. Reference: Next.js / TypeScript Stack (Optional)

If running the companion TypeScript/Node implementation:
```bash
# Install dependencies
npm install

# Setup environment in .env
DATABASE_URL="mongodb+srv://..."
NEXTAUTH_SECRET="..."
NEXTAUTH_URL="http://localhost:3000"

# Generate Prisma client & sync schema
npx prisma generate
npx prisma db push

# Start dev server
npm run dev

# Or build for production
npm run build
npm run start
```
Deployment for Next.js is configured for **Vercel** or **Node.js standalone server**.
