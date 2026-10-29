# CivicResolve

A comprehensive civic engagement platform designed to bridge the gap between citizens and local government. CivicResolve empowers citizens to voice their concerns, track issues, and engage in community improvement initiatives while providing authorities with efficient tools for management and resolution.

## 🚀 Features

### For Citizens
- **Smart Complaint Registration**: Register complaints with precise location pinning and media uploads.
- **Real-time Tracking**: Monitor the status and progress of your submitted issues.
- **AI-Powered Categorization**: Get instant, intelligent suggestions for complaint categories and urgency.
- **Map-Based Incident Location**: Use device location, pin or drag a map marker, or enter an address manually; coordinates are stored with the complaint.
- **Community Voting**: Upvote issues raised by others to highlight critical problems.
- **Profile Management**: Keep track of your contribution history and impact.

### For Administrators
- **Unified Dashboard**: Centralized view of all incoming complaints and community activities.
- **Workflow Automation**: Automated assignment and escalation of issues based on category and location.
- **Analytics & Reporting**: In-depth analytics on resolution times, recurring issues, and community engagement.
- **User Management**: Manage citizen accounts and administrative roles.

## 🛠️ Tech Stack

- **Backend**: **Spring Boot** (Java 21+) - RESTful APIs and business logic.
- **Database**: **MySQL** - Relational data storage.
- **Security**: Spring Security, JWT (JSON Web Tokens), BCrypt.
- **AI Integration**: Gemini API for complaint classification (configure `GEMINI_API_KEY`).
- **Frontend**: **Thymeleaf** & **HTML5** with modern CSS.

## 📂 Project Structure

```
CivicResolve/
├── src/
│   ├── main/
│   │   ├── java/com/civicresolve/
│   │   │   ├── config/        # Security & App Configurations
│   │   │   ├── controller/    # Web & REST Controllers
│   │   │   ├── dto/           # Data Transfer Objects
│   │   │   ├── entity/        # JPA Entities & Enums
│   │   │   ├── repository/    # Spring Data Repositories
│   │   │   └── service/       # Business Logic & User Details
│   │   ├── resources/
│   │   │   ├── application.properties  # Configuration
│   │   │   ├── static/        # CSS, JS, Images
│   │   │   └── templates/     # Thymeleaf Views (HTML)
│   ├── test/                  # Unit & Integration Tests
├── pom.xml                    # Maven Build Configuration
├── mvnw / mvnw.cmd            # Maven Wrapper Scripts
└── README.md
```

## ⚙️ Setup & Installation

### Prerequisites
- Java 21 or higher
- Maven 3.6 or higher
- MySQL 8 or higher

### 1. Clone the Repository
```bash
git clone https://github.com/Sameer0681/CivicResolve.git
cd CivicResolve
```

### 2. Database Configuration
Ensure MySQL is running and create a database named `civicresolve`.

The application uses the following local database settings:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/civicresolve
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD:}
```

Set `DB_USERNAME` and `DB_PASSWORD` in the environment instead of storing database credentials in the properties file.

Email OTPs require working SMTP credentials. Configure `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD` in the environment, and set `APP_MAIL_FROM` to an address accepted by your SMTP provider. For Gmail, use a Google App Password (with 2-Step Verification enabled), not your regular account password. If SMTP is not configured or delivery fails, the application reports an email delivery error; OTPs are never printed to logs or the console.

SMS and voice OTPs use Twilio by default. Configure `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and `TWILIO_PHONE_NUMBER`, or explicitly set `OTP_PROVIDER=mock` for local development. The application no longer seeds demo users or complaints; create real accounts through registration.

The complaint form classifies reports with Gemini when `GEMINI_API_KEY` is configured. If AI classification is unavailable or returns an unsupported sector, the server uses deterministic keyword routing so reports can still be submitted. The incident map uses Leaflet with OpenStreetMap tiles and Nominatim reverse address lookup; selected coordinates are sent to OpenStreetMap for address lookup. Browser location access requires user permission and a secure context (HTTPS or localhost). Follow the [OpenStreetMap tile usage policy](https://operations.osmfoundation.org/policies/tiles/) and [Nominatim usage policy](https://operations.osmfoundation.org/policies/nominatim/) before production deployment.

### 3. Build the Project
```bash
mvn clean install
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

## 🌐 Usage

Access the application at:
- **Frontend**: [http://localhost:8080](http://localhost:8080)
- **API Docs**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.