package com.meetingroom.viewmodel;

import com.meetingroom.model.Booking;
import com.meetingroom.model.User;
import com.meetingroom.service.BookingService;
import com.meetingroom.util.ToastUtil;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
@Slf4j
public class MainViewModel {

    @WireVariable
    private BookingService bookingService;

    private User currentUser;
    private List<Booking> upcomingBookings = new ArrayList<>();
    private List<Booking> unreadStatusUpdates = new ArrayList<>();

    @Init
    public void init() {
        currentUser = (User) Sessions.getCurrent().getAttribute("currentUser");
        if (currentUser == null) {
            Executions.sendRedirect("login.zul");
            return;
        }
        loadUpcomingBookings();
        loadUnreadStatusUpdates();
    }

    private void loadUpcomingBookings() {
        if (currentUser != null && "EMPLOYEE".equals(currentUser.getRole())) {
            List<Booking> allUserBookings = bookingService.getBookingsByUser(currentUser.getId());
            LocalDate today = LocalDate.now();
            LocalTime nowTime = LocalTime.now();
            upcomingBookings = allUserBookings.stream()
                    .filter(b -> "CONFIRMED".equals(b.getStatus()))
                    .filter(b -> b.getBookingDate().isAfter(today) ||
                            (b.getBookingDate().isEqual(today) && b.getEndTime().isAfter(nowTime)))
                    .collect(Collectors.toList());
        }
    }

    private void loadUnreadStatusUpdates() {
        if (currentUser != null && "EMPLOYEE".equals(currentUser.getRole())) {
            List<Booking> allUserBookings = bookingService.getBookingsByUser(currentUser.getId());
            unreadStatusUpdates = allUserBookings.stream()
                    .filter(b -> !b.isNotificationRead())
                    .collect(Collectors.toList());
        }
    }

    @Command
    @NotifyChange("upcomingBookings")
    public void cancelBookingFromDashboard(@org.zkoss.bind.annotation.BindingParam("id") Long id) {
        bookingService.cancelBooking(id);
        loadUpcomingBookings();
        ToastUtil.success("Booking cancelled successfully.");
    }

    @Command
    @NotifyChange("unreadStatusUpdates")
    public void dismissNotification(@org.zkoss.bind.annotation.BindingParam("booking") Booking booking) {
        if (booking == null) return;
        try {
            booking.setNotificationRead(true);
            bookingService.updateBooking(booking);
            loadUnreadStatusUpdates();
        } catch (Exception e) {
            log.error("Failed to dismiss notification", e);
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

    public List<Booking> getUpcomingBookings() {
        return upcomingBookings;
    }

    public List<Booking> getUnreadStatusUpdates() {
        return unreadStatusUpdates;
    }
}
