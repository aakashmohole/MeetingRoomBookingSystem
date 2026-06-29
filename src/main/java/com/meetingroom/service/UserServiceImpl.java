package com.meetingroom.service;

import com.meetingroom.dao.UserDao;
import com.meetingroom.model.User;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userService")
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void registerUser(User user) throws Exception {
        log.info("Attempting to register user: {}", user.getUsername());
        if (isUsernameExists(user.getUsername())) {
            log.warn("Username is already taken: {}", user.getUsername());
            throw new Exception("Username is already taken.");
        }
        if (isEmailExists(user.getEmail())) {
            log.warn("Email is already registered: {}", user.getUsername());
            throw new Exception("Email is already registered.");
        }
        try {
            if (user.getRole() == null || user.getRole().trim().isEmpty()) {
                user.setRole("USER");
            }
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));
            user.setPassword(hashedPassword);
            userDao.save(user);
            log.debug("Successfully saved user to database.");
        } catch (Exception e) {
            log.error("Failed to register user: {}", user.getUsername(), e);
            throw e;
        }
    }

    @Override
    public User login(String username, String password) {
        log.info("Attempting to login user: {}", username);

        try{
            User user = userDao.findByUsername(username);
            if (user != null && BCrypt.checkpw(password, user.getPassword())) {
                log.debug("Logged in Successfully!, {}", username);
                return user;
            }
        }catch (Exception e){
            log.error("check username and password!");
        }
        return null;
    }

    @Override
    public boolean isUsernameExists(String username) {
        boolean exists = userDao.findByUsername(username) != null;
        if (exists) {
            log.warn("Username already exists in DB: {}", username);
        }
        return exists;
    }

    @Override
    public boolean isEmailExists(String email) {
        boolean exists = userDao.findByEmail(email) != null;
        if (exists) {
            log.warn("Email already exists in DB: {}", email);
        }
        return exists;
    }

    @Override
    public java.util.List<User> getAllUsersExcept(Long currentUserId) {
        log.info("Fetching colleagues except User ID: {}", currentUserId);
        return userDao.findAll().stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .collect(java.util.stream.Collectors.toList());
    }
}
