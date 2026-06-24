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
