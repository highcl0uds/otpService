package com.otpservice.service;

import com.otpservice.dao.UserDao;
import com.otpservice.model.User;
import com.otpservice.model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public List<User> getAllNonAdminUsers() {
        List<User> users = userDao.findAllNonAdmin();
        log.debug("Retrieved {} non-admin users", users.size());
        return users;
    }

    public void deleteUser(long id) {
        User user = userDao.findById(id).orElseThrow(() ->
                new IllegalArgumentException("User not found: " + id));
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot delete admin user");
        }
        userDao.deleteById(id);
        log.info("User deleted: id={}", id);
    }
}
