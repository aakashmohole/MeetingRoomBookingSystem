package com.meetingroom.dao;

import com.meetingroom.model.Booking;
import com.meetingroom.model.BookingInvitee;
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

    void cancelRecurringSeries(String recurrenceId);

    void saveInvitee(BookingInvitee invitee);
    void updateInvitee(BookingInvitee invitee);
    List<BookingInvitee> findInviteesByUser(Long userId);
    BookingInvitee findInviteeByBookingAndUser(Long bookingId, Long userId);
}
