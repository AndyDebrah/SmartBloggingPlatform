# 📬 Postman Testing Guide - Epic 2 REST APIs

## 🚀 Getting Started

### 1. Start the Application

```bash
# Update pom.xml with Epic 1 configuration
# Make sure application-dev.properties has correct database credentials
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev