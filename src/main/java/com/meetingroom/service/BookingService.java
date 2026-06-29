package com.meetingroom.service;

import com.meetingroom.model.Booking;
import com.meetingroom.model.BookingInvitee;

import java.util.List;

public interface BookingService {
    void createBooking(Booking booking, java.time.LocalDate recurrenceEndDate, List<Long> inviteeUserIds) throws Exception;
    void updateBooking(Booking booking) throws Exception;
    void cancelBooking(Long bookingId);
    void cancelRecurringSeries(String recurrenceId);
    boolean hasOverlap(Long roomId, java.time.LocalDate date, java.time.LocalTime start, java.time.LocalTime end, Long excludeBookingId);
    List<Booking> getBookingsByUser(Long userId);
    List<Booking> getBookingsByRoom(Long roomId);
    List<Booking> getAllBookings();

    List<BookingInvitee> getInvitationsForUser(Long userId);
    void respondToInvitation(Long bookingId, Long userId, String status) throws Exception;
}
