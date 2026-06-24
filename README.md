# Meeting Room Booking System

A premium web application built using **Spring MVC (5.3.39)**, **Hibernate (5.6.15.Final)**, **Lombok (1.18.46)**, and **ZKOSS (9.6.0.2)** component-based UI framework for the frontend. 

The application implements a secure session-based User Authentication Module (Registration and Login) with BCrypt password encryption.

---

## Technical Stack & Configuration

1. **Frontend UI**: ZK OSS (`zkbind`, `zkplus`, `zhtml` version `9.6.0.2`).
2. **Backend Core**: Spring MVC framework with declarative transaction management (`@Transactional`).
3. **ORM/Database Persistence**: Pure Hibernate `SessionFactory` configuration (replaces JPA / JdbcTemplate).
4. **Security**: Password encryption using **BCrypt** hashing (`jbcrypt 0.4`).
5. **Boilerplate Reduction**: **Lombok** configured to reduce getter, setter, constructor, and builder code, fully compatible with JDK 17 to JDK 26.
6. **Embedded Server**: Tomcat 9 managed automatically via the `cargo-maven3-plugin`.

---

## Current Project Structure

```text
├── pom.xml                       # Maven build descriptor (with ZK, Lombok, BCrypt & Cargo configurations)
└── src/
    └── main/
        ├── java/com/meetingroom/
        │   ├── dao/             # Data Access Objects (UserDao, HibernateUserDao)
        │   ├── model/           # Domain Entities (User)
        │   ├── service/         # Business Logic Layer (UserService, UserServiceImpl)
        │   └── viewmodel/       # ZK ViewModels (LoginViewModel, RegisterViewModel, MainViewModel)
        ├── resources/
        │   ├── database.properties # Database connection settings
        │   └── schema.sql       # Database schema creation script
        └── webapp/
            ├── WEB-INF/
            │   ├── spring-servlet.xml # Spring Bean definitions, Hibernate SessionFactory & Transactions
            │   └── web.xml            # Servlet mapping & context listeners
            ├── index.zul        # Dashboard page (verifies session, redirects or allows logout)
            ├── login.zul        # Modern login interface
            └── register.zul     # Modern registration interface
```

---

## Database Configuration

### 1. Table Schema (`users`)
The MySQL schema uses a clean configuration. Table updates are managed automatically by Hibernate during development (`hibernate.hbm2ddl.auto=update`).

```sql
CREATE DATABASE IF NOT EXISTS meeting_room_booking;
USE meeting_room_booking;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Holds BCrypt hashes
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
);
```

### 2. Setup database credentials
Configure database credentials in **[database.properties](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/resources/database.properties)**:
```ini
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/meeting_room_booking?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=root
db.password=root
```

---

## Build and Run

To compile, assemble the WAR file, and launch the application on a local Tomcat 9 instance automatically:

```bash
mvn clean package cargo:run
```

Once started, access the application at:
`http://localhost:8080/meeting-room-booking-system/` (automatically redirects to `login.zul` if no session exists).

---

## Next Modules to Implement

To evolve this project into a complete Meeting Room Booking System, the following modules are planned:

### Module 1: Role-Based Authorization
- **Goal**: Classify users into roles: `ADMIN` and `USER`.
- **Database change**: Add `role` column to `users` table (e.g. `role VARCHAR(20) DEFAULT 'USER'`).
- **Functionality**:
  - `ADMIN` users can access room management (create/edit/delete rooms).
  - `USER` users can book rooms and view their reservation histories.

### Module 2: Room Management (Admin Console)
- **Goal**: CRUD console for administrators to manage room inventories.
- **Form Inputs**: Room Name, Capacity, Location, and Amenities (e.g. Projector, Whiteboard).
- **View Layer**: Tabular management page within ZK.

### Module 3: Booking Management
- **Goal**: Booking and calendar scheduling module.
- **Functionality**:
  - Check availability of a room for a given date, start time, and end time.
  - Create a booking slot.
  - Prevent overlapping reservations on the same room.
  - Display user reservation history dashboard.

---

## Proposed Database Schema Expansion

To support the upcoming modules, the database schema will expand as follows:

### 1. `users` Table (Updated)
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER' -- 'ADMIN' or 'USER'
);
```

### 2. `rooms` Table
```sql
CREATE TABLE rooms (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    location VARCHAR(150) NOT NULL,
    amenities VARCHAR(255) -- Comma-separated list (e.g., 'Projector, Whiteboard, Video Conference')
);
```

### 3. `bookings` Table
```sql
CREATE TABLE bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    purpose VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED', -- 'CONFIRMED', 'CANCELLED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);
```
