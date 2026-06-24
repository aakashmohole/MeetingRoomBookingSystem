package com.meetingroom.dao;

import com.meetingroom.model.User;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class HibernateUserDao implements UserDao {

    private final SessionFactory sessionFactory;

    public HibernateUserDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void save(User user) {
        sessionFactory.getCurrentSession().saveOrUpdate(user);
    }

    @Override
    public User findByUsername(String username) {
        return sessionFactory.getCurrentSession()
                .createQuery("from User where username = :username", User.class)
                .setParameter("username", username)
                .uniqueResult();
    }

    @Override
    public User findByEmail(String email) {
        return sessionFactory.getCurrentSession()
                .createQuery("from User where email = :email", User.class)
                .setParameter("email", email)
                .uniqueResult();
    }
}
