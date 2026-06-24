# Room CRUD and Double-Booking Prevention Guide

This document contains the database schemas, Hibernate entities, DAOs, Services, ViewModels, and UI files required to build Room CRUD operations (Admin console) and Booking Management with conflict check prevention.

---

## 1. Database Table Structures

Run these SQL scripts in your MySQL client to define the `rooms` and `bookings` tables:

```sql
USE meeting_room_booking;

-- Table for Room Inventory
CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    location VARCHAR(150) NOT NULL,
    amenities VARCHAR(255) -- E.g. 'Projector, Whiteboard, Video Conference'
);

-- Table for Room Bookings
CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED', -- 'CONFIRMED' or 'CANCELLED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);
```

---

## 2. Model Entities

### com.meetingroom.model.Room
```java
package com.meetingroom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "amenities")
    private String amenities;
}
```

### com.meetingroom.model.Booking
```java
package com.meetingroom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "purpose", nullable = false)
    private String purpose;

    @Column(name = "status", nullable = false)
    private String status; // 'CONFIRMED', 'CANCELLED'
}
```

---

## 3. Data Access Objects (DAO Layer)

### com.meetingroom.dao.RoomDao (Interface)
```java
package com.meetingroom.dao;

import com.meetingroom.model.Room;
import java.util.List;

public interface RoomDao {
    void save(Room room);
    void update(Room room);
    void delete(Long roomId);
    Room findById(Long roomId);
    Room findByName(String name);
    List<Room> findAll();
}
```

### com.meetingroom.dao.HibernateRoomDao (Implementation)
```java
package com.meetingroom.dao;

import com.meetingroom.model.Room;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
@Transactional
public class HibernateRoomDao implements RoomDao {

    private final SessionFactory sessionFactory;

    public HibernateRoomDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void save(Room room) {
        sessionFactory.getCurrentSession().save(room);
    }

    @Override
    public void update(Room room) {
        sessionFactory.getCurrentSession().update(room);
    }

    @Override
    public void delete(Long roomId) {
        Room room = findById(roomId);
        if (room != null) {
            sessionFactory.getCurrentSession().delete(room);
        }
    }

    @Override
    public Room findById(Long roomId) {
        return sessionFactory.getCurrentSession().get(Room.class, roomId);
    }

    @Override
    public Room findByName(String name) {
        return sessionFactory.getCurrentSession()
                .createQuery("from Room where name = :name", Room.class)
                .setParameter("name", name)
                .uniqueResult();
    }

    @Override
    public List<Room> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("from Room order by name", Room.class)
                .list();
    }
}
```

### com.meetingroom.dao.BookingDao (Interface)
```java
package com.meetingroom.dao;

import com.meetingroom.model.Booking;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingDao {
    void save(Booking booking);
    void update(Booking booking);
    Booking findById(Long bookingId);
    List<Booking> findByUserId(Long userId);
    List<Booking> findByRoomId(Long roomId);
    List<Booking> findAll();
    
    // Checks for overlapping bookings on the same room
    boolean hasOverlap(Long roomId, LocalDate date, LocalTime start, LocalTime end, Long excludeBookingId);
}
```

### com.meetingroom.dao.HibernateBookingDao (Implementation)
```java
package com.meetingroom.dao;

import com.meetingroom.model.Booking;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
@Transactional
public class HibernateBookingDao implements BookingDao {

    private final SessionFactory sessionFactory;

    public HibernateBookingDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void save(Booking booking) {
        sessionFactory.getCurrentSession().save(booking);
    }

    @Override
    public void update(Booking booking) {
        sessionFactory.getCurrentSession().update(booking);
    }

    @Override
    public Booking findById(Long bookingId) {
        return sessionFactory.getCurrentSession().get(Booking.class, bookingId);
    }

    @Override
    public List<Booking> findByUserId(Long userId) {
        return sessionFactory.getCurrentSession()
                .createQuery("from Booking where user.id = :userId order by bookingDate desc, startTime desc", Booking.class)
                .setParameter("userId", userId)
                .list();
    }

    @Override
    public List<Booking> findByRoomId(Long roomId) {
        return sessionFactory.getCurrentSession()
                .createQuery("from Booking where room.id = :roomId order by bookingDate desc", Booking.class)
                .setParameter("roomId", roomId)
                .list();
    }

    @Override
    public List<Booking> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("from Booking order by bookingDate desc, startTime desc", Booking.class)
                .list();
    }

    @Override
    public boolean hasOverlap(Long roomId, LocalDate date, LocalTime start, LocalTime end, Long excludeBookingId) {
        String hql = "select count(b) from Booking b " +
                     "where b.room.id = :roomId " +
                     "and b.bookingDate = :bookingDate " +
                     "and b.status = 'CONFIRMED' " +
                     "and b.startTime < :endTime " +
                     "and b.endTime > :startTime ";
        
        if (excludeBookingId != null) {
            hql += "and b.id != :excludeBookingId";
        }

        var query = sessionFactory.getCurrentSession()
                .createQuery(hql, Long.class)
                .setParameter("roomId", roomId)
                .setParameter("bookingDate", date)
                .setParameter("startTime", start)
                .setParameter("endTime", end);

        if (excludeBookingId != null) {
            query.setParameter("excludeBookingId", excludeBookingId);
        }

        Long count = query.uniqueResult();
        return count != null && count > 0;
    }
}
```

---

## 4. Business Logic Services (Service Layer)

### com.meetingroom.service.RoomService (Interface)
```java
package com.meetingroom.service;

import com.meetingroom.model.Room;
import java.util.List;

public interface RoomService {
    void addRoom(Room room) throws Exception;
    void updateRoom(Room room) throws Exception;
    void deleteRoom(Long roomId);
    Room getRoomById(Long roomId);
    List<Room> getAllRooms();
}
```

### com.meetingroom.service.RoomServiceImpl (Implementation)
```java
package com.meetingroom.service;

import com.meetingroom.dao.RoomDao;
import com.meetingroom.model.Room;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service("roomService")
@Transactional
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomDao roomDao;

    public RoomServiceImpl(RoomDao roomDao) {
        this.roomDao = roomDao;
    }

    @Override
    public void addRoom(Room room) throws Exception {
        log.info("Attempting to add room: {}", room.getName());
        if (roomDao.findByName(room.getName()) != null) {
            log.warn("Room name already exists: {}", room.getName());
            throw new Exception("Room name already exists.");
        }
        roomDao.save(room);
        log.debug("Successfully saved room: {}", room.getName());
    }

    @Override
    public void updateRoom(Room room) throws Exception {
        log.info("Attempting to update room ID: {}", room.getId());
        Room existing = roomDao.findByName(room.getName());
        if (existing != null && !existing.getId().equals(room.getId())) {
            log.warn("Another room with same name exists: {}", room.getName());
            throw new Exception("Another room with this name already exists.");
        }
        roomDao.update(room);
        log.debug("Successfully updated room: {}", room.getName());
    }

    @Override
    public void deleteRoom(Long roomId) {
        log.info("Deleting room ID: {}", roomId);
        roomDao.delete(roomId);
    }

    @Override
    public Room getRoomById(Long roomId) {
        return roomDao.findById(roomId);
    }

    @Override
    public List<Room> getAllRooms() {
        return roomDao.findAll();
    }
}
```

### com.meetingroom.service.BookingService (Interface)
```java
package com.meetingroom.service;

import com.meetingroom.model.Booking;
import java.util.List;

public interface BookingService {
    void createBooking(Booking booking) throws Exception;
    void updateBooking(Booking booking) throws Exception;
    void cancelBooking(Long bookingId);
    List<Booking> getBookingsByUser(Long userId);
    List<Booking> getBookingsByRoom(Long roomId);
    List<Booking> getAllBookings();
}
```

### com.meetingroom.service.BookingServiceImpl (Implementation)
```java
package com.meetingroom.service;

import com.meetingroom.dao.BookingDao;
import com.meetingroom.model.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service("bookingService")
@Transactional
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingDao bookingDao;

    public BookingServiceImpl(BookingDao bookingDao) {
        this.bookingDao = bookingDao;
    }

    @Override
    public void createBooking(Booking booking) throws Exception {
        log.info("Creating booking request for Room ID: {} on {}", booking.getRoom().getId(), booking.getBookingDate());
        
        // Time checks
        if (booking.getStartTime().isAfter(booking.getEndTime()) || booking.getStartTime().equals(booking.getEndTime())) {
            throw new Exception("Start time must be before end time.");
        }

        // Conflict check
        boolean conflict = bookingDao.hasOverlap(
                booking.getRoom().getId(), 
                booking.getBookingDate(), 
                booking.getStartTime(), 
                booking.getEndTime(),
                null
        );

        if (conflict) {
            log.warn("Booking conflict detected for Room ID: {}", booking.getRoom().getId());
            throw new Exception("This room is already booked for the selected date and time.");
        }

        booking.setStatus("CONFIRMED");
        bookingDao.save(booking);
        log.debug("Successfully saved booking ID: {}", booking.getId());
    }

    @Override
    public void updateBooking(Booking booking) throws Exception {
        log.info("Updating booking request for Booking ID: {}", booking.getId());

        if (booking.getStartTime().isAfter(booking.getEndTime()) || booking.getStartTime().equals(booking.getEndTime())) {
            throw new Exception("Start time must be before end time.");
        }

        // Conflict check excluding current booking
        boolean conflict = bookingDao.hasOverlap(
                booking.getRoom().getId(), 
                booking.getBookingDate(), 
                booking.getStartTime(), 
                booking.getEndTime(),
                booking.getId()
        );

        if (conflict) {
            log.warn("Booking conflict detected on update for Room ID: {}", booking.getRoom().getId());
            throw new Exception("This room is already booked for the selected date and time.");
        }

        bookingDao.update(booking);
        log.debug("Successfully updated booking ID: {}", booking.getId());
    }

    @Override
    public void cancelBooking(Long bookingId) {
        log.info("Cancelling booking ID: {}", bookingId);
        Booking booking = bookingDao.findById(bookingId);
        if (booking != null) {
            booking.setStatus("CANCELLED");
            bookingDao.update(booking);
            log.debug("Booking cancelled successfully.");
        }
    }

    @Override
    public List<Booking> getBookingsByUser(Long userId) {
        return bookingDao.findByUserId(userId);
    }

    @Override
    public List<Booking> getBookingsByRoom(Long roomId) {
        return bookingDao.findByRoomId(roomId);
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingDao.findAll();
    }
}
```

---

## 5. UI Views and ViewModels

### com.meetingroom.viewmodel.RoomManageViewModel (Admin CRUD VM)
```java
package com.meetingroom.viewmodel;

import com.meetingroom.model.Room;
import com.meetingroom.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import java.util.List;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
@Slf4j
public class RoomManageViewModel {

    @WireVariable
    private RoomService roomService;

    private List<Room> rooms;
    private Room selectedRoom = new Room(); // For bindings
    private String feedbackMessage;
    private String errorFeedback;
    private boolean editing = false;

    @Init
    public void init() {
        rooms = roomService.getAllRooms();
    }

    @Command
    @NotifyChange({"rooms", "selectedRoom", "feedbackMessage", "errorFeedback", "editing"})
    public void saveRoom() {
        feedbackMessage = null;
        errorFeedback = null;
        try {
            if (selectedRoom.getName() == null || selectedRoom.getName().trim().isEmpty()) {
                errorFeedback = "Room name is required.";
                return;
            }
            if (selectedRoom.getCapacity() <= 0) {
                errorFeedback = "Capacity must be positive.";
                return;
            }
            if (selectedRoom.getLocation() == null || selectedRoom.getLocation().trim().isEmpty()) {
                errorFeedback = "Location is required.";
                return;
            }

            if (selectedRoom.getId() == null) {
                roomService.addRoom(selectedRoom);
                feedbackMessage = "Room added successfully!";
            } else {
                roomService.updateRoom(selectedRoom);
                feedbackMessage = "Room updated successfully!";
            }
            rooms = roomService.getAllRooms();
            selectedRoom = new Room();
            editing = false;
        } catch (Exception e) {
            errorFeedback = e.getMessage();
        }
    }

    @Command
    @NotifyChange({"selectedRoom", "editing", "errorFeedback", "feedbackMessage"})
    public void editRoom(@BindingParam("room") Room room) {
        selectedRoom = Room.builder()
                .id(room.getId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .location(room.getLocation())
                .amenities(room.getAmenities())
                .build();
        editing = true;
        feedbackMessage = null;
        errorFeedback = null;
    }

    @Command
    @NotifyChange({"rooms", "selectedRoom", "editing", "feedbackMessage", "errorFeedback"})
    public void deleteRoom(@BindingParam("id") Long id) {
        roomService.deleteRoom(id);
        rooms = roomService.getAllRooms();
        selectedRoom = new Room();
        editing = false;
        feedbackMessage = "Room deleted successfully!";
        errorFeedback = null;
    }

    @Command
    @NotifyChange({"selectedRoom", "editing", "errorFeedback", "feedbackMessage"})
    public void cancelEdit() {
        selectedRoom = new Room();
        editing = false;
        feedbackMessage = null;
        errorFeedback = null;
    }

    // Getters and Setters
    public List<Room> getRooms() { return rooms; }
    public Room getSelectedRoom() { return selectedRoom; }
    public void setSelectedRoom(Room selectedRoom) { this.selectedRoom = selectedRoom; }
    public String getFeedbackMessage() { return feedbackMessage; }
    public String getErrorFeedback() { return errorFeedback; }
    public boolean isEditing() { return editing; }
}
```

### ZK View: room_manage.zul (Admin UI page)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<zk xmlns="http://www.zkoss.org/2005/zul"
    xmlns:h="http://www.w3.org/1999/xhtml">
    <style>
        body {
            background-color: #0f172a;
            color: #f8fafc;
            font-family: 'Inter', sans-serif;
            margin: 0;
            padding: 0;
        }
        .container {
            max-width: 1000px;
            margin: 40px auto;
            padding: 20px;
        }
        .header {
            margin-bottom: 24px;
        }
        .header h1 {
            font-size: 2rem;
            font-weight: 800;
            background: linear-gradient(135deg, #38bdf8, #818cf8);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin: 0;
        }
        .card {
            background: rgba(30, 41, 59, 0.7);
            backdrop-filter: blur(10px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 16px;
            padding: 24px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
            margin-bottom: 24px;
        }
        .form-group {
            margin-bottom: 16px;
        }
        .form-label {
            display: block;
            margin-bottom: 6px;
            font-size: 0.85rem;
            color: #94a3b8;
            font-weight: 500;
        }
        .form-input {
            background: rgba(15, 23, 42, 0.6);
            border: 1px solid rgba(255, 255, 255, 0.15);
            border-radius: 8px;
            padding: 10px 14px;
            color: #f8fafc;
            width: 100%;
            box-sizing: border-box;
            font-size: 0.95rem;
        }
        .form-input:focus {
            border-color: #38bdf8;
            outline: none;
        }
        .btn-action {
            background: linear-gradient(135deg, #38bdf8, #818cf8);
            border: none;
            border-radius: 8px;
            color: white;
            padding: 10px 20px;
            font-weight: 600;
            cursor: pointer;
        }
        .btn-cancel {
            background: rgba(148, 163, 184, 0.1);
            border: 1px solid rgba(148, 163, 184, 0.25);
            border-radius: 8px;
            color: #e2e8f0;
            padding: 10px 20px;
            font-weight: 600;
            cursor: pointer;
            margin-left: 10px;
        }
        .success-box {
            background: rgba(16, 185, 129, 0.1);
            border: 1px solid rgba(16, 185, 129, 0.2);
            color: #34d399;
            padding: 10px;
            border-radius: 8px;
            margin-bottom: 16px;
            text-align: center;
        }
        .error-box {
            background: rgba(239, 68, 68, 0.1);
            border: 1px solid rgba(239, 68, 68, 0.2);
            color: #f87171;
            padding: 10px;
            border-radius: 8px;
            margin-bottom: 16px;
            text-align: center;
        }
        .z-listbox {
            border: none;
            background: transparent;
        }
        .z-listheader {
            background: #1e293b;
            color: #94a3b8;
            font-weight: 600;
            padding: 12px;
        }
        .z-listitem {
            background: transparent;
            border-bottom: 1px solid #334155;
        }
        .z-listcell {
            color: #e2e8f0;
            padding: 14px;
        }
    </style>

    <div class="container" viewModel="@id('vm') @init('com.meetingroom.viewmodel.RoomManageViewModel')">
        <div class="header">
            <h:h1>Manage Meeting Rooms</h:h1>
        </div>

        <div class="card">
            <h:h3 style="margin-top:0; color:#f8fafc;"><label value="@load(vm.editing ? 'Edit Room' : 'Add New Room')"/></h:h3>

            <div class="success-box" if="${not empty vm.feedbackMessage}">
                <label value="@load(vm.feedbackMessage)"/>
            </div>

            <div class="error-box" if="${not empty vm.errorFeedback}">
                <label value="@load(vm.errorFeedback)"/>
            </div>

            <div class="form-group">
                <h:label class="form-label">Room Name</h:label>
                <textbox value="@bind(vm.selectedRoom.name)" class="form-input" placeholder="e.g. Boardroom A"/>
            </div>

            <div class="form-group">
                <h:label class="form-label">Capacity</h:label>
                <intbox value="@bind(vm.selectedRoom.capacity)" class="form-input" placeholder="e.g. 10"/>
            </div>

            <div class="form-group">
                <h:label class="form-label">Location / Floor</h:label>
                <textbox value="@bind(vm.selectedRoom.location)" class="form-input" placeholder="e.g. 2nd Floor, West Wing"/>
            </div>

            <div class="form-group">
                <h:label class="form-label">Amenities (Comma separated)</h:label>
                <textbox value="@bind(vm.selectedRoom.amenities)" class="form-input" placeholder="e.g. Projector, Whiteboard, TV"/>
            </div>

            <button label="Save Room" onClick="@command('saveRoom')" class="btn-action"/>
            <button label="Cancel" onClick="@command('cancelEdit')" class="btn-cancel" if="${vm.editing}"/>
        </div>

        <div class="card">
            <h:h3 style="margin-top:0; color:#f8fafc;">Room Inventory List</h:h3>
            <listbox model="@load(vm.rooms)" emptyMessage="No rooms found.">
                <listhead>
                    <listheader label="Room Name" width="25%"/>
                    <listheader label="Capacity" width="15%"/>
                    <listheader label="Location" width="30%"/>
                    <listheader label="Amenities" width="20%"/>
                    <listheader label="Actions" width="10%"/>
                </listhead>
                <template name="model" var="room">
                    <listitem>
                        <listcell><label value="@load(room.name)" style="font-weight:bold;"/></listcell>
                        <listcell><label value="@load(room.capacity)"/><label value=" seats"/></listcell>
                        <listcell><label value="@load(room.location)"/></listcell>
                        <listcell><label value="@load(room.amenities)"/></listcell>
                        <listcell>
                            <hlayout spacing="8px">
                                <a label="Edit" onClick="@command('editRoom', room=room)" style="color:#38bdf8; cursor:pointer; font-weight:600;"/>
                                <a label="Delete" onClick="@command('deleteRoom', id=room.id)" style="color:#f87171; cursor:pointer; font-weight:600;"/>
                            </hlayout>
                        </listcell>
                    </listitem>
                </template>
            </listbox>
        </div>
    </div>
</zk>
```

---

### com.meetingroom.viewmodel.EmployeeBookingViewModel (Employee VM)
```java
package com.meetingroom.viewmodel;

import com.meetingroom.model.Booking;
import com.meetingroom.model.Room;
import com.meetingroom.model.User;
import com.meetingroom.service.BookingService;
import com.meetingroom.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
@Slf4j
public class EmployeeBookingViewModel {

    @WireVariable
    private RoomService roomService;

    @WireVariable
    private BookingService bookingService;

    private User currentUser;
    private List<Room> rooms;
    private List<Booking> myBookings;

    // Filter & Booking State variables
    private Room selectedRoom;
    private Date selectDate = new Date(); // Bound to ZK datebox
    private String startHour = "09";     // Bound to listbox hours
    private String startMin = "00";
    private String endHour = "10";
    private String endMin = "00";
    private String purpose;

    private String successMessage;
    private String errorMessage;

    @Init
    public void init() {
        currentUser = (User) Sessions.getCurrent().getAttribute("currentUser");
        if (currentUser == null) {
            Executions.sendRedirect("login.zul");
            return;
        }
        rooms = roomService.getAllRooms();
        myBookings = bookingService.getBookingsByUser(currentUser.getId());
    }

    @Command
    @NotifyChange({"myBookings", "successMessage", "errorMessage", "purpose"})
    public void bookSelectedRoom(@BindingParam("room") Room room) {
        successMessage = null;
        errorMessage = null;
        
        if (selectDate == null) {
            errorMessage = "Please select a date.";
            return;
        }
        if (purpose == null || purpose.trim().isEmpty()) {
            errorMessage = "Please specify the purpose of this meeting.";
            return;
        }

        try {
            LocalDate date = selectDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalTime start = LocalTime.of(Integer.parseInt(startHour), Integer.parseInt(startMin));
            LocalTime end = LocalTime.of(Integer.parseInt(endHour), Integer.parseInt(endMin));

            Booking booking = Booking.builder()
                    .user(currentUser)
                    .room(room)
                    .bookingDate(date)
                    .startTime(start)
                    .endTime(end)
                    .purpose(purpose.trim())
                    .build();

            bookingService.createBooking(booking);
            successMessage = "Successfully booked room: " + room.getName();
            purpose = ""; // Clear input
            myBookings = bookingService.getBookingsByUser(currentUser.getId());
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }
    }

    @Command
    @NotifyChange({"myBookings", "successMessage", "errorMessage"})
    public void cancelBooking(@BindingParam("id") Long id) {
        bookingService.cancelBooking(id);
        myBookings = bookingService.getBookingsByUser(currentUser.getId());
        successMessage = "Booking cancelled successfully.";
        errorMessage = null;
    }

    // Getters and Setters
    public List<Room> getRooms() { return rooms; }
    public List<Booking> getMyBookings() { return myBookings; }
    public Date getSelectDate() { return selectDate; }
    public void setSelectDate(Date selectDate) { this.selectDate = selectDate; }
    public String getStartHour() { return startHour; }
    public void setStartHour(String startHour) { this.startHour = startHour; }
    public String getStartMin() { return startMin; }
    public void setStartMin(String startMin) { this.startMin = startMin; }
    public String getEndHour() { return endHour; }
    public void setEndHour(String endHour) { this.endHour = endHour; }
    public String getEndMin() { return endMin; }
    public void setEndMin(String endMin) { this.endMin = endMin; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getSuccessMessage() { return successMessage; }
    public String getErrorMessage() { return errorMessage; }
}
```

### ZK View: book_room.zul (Employee Booking Page)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<zk xmlns="http://www.zkoss.org/2005/zul"
    xmlns:h="http://www.w3.org/1999/xhtml">
    <style>
        body {
            background-color: #0f172a;
            color: #f8fafc;
            font-family: 'Inter', sans-serif;
            margin: 0;
            padding: 0;
        }
        .container {
            max-width: 1000px;
            margin: 40px auto;
            padding: 20px;
        }
        .header {
            margin-bottom: 24px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .header h1 {
            font-size: 2rem;
            font-weight: 800;
            background: linear-gradient(135deg, #38bdf8, #818cf8);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin: 0;
        }
        .btn-back {
            background: rgba(148, 163, 184, 0.1);
            border: 1px solid rgba(148, 163, 184, 0.25);
            border-radius: 8px;
            color: #e2e8f0;
            padding: 8px 16px;
            font-weight: 600;
            cursor: pointer;
        }
        .card {
            background: rgba(30, 41, 59, 0.7);
            backdrop-filter: blur(10px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 16px;
            padding: 24px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
            margin-bottom: 24px;
        }
        .form-row {
            display: flex;
            gap: 16px;
            margin-bottom: 16px;
            flex-wrap: wrap;
        }
        .form-item {
            flex: 1;
            min-width: 200px;
        }
        .form-label {
            display: block;
            margin-bottom: 6px;
            font-size: 0.85rem;
            color: #94a3b8;
            font-weight: 500;
        }
        .form-input {
            background: rgba(15, 23, 42, 0.6);
            border: 1px solid rgba(255, 255, 255, 0.15);
            border-radius: 8px;
            padding: 10px 14px;
            color: #f8fafc;
            width: 100%;
            box-sizing: border-box;
            font-size: 0.95rem;
        }
        select.form-input option {
            background-color: #1e293b;
            color: #f8fafc;
        }
        .time-picker {
            display: flex;
            align-items: center;
            gap: 6px;
        }
        .btn-book {
            background: linear-gradient(135deg, #38bdf8, #818cf8);
            border: none;
            border-radius: 8px;
            color: white;
            padding: 8px 16px;
            font-weight: 600;
            cursor: pointer;
        }
        .btn-cancel {
            background: rgba(239, 68, 68, 0.2);
            border: 1px solid rgba(239, 68, 68, 0.4);
            border-radius: 8px;
            color: #f87171;
            padding: 6px 12px;
            font-weight: 600;
            cursor: pointer;
        }
        .success-box {
            background: rgba(16, 185, 129, 0.1);
            border: 1px solid rgba(16, 185, 129, 0.2);
            color: #34d399;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 16px;
            text-align: center;
        }
        .error-box {
            background: rgba(239, 68, 68, 0.1);
            border: 1px solid rgba(239, 68, 68, 0.2);
            color: #f87171;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 16px;
            text-align: center;
        }
        .z-datebox-input {
            background: rgba(15, 23, 42, 0.6) !important;
            color: #f8fafc !important;
            border: 1px solid rgba(255, 255, 255, 0.15) !important;
            border-radius: 8px !important;
            height: 38px;
            padding: 10px 14px;
        }
        .z-listbox {
            border: none;
            background: transparent;
        }
        .z-listheader {
            background: #1e293b;
            color: #94a3b8;
            font-weight: 600;
            padding: 12px;
        }
        .z-listitem {
            background: transparent;
            border-bottom: 1px solid #334155;
        }
        .z-listcell {
            color: #e2e8f0;
            padding: 14px;
        }
    </style>

    <div class="container" viewModel="@id('vm') @init('com.meetingroom.viewmodel.EmployeeBookingViewModel')">
        <div class="header">
            <h:h1>Book a Meeting Room</h:h1>
            <button label="Back to Dashboard" onClick="Executions.sendRedirect(&quot;index.zul&quot;)" class="btn-back"/>
        </div>

        <div class="card">
            <h:h3 style="margin-top:0; color:#f8fafc;">1. Select Date &amp; Time Slot</h:h3>

            <div class="success-box" if="${not empty vm.successMessage}">
                <label value="@load(vm.successMessage)"/>
            </div>

            <div class="error-box" if="${not empty vm.errorMessage}">
                <label value="@load(vm.errorMessage)"/>
            </div>

            <div class="form-row">
                <div class="form-item">
                    <h:label class="form-label">Booking Date</h:label>
                    <datebox value="@bind(vm.selectDate)" format="yyyy-MM-dd" constraint="no past" width="100%"/>
                </div>

                <div class="form-item">
                    <h:label class="form-label">Start Time</h:label>
                    <div class="time-picker">
                        <h:select value="@bind(vm.startHour)" class="form-input" style="width: 80px;">
                            <h:option value="08">08</h:option>
                            <h:option value="09">09</h:option>
                            <h:option value="10">10</h:option>
                            <h:option value="11">11</h:option>
                            <h:option value="12">12</h:option>
                            <h:option value="13">13</h:option>
                            <h:option value="14">14</h:option>
                            <h:option value="15">15</h:option>
                            <h:option value="16">16</h:option>
                            <h:option value="17">17</h:option>
                            <h:option value="18">18</h:option>
                        </h:select>
                        <label value=":" style="color: #94a3b8; font-weight: bold;"/>
                        <h:select value="@bind(vm.startMin)" class="form-input" style="width: 80px;">
                            <h:option value="00">00</h:option>
                            <h:option value="30">30</h:option>
                        </h:select>
                    </div>
                </div>

                <div class="form-item">
                    <h:label class="form-label">End Time</h:label>
                    <div class="time-picker">
                        <h:select value="@bind(vm.endHour)" class="form-input" style="width: 80px;">
                            <h:option value="08">08</h:option>
                            <h:option value="09">09</h:option>
                            <h:option value="10">10</h:option>
                            <h:option value="11">11</h:option>
                            <h:option value="12">12</h:option>
                            <h:option value="13">13</h:option>
                            <h:option value="14">14</h:option>
                            <h:option value="15">15</h:option>
                            <h:option value="16">16</h:option>
                            <h:option value="17">17</h:option>
                            <h:option value="18">18</h:option>
                            <h:option value="19">19</h:option>
                        </h:select>
                        <label value=":" style="color: #94a3b8; font-weight: bold;"/>
                        <h:select value="@bind(vm.endMin)" class="form-input" style="width: 80px;">
                            <h:option value="00">00</h:option>
                            <h:option value="30">30</h:option>
                        </h:select>
                    </div>
                </div>
            </div>

            <div class="form-group">
                <h:label class="form-label">Purpose of Meeting</h:label>
                <textbox value="@bind(vm.purpose)" class="form-input" placeholder="e.g. Sprint Planning, Project Review"/>
            </div>
        </div>

        <div class="card">
            <h:h3 style="margin-top:0; color:#f8fafc;">2. Select a Room to Book</h:h3>
            <listbox model="@load(vm.rooms)" emptyMessage="No rooms configured.">
                <listhead>
                    <listheader label="Room Name" width="30%"/>
                    <listheader label="Capacity" width="20%"/>
                    <listheader label="Location" width="30%"/>
                    <listheader label="Amenities" width="10%"/>
                    <listheader label="Action" width="10%"/>
                </listhead>
                <template name="model" var="room">
                    <listitem>
                        <listcell><label value="@load(room.name)" style="font-weight:bold;"/></listcell>
                        <listcell><label value="@load(room.capacity)"/><label value=" seats"/></listcell>
                        <listcell><label value="@load(room.location)"/></listcell>
                        <listcell><label value="@load(room.amenities)"/></listcell>
                        <listcell>
                            <button label="Book" onClick="@command('bookSelectedRoom', room=room)" class="btn-book"/>
                        </listcell>
                    </listitem>
                </template>
            </listbox>
        </div>

        <div class="card">
            <h:h3 style="margin-top:0; color:#f8fafc;">My Bookings History</h:h3>
            <listbox model="@load(vm.myBookings)" emptyMessage="You have no bookings recorded.">
                <listhead>
                    <listheader label="Room Name" width="20%"/>
                    <listheader label="Booking Date" width="15%"/>
                    <listheader label="Start Time" width="15%"/>
                    <listheader label="End Time" width="15%"/>
                    <listheader label="Purpose" width="25%"/>
                    <listheader label="Status" width="10%"/>
                    <listheader label="Action" width="10%"/>
                </listhead>
                <template name="model" var="booking">
                    <listitem>
                        <listcell><label value="@load(booking.room.name)"/></listcell>
                        <listcell><label value="@load(booking.bookingDate)"/></listcell>
                        <listcell><label value="@load(booking.startTime)"/></listcell>
                        <listcell><label value="@load(booking.endTime)"/></listcell>
                        <listcell><label value="@load(booking.purpose)"/></listcell>
                        <listcell>
                            <label value="@load(booking.status)" 
                                   style="@load(booking.status eq 'CONFIRMED' ? 'color:#34d399; font-weight:600;' : 'color:#94a3b8; text-decoration:line-through;')"/>
                        </listcell>
                        <listcell>
                            <button label="Cancel" onClick="@command('cancelBooking', id=booking.id)" 
                                    class="btn-cancel" if="${booking.status eq 'CONFIRMED'}"/>
                        </listcell>
                    </listitem>
                </template>
            </listbox>
        </div>
    </div>
</zk>
```
