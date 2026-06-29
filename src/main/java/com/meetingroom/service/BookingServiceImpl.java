package com.meetingroom.service;

import com.meetingroom.dao.BookingDao;
import com.meetingroom.model.Booking;
import com.meetingroom.model.BookingInvitee;
import com.meetingroom.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service("bookingService")
@Transactional
@Slf4j
public class BookingServiceImpl implements BookingService{

    private final BookingDao bookingDao;
    private final com.meetingroom.dao.UserDao userDao;

    public BookingServiceImpl(BookingDao bookingDao, com.meetingroom.dao.UserDao userDao) {
        this.bookingDao = bookingDao;
        this.userDao = userDao;
    }


    @Override
    public void createBooking(Booking booking, java.time.LocalDate recurrenceEndDate, List<Long> inviteeUserIds) throws Exception {
        log.info("Creating booking request for Room ID: {} on {}", booking.getRoom().getId(), booking.getBookingDate());
        if (booking.getStartTime().isAfter(booking.getEndTime()) || booking.getStartTime().equals(booking.getEndTime())) {
            throw new Exception("Start time must be before end time.");
        }

        boolean isRecurring = booking.getRecurrenceType() != null && 
                             ("DAILY".equalsIgnoreCase(booking.getRecurrenceType()) || "WEEKLY".equalsIgnoreCase(booking.getRecurrenceType())) &&
                             recurrenceEndDate != null;

        java.util.List<Booking> savedBookings = new java.util.ArrayList<>();

        if (isRecurring) {
            if (recurrenceEndDate.isBefore(booking.getBookingDate())) {
                throw new Exception("Recurrence end date cannot be before booking date.");
            }

            // Generate dates
            java.util.List<java.time.LocalDate> dates = new java.util.ArrayList<>();
            java.time.LocalDate current = booking.getBookingDate();
            while (!current.isAfter(recurrenceEndDate)) {
                dates.add(current);
                if ("DAILY".equalsIgnoreCase(booking.getRecurrenceType())) {
                    current = current.plusDays(1);
                } else if ("WEEKLY".equalsIgnoreCase(booking.getRecurrenceType())) {
                    current = current.plusWeeks(1);
                } else {
                    break;
                }
            }

            // Check conflicts for all dates
            java.util.List<String> conflictingDates = new java.util.ArrayList<>();
            for (java.time.LocalDate d : dates) {
                boolean conflict = bookingDao.hasOverlap(
                        booking.getRoom().getId(),
                        d,
                        booking.getStartTime(),
                        booking.getEndTime(),
                        null
                );
                if (conflict) {
                    conflictingDates.add(d.toString());
                }
            }

            if (!conflictingDates.isEmpty()) {
                log.warn("Booking conflicts detected for recurring booking on dates: {}", conflictingDates);
                throw new Exception("This room is already booked on the following dates: " + String.join(", ", conflictingDates));
            }

            // Save all occurrences
            String recurrenceId = java.util.UUID.randomUUID().toString();
            for (java.time.LocalDate d : dates) {
                Booking occ = Booking.builder()
                        .user(booking.getUser())
                        .room(booking.getRoom())
                        .bookingDate(d)
                        .startTime(booking.getStartTime())
                        .endTime(booking.getEndTime())
                        .purpose(booking.getPurpose())
                        .status("CONFIRMED")
                        .recurrenceId(recurrenceId)
                        .recurrenceType(booking.getRecurrenceType().toUpperCase())
                        .notificationRead(true)
                        .build();
                bookingDao.save(occ);
                savedBookings.add(occ);
            }
        } else {
            // Single booking
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
            savedBookings.add(booking);
            log.debug("Successfully saved booking ID: {}", booking.getId());
        }

        // Add invitees to each saved booking
        if (inviteeUserIds != null && !inviteeUserIds.isEmpty()) {
            for (Booking saved : savedBookings) {
                for (Long userId : inviteeUserIds) {
                    User inviteeUser = userDao.findById(userId);
                    if (inviteeUser != null) {
                        com.meetingroom.model.BookingInvitee invitee = com.meetingroom.model.BookingInvitee.builder()
                                .booking(saved)
                                .user(inviteeUser)
                                .status("PENDING")
                                .build();
                        bookingDao.saveInvitee(invitee);
                    }
                }
            }
        }
    }

    @Override
    public void updateBooking(Booking booking) throws Exception {
        log.info("Updating booking request for Booking ID: {}", booking.getId());

        if (booking.getStartTime().isAfter(booking.getEndTime()) || booking.getStartTime().equals(booking.getEndTime())) {
            throw new Exception("Start time must be before end time.");
        }

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
    public void cancelRecurringSeries(String recurrenceId) {
        log.info("Cancelling recurring series with Recurrence ID: {}", recurrenceId);
        if (recurrenceId != null && !recurrenceId.isEmpty()) {
            bookingDao.cancelRecurringSeries(recurrenceId);
            log.debug("Recurring series cancelled successfully.");
        }
    }

    @Override
    public boolean hasOverlap(Long roomId, java.time.LocalDate date, java.time.LocalTime start, java.time.LocalTime end, Long excludeBookingId) {
        return bookingDao.hasOverlap(roomId, date, start, end, excludeBookingId);
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

    @Override
    public List<com.meetingroom.model.BookingInvitee> getInvitationsForUser(Long userId) {
        log.info("Fetching invitations for User ID: {}", userId);
        return bookingDao.findInviteesByUser(userId);
    }

    @Override
    public void respondToInvitation(Long bookingId, Long userId, String status) throws Exception {
        log.info("User ID: {} responding with {} to Booking ID: {}", userId, status, bookingId);
        com.meetingroom.model.BookingInvitee invitee = bookingDao.findInviteeByBookingAndUser(bookingId, userId);
        if (invitee == null) {
            throw new Exception("Invitation not found.");
        }
        invitee.setStatus(status.toUpperCase());
        bookingDao.updateInvitee(invitee);
        log.debug("Invitation response saved successfully.");
    }
}
