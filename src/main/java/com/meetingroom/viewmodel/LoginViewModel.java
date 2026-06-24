package com.meetingroom.viewmodel;

import com.meetingroom.model.User;
import com.meetingroom.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
@Slf4j
public class LoginViewModel {

    @WireVariable
    private UserService userService;

    private String username;
    private String password;
    private String errorMessage;

    @Init
    public void init() {
        log.info("Initializing LoginViewModel. Checking active sessions.");
        if (Sessions.getCurrent().getAttribute("currentUser") != null) {
            log.info("Active user session detected. Redirecting to dashboard.");
            Executions.sendRedirect("index.zul");
        }
    }

    @Command
    @NotifyChange("errorMessage")
    public void login() {
        log.info("Login attempt for username: {}", username);
        if (username == null || username.trim().isEmpty()) {
            errorMessage = "Username is required.";
            log.warn("Login validation failed: Username is empty.");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            errorMessage = "Password is required.";
            log.warn("Login validation failed: Password is empty.");
            return;
        }

        User user = userService.login(username.trim(), password);
        if (user != null) {
            log.info("Login successful for user: {}. Storing in session and redirecting.", user.getUsername());
            Sessions.getCurrent().setAttribute("currentUser", user);
            errorMessage = null;
            Executions.sendRedirect("index.zul");
        } else {
            log.warn("Login failed for user: {}. Invalid username or password.", username);
            errorMessage = "Invalid username or password.";
        }
    }

    @Command
    public void navigateToRegister() {
        log.info("Redirecting to registration page.");
        Executions.sendRedirect("register.zul");
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
