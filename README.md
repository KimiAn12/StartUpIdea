# LegalEase AI - Legal Document Assistant

A full-stack legal document analysis application powered by AI that transforms complex legal documents into plain English.

## Features

- **User Authentication**: Secure JWT-based authentication system
- **Document Upload**: Support for PDF and Word documents (up to 50MB)
- **AI-Powered Analysis**:
  - Document summarization in plain English
  - Key clause extraction with importance ratings
  - Natural language Q&A about documents
  - Legal template generation
- **Modern UI**: Responsive Next.js frontend with Tailwind CSS
- **Secure Backend**: Spring Boot REST API with PostgreSQL database

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.4
- **Language**: Java 24
- **Database**: PostgreSQL
- **Authentication**: JWT with Spring Security
- **Document Processing**: Apache Tika, PDFBox
- **AI Integration**: Google Gemini API

### Frontend
- **Framework**: Next.js 14 with TypeScript
- **Styling**: Tailwind CSS
- **State Management**: React Query
- **Authentication**: JWT with HTTP-only cookies
- **UI Components**: Custom components with Lucide icons

## Prerequisites

Before running the application, ensure you have:

1. **Java 17 or higher** installed
2. **Node.js 18 or higher** installed
3. **PostgreSQL 12 or higher** installed and running
4. **Google Gemini API key** (get it from [Google AI Studio](https://makersuite.google.com/app/apikey))

## Quick Start

### 1. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE legalease_db;
CREATE USER legalease_user WITH ENCRYPTED PASSWORD 'legalease_password';
GRANT ALL PRIVILEGES ON DATABASE legalease_db TO legalease_user;
```

### 2. Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd legaleraseai
   ```

2. Copy environment configuration:
   ```bash
   cp env.example .env
   ```

3. Update the `.env` file with your configuration:
   ```env
   DB_USERNAME=legalease_user
   DB_PASSWORD=legalease_password
   JWT_SECRET=your-super-secret-jwt-key-here-make-it-long-and-random
   GOOGLE_GEMINI_API_KEY=your-google-gemini-api-key-here
   CORS_ALLOWED_ORIGINS=http://localhost:3000
   ```

4. Run the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```

   Or on Windows:
   ```bash
   mvnw.cmd spring-boot:run
   ```

The backend will start on `http://localhost:8080`

### 3. Frontend Setup

1. Navigate to the frontend directory:
   ```bash
   cd legaleraseai/frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Copy environment configuration:
   ```bash
   cp env.example .env.local
   ```

4. Update the `.env.local` file:
   ```env
   NEXT_PUBLIC_API_URL=http://localhost:8080
   NODE_ENV=development
   ```

5. Start the development server:
   ```bash
   npm run dev
   ```

The frontend will start on `http://localhost:3000`

## API Documentation

### Authentication Endpoints

- `POST /api/auth/signup` - Register a new user
- `POST /api/auth/signin` - Login user
- `GET /api/auth/me` - Get current user profile

### Document Endpoints

- `POST /api/documents/upload` - Upload a document
- `GET /api/documents` - Get user's documents (with pagination and search)
- `GET /api/documents/{id}` - Get specific document
- `DELETE /api/documents/{id}` - Delete a document

### AI Analysis Endpoints

- `POST /api/ai/documents/{id}/summarize` - Generate document summary
- `POST /api/ai/documents/{id}/extract-clauses` - Extract key clauses
- `POST /api/ai/documents/{id}/question` - Ask questions about document
- `POST /api/ai/templates/generate` - Generate legal templates
- `GET /api/ai/documents/{id}/analyses` - Get all analyses for document
- `GET /api/ai/documents/{id}/clauses` - Get extracted clauses

## Project Structure

```
legaleraseai/
├── src/main/java/com/kimi/legaleraseai/
│   ├── config/              # Configuration classes
│   ├── controller/          # REST controllers
│   ├── dto/                # Data transfer objects
│   ├── entity/             # JPA entities
│   ├── repository/         # Data repositories
│   ├── security/           # Security configuration
│   └── service/            # Business logic services
├── src/main/resources/
│   └── application.properties
├── frontend/
│   ├── src/
│   │   ├── app/            # Next.js app directory
│   │   ├── components/     # React components
│   │   ├── contexts/       # React contexts
│   │   └── lib/           # Utilities and API client
│   ├── public/            # Static assets
│   └── package.json
└── pom.xml
```

## Database Schema

The application uses the following main entities:

- **Users**: User accounts with authentication
- **Documents**: Uploaded legal documents with metadata
- **DocumentAnalysis**: AI-generated analyses (summaries, Q&A responses)
- **ExtractedClauses**: Key legal clauses extracted from documents

## Configuration

### Backend Configuration

Key configuration properties in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/legalease_db
spring.jpa.hibernate.ddl-auto=update

# File Upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# JWT
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration=86400

# Google Gemini API
google.gemini.api.key=${GOOGLE_GEMINI_API_KEY}
```

### Frontend Configuration

Environment variables for Next.js:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NODE_ENV=development
```

## Deployment

### Backend Deployment

1. Build the JAR file:
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. Run the JAR file:
   ```bash
   java -jar target/legaleraseai-0.0.1-SNAPSHOT.jar
   ```

### Frontend Deployment

1. Build the application:
   ```bash
   npm run build
   ```

2. Start the production server:
   ```bash
   npm start
   ```

## Security Considerations

- JWT tokens are stored in HTTP-only cookies
- File uploads are validated for type and size
- SQL injection protection via JPA
- CORS is configured for frontend domain
- All API endpoints (except auth) require authentication

## Troubleshooting

### Common Issues

1. **Database Connection Error**:
   - Ensure PostgreSQL is running
   - Check database credentials in environment variables
   - Verify database exists and user has permissions

2. **Gemini API Error**:
   - Verify API key is correct
   - Check API quota and billing
   - Ensure internet connectivity

3. **File Upload Issues**:
   - Check file size (max 50MB)
   - Verify file type (PDF, DOC, DOCX only)
   - Ensure sufficient disk space

4. **CORS Issues**:
   - Update `CORS_ALLOWED_ORIGINS` to include frontend URL
   - Check if frontend and backend URLs match

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For support and questions:
- Create an issue on GitHub
- Check the troubleshooting section
- Review the API documentation

---

Built with ❤️ using Spring Boot, Next.js, and Google Gemini AI
