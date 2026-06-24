package com.meetingroom.viewmodel;

import com.meetingroom.model.User;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;

public class MainViewModel {

    private User currentUser;

    @Init
    public void init() {
        currentUser = (User) Sessions.getCurrent().getAttribute("currentUser");
        if (currentUser == null) {
            Executions.sendRedirect("login.zul");
        }
    }

    @Command
    public void logout() {
        Sessions.getCurrent().removeAttribute("currentUser");
        Sessions.getCurrent().invalidate();
        Executions.sendRedirect("login.zul");
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
