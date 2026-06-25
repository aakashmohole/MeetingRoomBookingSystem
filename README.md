# Meeting Room Booking System (Roomify)

A premium web application built using **Spring MVC (5.3.39)**, **Hibernate (5.6.15.Final)**, **Lombok (1.18.46)**, and the **ZKOSS (9.6.0.2)** component-based UI framework for the frontend. 

The application features a secure user authentication system, role-based controls, a modern visual layout utilizing a slate-styled dashboard sidebar, booking schedulers with conflict prevention, and a notification alert system when booking statuses are updated by administrators.

---

## 🚀 Key Features

* **User Authentication**: Secure registration and login using **BCrypt** password hashing (`jbcrypt 0.4`).
* **Role-Based Views**: Dynamic workspace layouts adjusting interface panels and navigation sidebars automatically according to user role permissions (`ADMIN` vs. `EMPLOYEE`).
* **Admin Room Console**: Complete CRUD interface for administrators to manage room inventories (Room Name, Capacity, Location, and Amenities).
* **Employee Booking Scheduler**: Visual booking form to check room availability for any date/time slot, preventing overlapping reservations.
* **Status Updates with Remarks**: Administrators can approve or cancel bookings from the **System Bookings** console, triggering a popup modal to supply reasoning/remarks.
* **Employee Notification Banner**: Dynamically presents color-coded notification alerts (green for approved, red for cancelled) on the employee's dashboard listing the admin remarks, which can be dismissed.
* **Dockerized Container Architecture**: Pre-configured build systems using multi-service configurations for MySQL and Tomcat web application servers.

---

## 🛠️ Technology Stack & Dependencies

1. **Frontend UI**: ZK OSS (`zkbind`, `zkplus`, `zhtml` version `9.6.0.2`) styled with custom vanilla CSS variables.
2. **Backend Framework**: Spring MVC with declarative annotation-driven transaction management (`@Transactional`).
3. **Database Layer**: Pure Hibernate `SessionFactory` configuration (replaces JPA / JdbcTemplate).
4. **Boilerplate Reduction**: **Lombok** configured to reduce getter, setter, constructor, and builder code, fully compatible with JDK 17 to JDK 26.
5. **Embedded Server**: Tomcat 9 managed automatically via the `cargo-maven3-plugin` or packaged via Docker.

---

## 📂 Project Structure

```text
├── pom.xml                       # Maven build descriptor (Lombok, BCrypt & Cargo configurations)
├── Dockerfile                    # Container configuration file (uses Tomcat 9 on JDK 17)
├── docker-compose.yml            # Multi-service setup (MySQL 8 database + Tomcat Web App)
└── src/
    └── main/
        ├── java/com/meetingroom/
        │   ├── dao/             # Data Access Objects (UserDao, BookingDao, RoomDao)
        │   ├── model/           # Domain Entities (User, Room, Booking)
        │   ├── service/         # Business Logic Layer (UserService, BookingService, RoomService)
        │   ├── util/            # Toast notifications utilities
        │   └── viewmodel/       # ZK ViewModels (Login, Register, Main, Booking, Room, AdminBookings)
        ├── resources/
        │   ├── database.properties # Database connection settings
        │   └── schema.sql       # Database schema creation script (DDL)
        └── webapp/
            ├── WEB-INF/
            │   ├── spring-servlet.xml # Spring Beans, Hibernate SessionFactory & Transactions
            │   └── web.xml            # Servlet mapping & context listeners
            ├── sidebar.zul      # Reusable sidebar component with role-based links
            ├── index.zul        # Main dashboard console (employee alert banners)
            ├── login.zul        # Modern centered login interface
            ├── register.zul     # Modern signup interface
            ├── book_room.zul    # Employee room booking & scheduler
            ├── my_bookings.zul  # Employee personal booking history list
            ├── room_manage.zul  # Admin room CRUD inventory manager
            └── admin_bookings.zul  # Admin bookings supervisor console (status change remarks dialog)
```

---

## 💾 Database Configuration

The application uses MySQL. Table updates are managed automatically by Hibernate during development (`hibernate.hbm2ddl.auto=update`).

Configure local database credentials in **`src/main/resources/database.properties`**:
```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/meeting_room_booking?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=root
db.password=root
```

---

## 🏃 Run Local Server (Without Docker)

To compile, assemble the WAR file, and launch the application on a local Tomcat 9 instance automatically:

```bash
mvn clean package cargo:run
```

Once started, access the application at:
👉 `http://localhost:8080/meeting-room-booking-system/` *(automatically redirects to `login.zul` if no session exists)*.

---

## 🐳 Run Container Server (With Docker Compose)

To spin up both the database and application servers in container space without local configurations:

1. Build the WAR package:
   ```bash
   mvn clean package
   ```
2. Build and run the services using Docker Compose:
   ```bash
   docker-compose up -d --build
   ```
3. Access the application in your browser at:
   👉 **`http://localhost:8080/`** (maps directly to the root context)
4. Teardown:
   ```bash
   docker-compose down
   ```

*Note: The MySQL database container maps port `3307` on the host to avoid binding conflicts with your local machine's MySQL running on `3306`.*
