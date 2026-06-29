CREATE DATABASE IF NOT EXISTS meeting_room_booking;

USE meeting_room_booking;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
);

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
    admin_remarks VARCHAR(255),
    notification_read BOOLEAN NOT NULL DEFAULT TRUE,
    recurrence_id VARCHAR(100) NULL,
    recurrence_type VARCHAR(50) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS booking_invitees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);