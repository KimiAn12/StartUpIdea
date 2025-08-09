# LegalEase AI - Detailed Setup Guide

This guide provides step-by-step instructions for setting up the LegalEase AI application in different environments.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Development Environment Setup](#development-environment-setup)
3. [Production Deployment](#production-deployment)
4. [Environment Variables](#environment-variables)
5. [Database Setup](#database-setup)
6. [API Keys Setup](#api-keys-setup)
7. [Troubleshooting](#troubleshooting)

## Prerequisites

### System Requirements

- **Operating System**: Windows 10+, macOS 10.15+, or Linux
- **Memory**: Minimum 4GB RAM (8GB recommended)
- **Disk Space**: At least 2GB free space
- **Network**: Internet connection for API calls and dependency downloads

### Required Software

1. **Java Development Kit (JDK) 17+**
   - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
   - Verify installation: `java -version`

2. **Node.js 18+**
   - Download from [nodejs.org](https://nodejs.org/)
   - Verify installation: `node --version` and `npm --version`

3. **PostgreSQL 12+**
   - Download from [postgresql.org](https://www.postgresql.org/download/)
   - Alternative: Use Docker: `docker run --name postgres -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres`

4. **Git**
   - Download from [git-scm.com](https://git-scm.com/)

## Development Environment Setup

### Step 1: Clone the Repository

```bash
git clone <your-repository-url>
cd legaleraseai
```

### Step 2: Database Setup

#### Option A: Local PostgreSQL Installation

1. Start PostgreSQL service
2. Connect to PostgreSQL:
   ```bash
   psql -U postgres
   ```

3. Create database and user:
   ```sql
   CREATE DATABASE legalease_db;
   CREATE USER legalease_user WITH ENCRYPTED PASSWORD 'legalease_password';
   GRANT ALL PRIVILEGES ON DATABASE legalease_db TO legalease_user;
   \q
   ```

#### Option B: Docker PostgreSQL

```bash
docker run --name legalease-postgres \
  -e POSTGRES_DB=legalease_db \
  -e POSTGRES_USER=legalease_user \
  -e POSTGRES_PASSWORD=legalease_password \
  -p 5432:5432 \
  -d postgres:15
```

### Step 3: Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd legaleraseai
   ```

2. Create environment file:
   ```bash
   cp env.example .env
   ```

3. Edit `.env` file with your configuration:
   ```env
   # Database Configuration
   DB_USERNAME=legalease_user
   DB_PASSWORD=legalease_password
   
   # JWT Configuration (generate a secure random string)
   JWT_SECRET=your-super-secret-jwt-key-here-make-it-long-and-random-at-least-256-bits
   JWT_EXPIRATION=86400
   
   # Google Gemini API Configuration
   GOOGLE_GEMINI_API_KEY=your-google-gemini-api-key-here
   
   # CORS Configuration
   CORS_ALLOWED_ORIGINS=http://localhost:3000
   ```

4. Install dependencies and run:
   ```bash
   # On Unix/macOS
   ./mvnw clean install
   ./mvnw spring-boot:run
   
   # On Windows
   mvnw.cmd clean install
   mvnw.cmd spring-boot:run
   ```

5. Verify backend is running:
   - Open `http://localhost:8080/actuator/health`
   - Should return: `{"status":"UP"}`

### Step 4: Frontend Setup

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Create environment file:
   ```bash
   cp env.example .env.local
   ```

4. Edit `.env.local` file:
   ```env
   NEXT_PUBLIC_API_URL=http://localhost:8080
   NODE_ENV=development
   ```

5. Start the development server:
   ```bash
   npm run dev
   ```

6. Verify frontend is running:
   - Open `http://localhost:3000`
   - Should see the LegalEase AI homepage

## ____________________________________________________________________

## Production Deployment

### Backend Deployment

#### Option A: JAR Deployment

1. Build the application:
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. Set environment variables:
   ```bash
   export DB_USERNAME=your_db_user
   export DB_PASSWORD=your_db_password
   export JWT_SECRET=your_jwt_secret
   export GOOGLE_GEMINI_API_KEY=your_api_key
   ```

3. Run the JAR:
   ```bash
   java -jar target/legaleraseai-0.0.1-SNAPSHOT.jar
   ```

#### Option B: Docker Deployment

1. Create `Dockerfile` in the root directory:
   ```dockerfile
   FROM openjdk:17-jdk-slim
   VOLUME /tmp
   COPY target/legaleraseai-0.0.1-SNAPSHOT.jar app.jar
   ENTRYPOINT ["java","-jar","/app.jar"]
   ```

2. Build and run:
   ```bash
   ./mvnw clean package -DskipTests
   docker build -t legalease-backend .
   docker run -p 8080:8080 \
     -e DB_USERNAME=your_db_user \
     -e DB_PASSWORD=your_db_password \
     -e JWT_SECRET=your_jwt_secret \
     -e GOOGLE_GEMINI_API_KEY=your_api_key \
     legalease-backend
   ```

### Frontend Deployment

#### Option A: Static Export

1. Update `next.config.js`:
   ```javascript
   /** @type {import('next').NextConfig} */
   const nextConfig = {
     output: 'export',
     trailingSlash: true,
     images: {
       unoptimized: true
     }
   }
   module.exports = nextConfig
   ```

2. Build and export:
   ```bash
   npm run build
   ```

3. Deploy the `out` folder to your static hosting service.

#### Option B: Node.js Server

1. Build the application:
   ```bash
   npm run build
   ```

2. Start the production server:
   ```bash
   npm start
   ```

## Environment Variables

### Backend Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DB_USERNAME` | Database username | legalease_user | Yes |
| `DB_PASSWORD` | Database password | legalease_password | Yes |
| `JWT_SECRET` | JWT signing secret | - | Yes |
| `JWT_EXPIRATION` | JWT expiration time (seconds) | 86400 | No |
| `GOOGLE_GEMINI_API_KEY` | Google Gemini API key | - | Yes |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | http://localhost:3000 | No |

### Frontend Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `NEXT_PUBLIC_API_URL` | Backend API URL | http://localhost:8080 | Yes |
| `NODE_ENV` | Environment mode | development | No |

## API Keys Setup

### Google Gemini API Key

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the generated API key
5. Add it to your environment variables as `GOOGLE_GEMINI_API_KEY`

**Important**: Keep your API key secure and never commit it to version control.

### JWT Secret Generation

Generate a secure JWT secret:

```bash
# Using OpenSSL
openssl rand -base64 64

# Using Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"

# Using Python
python -c "import secrets; print(secrets.token_urlsafe(64))"
```

## Database Setup

### Schema Migration

The application uses Hibernate with `ddl-auto=update`, so tables will be created automatically. For production, consider:

1. Setting `ddl-auto=validate`
2. Using Flyway or Liquibase for migrations
3. Creating tables manually with proper indexes

### Sample Database Schema

```sql
-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(120) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER',
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Documents table
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    content_type VARCHAR(100),
    extracted_text TEXT,
    processing_status VARCHAR(20) DEFAULT 'PENDING',
    processing_error TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Document analyses table
CREATE TABLE document_analyses (
    id BIGSERIAL PRIMARY KEY,
    analysis_type VARCHAR(50) NOT NULL,
    result TEXT NOT NULL,
    prompt TEXT,
    confidence_score DOUBLE PRECISION,
    status VARCHAR(20) DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    document_id BIGINT NOT NULL,
    FOREIGN KEY (document_id) REFERENCES documents(id)
);

-- Extracted clauses table
CREATE TABLE extracted_clauses (
    id BIGSERIAL PRIMARY KEY,
    clause_type VARCHAR(100) NOT NULL,
    clause_text TEXT NOT NULL,
    start_position INTEGER,
    end_position INTEGER,
    confidence_score DOUBLE PRECISION,
    importance_level VARCHAR(20) DEFAULT 'MEDIUM',
    plain_english_explanation TEXT,
    created_at TIMESTAMP NOT NULL,
    document_id BIGINT NOT NULL,
    FOREIGN KEY (document_id) REFERENCES documents(id)
);
```

## Troubleshooting

### Common Issues

#### Backend Issues

1. **Application fails to start**
   - Check Java version: `java -version`
   - Verify environment variables are set
   - Check database connection

2. **Database connection errors**
   ```bash
   # Test database connection
   psql -h localhost -U legalease_user -d legalease_db
   ```

3. **Gemini API errors**
   - Verify API key is correct
   - Check API quotas in Google Cloud Console
   - Test API key with curl:
   ```bash
   curl -H "Content-Type: application/json" \
        -d '{"contents":[{"parts":[{"text":"Hello"}]}]}' \
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=YOUR_API_KEY"
   ```

#### Frontend Issues

1. **Cannot connect to backend**
   - Verify `NEXT_PUBLIC_API_URL` is correct
   - Check if backend is running on specified port
   - Check browser console for CORS errors

2. **Build failures**
   - Clear node_modules: `rm -rf node_modules && npm install`
   - Check Node.js version compatibility
   - Verify all environment variables are set

#### File Upload Issues

1. **Files not uploading**
   - Check file size (max 50MB)
   - Verify file type (PDF, DOC, DOCX)
   - Check disk space on server

2. **Processing failures**
   - Check Apache Tika dependencies
   - Verify file is not corrupted
   - Check server logs for detailed errors

### Logging

Enable debug logging by adding to `application.properties`:

```properties
logging.level.com.kimi.legaleraseai=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=DEBUG
```

### Performance Optimization

1. **Database**
   - Add indexes on frequently queried columns
   - Configure connection pooling
   - Monitor query performance

2. **File Storage**
   - Consider using cloud storage (AWS S3, Google Cloud Storage)
   - Implement file cleanup for old documents
   - Add file compression for large documents

3. **API Rate Limiting**
   - Implement rate limiting for Gemini API calls
   - Add caching for frequently requested analyses
   - Consider API call batching

## Support

If you encounter issues not covered in this guide:

1. Check the application logs
2. Review the [main README](README.md)
3. Search for similar issues in the project repository
4. Create a new issue with detailed error information

Remember to never include sensitive information (API keys, passwords) when reporting issues.
