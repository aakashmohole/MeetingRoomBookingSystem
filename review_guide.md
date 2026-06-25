# Manager Review Guide: Meeting Room Booking System

This guide outlines the quick-start steps to run and review the **Meeting Room Booking System** using Docker. You do **not** need Java, Maven, MySQL, or the source code repository set up locally. 

---

## 1. Prerequisites
Ensure you have **Docker** and **Docker Desktop** installed and running on your machine:
* [Download Docker Desktop](https://www.docker.com/products/docker-desktop/)

---

## 2. Setup & Execution

1. **Create an empty folder** on your machine (e.g., `meeting-room-review/`).
2. Inside that folder, create a file named **`docker-compose.yml`** and paste the following content:

```yaml
services:
  db:
    image: mysql:8.0
    container_name: room-booking-db
    restart: always
    environment:
      MYSQL_DATABASE: meeting_room_booking
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3307:3306"
    volumes:
      - db-data:/var/lib/mysql

  web:
    image: aakashmohole/room-booking-app:latest
    container_name: room-booking-app
    restart: always
    ports:
      - "8080:8080"
    depends_on:
      - db
    environment:
      CATALINA_OPTS: "-Ddb.url=jdbc:mysql://db:3306/meeting_room_booking?useSSL=false\\&allowPublicKeyRetrieval=true\\&serverTimezone=UTC -Ddb.username=root -Ddb.password=root"

volumes:
  db-data:
```

3. Open your terminal inside that folder and run:
   ```bash
   docker-compose up -d
   ```
   *This will pull the MySQL database image and the pre-built application image from Docker Hub (`aakashmohole/room-booking-app:latest`), create the containers, and link them automatically. Hibernate will dynamically generate the required database tables.*

---

## 3. Accessing the Application

Once starting the containers, open your browser and navigate to:
👉 **`http://localhost:8080/`** (redirects to the login console)

---

## 4. Suggested Testing Workflow (Self-Service)

Since the database starts fresh, follow this workflow to test both user roles (Admin & Employee) and the new status remarks notification features:

### Step A: Register the Admin Account
1. On the login page, click **Register**.
2. Fill out the registration form:
   * **Username**: `admin1`
   * **Role**: Select **Admin** from the drop-down.
3. Click **Sign Up** and sign in.

### Step B: Create Meeting Rooms (Admin Console)
1. Go to **Manage Rooms** via the sidebar.
2. Create 1 or 2 rooms (e.g., Room Name: `Boardroom`, Capacity: `12`, Location: `4th Floor`, Amenities: `Projector, Whiteboard`).

### Step C: Register the Employee Account
1. Log out.
2. Click **Register** on the login page.
3. Fill out the form:
   * **Username**: `employee1`
   * **Role**: Select **Employee**.
4. Sign in.

### Step D: Create a Booking (Employee Console)
1. Go to **Book a Room** in the sidebar.
2. Select your desired date/time slot, add a purpose, select the room, and click **Book**.
3. Log out.

### Step E: Review & Approve with Remarks (Admin Console)
1. Log in as the Admin (`admin1`).
2. Go to **System Bookings**.
3. Click **Confirm** (or **Cancel**) on the booking request.
4. A **modal popup** will appear. Enter a reason (e.g., *"Approved: Room assigned for board meeting"*), and click **Submit**.
5. Log out.

### Step F: Check Dynamic Notifications (Employee Dashboard)
1. Log back in as `employee1`.
2. Look at the top of the **Dashboard**. You will see a premium color-coded notification card containing the booking details and the exact status remarks entered by the Admin.
3. Click **Dismiss** to clear the notification.

---

## 5. Teardown
To stop the services and release container resources, run:
```bash
docker-compose down
```
*(Add the `-v` flag if you want to wipe the test database volumes clean: `docker-compose down -v`)*
