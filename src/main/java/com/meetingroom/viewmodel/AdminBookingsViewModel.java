package com.meetingroom.viewmodel;

import com.meetingroom.model.Booking;
import com.meetingroom.model.User;
import com.meetingroom.service.BookingService;
import com.meetingroom.util.ToastUtil;
import lombok.extern.slf4j.Slf4j;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
@Slf4j
public class AdminBookingsViewModel {

    @WireVariable
    private BookingService bookingService;

    private User currentUser;
    private List<Booking> allBookings = new ArrayList<>();
    private List<Booking> filteredBookings = new ArrayList<>();

    // Stats variables
    private int totalBookings = 0;
    private int activeBookings = 0;
    private int cancelledBookings = 0;

    // Filter variables
    private String searchText = "";
    private String selectedStatus = "ALL";
    private Date filterDate;

    @Init
    public void init() {
        currentUser = (User) Sessions.getCurrent().getAttribute("currentUser");
        if (currentUser == null) {
            Executions.sendRedirect("login.zul");
            return;
        }

        // Safeguard: Check admin role
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            ToastUtil.error("Access denied. Admin privileges required.");
            Executions.sendRedirect("index.zul");
            return;
        }

        loadData();
    }

    private void loadData() {
        try {
            allBookings = bookingService.getAllBookings();
            calculateStats();
            applyFilters();
        } catch (Exception e) {
            log.error("Failed to load all bookings for admin", e);
            ToastUtil.error("Failed to load booking data.");
        }
    }

    private void calculateStats() {
        totalBookings = allBookings.size();
        activeBookings = (int) allBookings.stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                .count();
        cancelledBookings = (int) allBookings.stream()
                .filter(b -> "CANCELLED".equalsIgnoreCase(b.getStatus()))
                .count();
    }

    @Command
    @NotifyChange("filteredBookings")
    public void applyFilters() {
        LocalDate localFilterDate = null;
        if (filterDate != null) {
            localFilterDate = filterDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        final LocalDate finalLocalFilterDate = localFilterDate;
        filteredBookings = allBookings.stream()
                .filter(b -> {
                    if ("ALL".equalsIgnoreCase(selectedStatus)) return true;
                    return selectedStatus.equalsIgnoreCase(b.getStatus());
                })
                .filter(b -> {
                    if (searchText == null || searchText.trim().isEmpty()) return true;
                    String query = searchText.toLowerCase().trim();
                    boolean roomMatch = b.getRoom().getName().toLowerCase().contains(query);
                    boolean purposeMatch = b.getPurpose().toLowerCase().contains(query);
                    boolean userMatch = b.getUser().getUsername().toLowerCase().contains(query) 
                            || b.getUser().getEmail().toLowerCase().contains(query);
                    return roomMatch || purposeMatch || userMatch;
                })
                .filter(b -> {
                    if (finalLocalFilterDate == null) return true;
                    return b.getBookingDate().isEqual(finalLocalFilterDate);
                })
                .collect(Collectors.toList());
    }

    @Command
    @NotifyChange({"searchText", "selectedStatus", "filterDate", "filteredBookings"})
    public void resetFilters() {
        searchText = "";
        selectedStatus = "ALL";
        filterDate = null;
        applyFilters();
    }

    @Command
    @NotifyChange({"filteredBookings", "totalBookings", "activeBookings", "cancelledBookings"})
    public void approveBooking(@BindingParam("booking") Booking booking) {
        if (booking == null) return;
        try {
            booking.setStatus("CONFIRMED");
            bookingService.updateBooking(booking);
            ToastUtil.success("Booking for " + booking.getRoom().getName() + " has been approved.");
            loadData();
        } catch (Exception e) {
            log.error("Failed to approve booking", e);
            ToastUtil.error("Failed to approve booking: " + e.getMessage());
        }
    }

    @Command
    @NotifyChange({"filteredBookings", "totalBookings", "activeBookings", "cancelledBookings"})
    public void cancelBooking(@BindingParam("booking") Booking booking) {
        if (booking == null) return;
        try {
            booking.setStatus("CANCELLED");
            bookingService.updateBooking(booking);
            ToastUtil.success("Booking for " + booking.getRoom().getName() + " has been cancelled.");
            loadData();
        } catch (Exception e) {
            log.error("Failed to cancel booking", e);
            ToastUtil.error("Failed to cancel booking: " + e.getMessage());
        }
    }

    // Getters and Setters
    public List<Booking> getFilteredBookings() { return filteredBookings; }
    public int getTotalBookings() { return totalBookings; }
    public int getActiveBookings() { return activeBookings; }
    public int getCancelledBookings() { return cancelledBookings; }
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
    public String getSelectedStatus() { return selectedStatus; }
    public void setSelectedStatus(String selectedStatus) { this.selectedStatus = selectedStatus; }
    public Date getFilterDate() { return filterDate; }
    public void setFilterDate(Date filterDate) { this.filterDate = filterDate; }
}
