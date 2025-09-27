---

# PrivyCode – Backend Service

**Author:** Jai
**Project Type:** Full-Stack Application (Backend)
**Tech Stack:** Spring Boot, Java, MySQL/Postgres

---

## **Project Overview**

PrivyCode is a backend service that enables developers to securely share **read-only access to private GitHub repositories** via temporary viewer links.
It provides APIs to:

* Create and manage viewer links
* Track views and handle expiration
* Fetch repository content (without download)
* Maintain secure and scalable backend architecture

This backend is part of a **full-stack application**, with Angular as the frontend.

---

## **Features**

* **Viewer Link Management:** Create, update, delete temporary read-only links
* **Access Control:** Limit views per link and set expiration timestamps
* **Secure Repository Access:** Encrypted tokens for safe GitHub repo interaction
* **DTO & Service Layer:** Clean architecture for maintainability and scalability
* **Logging:** Comprehensive logging for debugging and monitoring

---

## **Tech Stack & Tools**

* **Backend:** Spring Boot, Java 17+
* **Database:** MySQL/Postgres
* **Security:** Encryption for tokens
* **Dependencies:** Spring Web, Spring Data JPA, Lombok (optional), Validation

---

## **API Endpoints**

| Endpoint             | Method | Description              |
| -------------------- | ------ | ------------------------ |
| `/viewer-links`      | POST   | Create a new viewer link |
| `/viewer-links`      | GET    | Get all viewer links     |
| `/viewer-links/{id}` | GET    | Get a viewer link by ID  |
| `/viewer-links/{id}` | PUT    | Update a viewer link     |
| `/viewer-links/{id}` | DELETE | Delete a viewer link     |

*(Add more endpoints if needed, e.g., GitHub OAuth integration later)*

---

## **Setup Instructions**

1. Clone the repository:

```bash
git clone <your-repo-link>
cd privycode-backend
```

2. Configure database in `application.properties` or `application.yml`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/privycode
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
```

3. Build and run the project:

```bash
./mvnw clean install
./mvnw spring-boot:run
```

4. Access API endpoints at:

```
http://localhost:8080
```

---

## **Future Enhancements**

* GitHub OAuth2 integration for automated token handling
* Advanced role-based access control
* Full frontend integration with Angular project
* Notifications for link expiry

---

## **Demo & Project Summary**

* [Demo Video Link](https://drive.google.com/file/d/1FhyNal4D_cMGuzl7r_OQdQGg82jWoxiJ/view?usp=drive_link)

* <img width="1338" height="670" alt="Screenshot 2025-08-13 101621" src="https://github.com/user-attachments/assets/dc0e8796-dd9f-4944-8a59-180424143c26" />

  

---

## **Contact**

**Author:** Jai
**Email:** [jaytechy33@gmail.com]
**LinkedIn:** [https://www.linkedin.com/in/tellurijayasekharreddy/]

---
