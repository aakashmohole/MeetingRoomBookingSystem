package com.meetingroom.viewmodel;

import com.meetingroom.model.User;
import com.meetingroom.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import com.meetingroom.util.ToastUtil;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
@Slf4j
public class RegisterViewModel {

    @WireVariable
    private UserService userService;

    private String username;
    private String email;
    private String password;
    private String confirmPassword;
    private String role = "EMPLOYEE"; // Defaults to EMPLOYEE
    private String errorMessage;
    private String successMessage;

    @Init
    public void init() {
        log.info("Initializing RegisterViewModel. Checking active sessions.");
        if (Sessions.getCurrent().getAttribute("currentUser") != null) {
            log.info("Active session detected. Redirecting from registration to dashboard.");
            Executions.sendRedirect("index.zul");
        }
    }

    @Command
    @NotifyChange({"username", "email", "password", "confirmPassword", "role", "errorMessage", "successMessage"})
    public void register() {
        log.info("Registration attempt submitted for username: {}, email: {}, role: {}", username, email, role);
        errorMessage = null;
        successMessage = null;

        if (username == null || username.trim().isEmpty()) {
            errorMessage = "Username is required.";
            ToastUtil.warning("Username is required.");
            log.warn("Registration validation failed: Username is empty.");
            return;
        }
        if (email == null || email.trim().isEmpty()) {
            errorMessage = "Email is required.";
            ToastUtil.warning("Email is required.");
            log.warn("Registration validation failed: Email is empty.");
            return;
        }
        if (password == null || password.isEmpty()) {
            errorMessage = "Password is required.";
            ToastUtil.warning("Password is required.");
            log.warn("Registration validation failed: Password is empty.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            errorMessage = "Passwords do not match.";
            ToastUtil.warning("Passwords do not match.");
            log.warn("Registration validation failed: Passwords do not match.");
            return;
        }

        User newUser = User.builder()
                .username(username.trim())
                .email(email.trim())
                .password(password)
                .role(role)
                .build();

        try {
            userService.registerUser(newUser);
            log.info("Registration successful for user: {}. Redirecting to login.", username);
            successMessage = "Registration successful!";
            ToastUtil.success("Registration successful! Redirecting to sign in...");
            Executions.sendRedirect("login.zul");
        } catch (Exception e) {
            log.error("Registration failed for user: {}", username, e);
            errorMessage = e.getMessage();
            ToastUtil.error("Registration failed: " + e.getMessage());
        }
    }

    @Command
    public void navigateToLogin() {
        log.info("Redirecting to login page.");
        Executions.sendRedirect("login.zul");
    }

    @Command
    public void selectRole(@BindingParam("val") String val) {
        log.info("Role selected in ViewModel: {}", val);
        this.role = val;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }
}
