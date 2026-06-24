package com.meetingroom.service;

import com.meetingroom.model.User;

public interface UserService {
    void registerUser(User user) throws Exception;
    User login(String username, String password);
    boolean isUsernameExists(String username);
    boolean isEmailExists(String email);
}
