package com.meetingroom.dao;

import com.meetingroom.model.User;

public interface UserDao {
    void save(User user);
    User findById(Long id);
    User findByUsername(String username);
    User findByEmail(String email);
    java.util.List<User> findAll();
}
