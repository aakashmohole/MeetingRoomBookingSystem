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
public class BookingDaoImpl implements BookingDao {
    private final SessionFactory sessionFactory;

    public BookingDaoImpl(SessionFactory sessionFactory){
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
