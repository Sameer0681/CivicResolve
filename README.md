# CivicResolve

A comprehensive civic engagement platform designed to bridge the gap between citizens and local government. CivicResolve empowers citizens to voice their concerns, track issues, and engage in community improvement initiatives while providing authorities with efficient tools for management and resolution.

## 🚀 Features

### For Citizens
- **Smart Complaint Registration**: Register complaints with precise location pinning and media uploads.
- **Real-time Tracking**: Monitor the status and progress of your submitted issues.
- **AI-Powered Categorization**: Get instant, intelligent suggestions for complaint categories and urgency.
- **Community Voting**: Upvote issues raised by others to highlight critical problems.
- **Profile Management**: Keep track of your contribution history and impact.

### For Administrators
- **Unified Dashboard**: Centralized view of all incoming complaints and community activities.
- **Workflow Automation**: Automated assignment and escalation of issues based on category and location.
- **Analytics & Reporting**: In-depth analytics on resolution times, recurring issues, and community engagement.
- **User Management**: Manage citizen accounts and administrative roles.

## 🛠️ Tech Stack

- **Backend**: **Spring Boot** (Java 17+) - RESTful APIs and business logic.
- **Database**: **PostgreSQL** - Relational data storage.
- **Security**: Spring Security, JWT (JSON Web Tokens), BCrypt.
- **AI Integration**: Apache OpenNLP for text processing and categorization.
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
- Java 17 or higher
- Maven 3.6 or higher
- PostgreSQL 12 or higher

### 1. Clone the Repository
```bash
git clone https://github.com/Sameer0681/CivicResolve.git
cd CivicResolve
```

### 2. Database Configuration
Ensure PostgreSQL is running and create a database named `civicresolve_db`.

Update `src/main/resources/application.properties` with your database credentials:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/civicresolve_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

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

### Default Credentials
- **Admin**: [EMAIL_ADDRESS] / admin123
- **Citizen**: [EMAIL_ADDRESS] / citizen123

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.