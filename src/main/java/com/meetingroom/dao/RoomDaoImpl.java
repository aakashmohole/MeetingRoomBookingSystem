package com.meetingroom.dao;

import com.meetingroom.model.Room;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
@Transactional
public class RoomDaoImpl implements RoomDao{

    private final SessionFactory sessionFactory;
    public RoomDaoImpl(SessionFactory sessionFactory){
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
        if(room != null){
            sessionFactory.getCurrentSession().delete(room);
        }
    }

    @Override
    public Room findById(Long id) {
        return sessionFactory.getCurrentSession().get(Room.class, id);
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
