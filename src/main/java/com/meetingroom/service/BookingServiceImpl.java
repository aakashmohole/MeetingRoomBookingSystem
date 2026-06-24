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
public class BookingServiceImpl implements BookingService{

    private final BookingDao bookingDao;

    public BookingServiceImpl(BookingDao bookingDao) {
        this.bookingDao = bookingDao;
    }


    @Override
    public void createBooking(Booking booking) throws Exception {
        log.info("Creating booking request for Room ID: {} on {}", booking.getRoom().getId(), booking.getBookingDate());
        if(booking.getStartTime().isAfter(booking.getEndTime()) ||  booking.getStartTime().equals(booking.getEndTime())){
            throw new Exception("Start time must be before end time.");
        }

        boolean conflict = bookingDao.hasOverlap(
                booking.getRoom().getId(),
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                null
        );

        if(conflict){
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
