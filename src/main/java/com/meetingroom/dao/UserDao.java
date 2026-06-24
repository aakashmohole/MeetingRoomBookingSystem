package com.meetingroom.dao;

import com.meetingroom.model.User;

public interface UserDao {
    void save(User user);
    User findByUsername(String username);
    User findByEmail(String email);
}
