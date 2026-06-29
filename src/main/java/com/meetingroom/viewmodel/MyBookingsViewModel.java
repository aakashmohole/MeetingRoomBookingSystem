package com.meetingroom.viewmodel;

import com.meetingroom.model.Booking;
import com.meetingroom.model.User;
import com.meetingroom.service.BookingExportService;
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
public class MyBookingsViewModel {

    @WireVariable
    private BookingService bookingService;

    @WireVariable
    private BookingExportService bookingExportService;

    private boolean exportDialogOpen = false;
    private String exportScope = "ALL";
    private Date exportStartDate;
    private Date exportEndDate;
    private String exportFormat = "EXCEL";



    private User currentUser;
    private List<Booking> allBookings = new ArrayList<>();
    private List<Booking> filteredBookings = new ArrayList<>();
    private List<com.meetingroom.model.BookingInvitee> invitations = new ArrayList<>();

    // Stats
    private int totalBookings = 0;
    private int activeBookings = 0;
    private int cancelledBookings = 0;

    // Filter Fields
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
        loadData();
    }

    private void loadData() {
        allBookings = bookingService.getBookingsByUser(currentUser.getId());
        invitations = bookingService.getInvitationsForUser(currentUser.getId());
        calculateStats();
        applyFilters();
    }

    private void calculateStats() {
        totalBookings = allBookings.size();
        activeBookings = (int) allBookings.stream().filter(b -> "CONFIRMED".equals(b.getStatus())).count();
        cancelledBookings = (int) allBookings.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();
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
                    return roomMatch || purposeMatch;
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
    public void cancelBooking(@BindingParam("id") Long id) {
        try {
            bookingService.cancelBooking(id);
            ToastUtil.success("Booking cancelled successfully.");
            loadData();
        } catch (Exception e) {
            ToastUtil.error("Failed to cancel booking: " + e.getMessage());
        }
    }

    @Command
    @NotifyChange({"filteredBookings", "totalBookings", "activeBookings", "cancelledBookings"})
    public void cancelSeries(@BindingParam("recurrenceId") String recurrenceId) {
        try {
            bookingService.cancelRecurringSeries(recurrenceId);
            ToastUtil.success("Entire recurring series cancelled successfully.");
            loadData();
        } catch (Exception e) {
            ToastUtil.error("Failed to cancel series: " + e.getMessage());
        }
    }

    @Command
    @NotifyChange({"invitations", "filteredBookings", "totalBookings", "activeBookings", "cancelledBookings"})
    public void respondToInvite(@BindingParam("bookingId") Long bookingId, @BindingParam("status") String status) {
        try {
            bookingService.respondToInvitation(bookingId, currentUser.getId(), status);
            ToastUtil.success("RSVP status updated to " + status + ".");
            loadData();
        } catch (Exception e) {
            ToastUtil.error("Failed to respond to invitation: " + e.getMessage());
        }
    }

    @Command
    @NotifyChange("exportDialogOpen")
    public void openExportDialog() {
        this.exportScope = "ALL";
        this.exportStartDate = null;
        this.exportEndDate = null;
        this.exportFormat = "EXCEL";
        this.exportDialogOpen = true;
    }

    @Command
    @NotifyChange("exportDialogOpen")
    public void closeExportDialog() {
        this.exportDialogOpen = false;
    }

    @Command
    @NotifyChange({"exportDialogOpen", "exportScope"})
    public void changeExportScope() {
        // Triggers UI updates
    }


    @Command
    @NotifyChange("exportDialogOpen")
    public void performExport(){
        List<Booking> bookingsToExport;
        String fileName = "";
        switch (exportScope){
            case "FILTERED":
                bookingsToExport = new ArrayList<>(filteredBookings);
                break;
            case "RANGE":
                if (exportStartDate == null || exportEndDate == null) {
                    ToastUtil.error("Please select both Start and End Dates.");
                    return;
                }
                if (exportStartDate.after(exportEndDate)) {
                    ToastUtil.error("Start Date cannot be after End Date.");
                    return;
                }

                LocalDate start = exportStartDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate end = exportEndDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                // Filter allBookings in-memory for security and efficiency
                bookingsToExport = allBookings.stream()
                        .filter(b -> !b.getBookingDate().isBefore(start) && !b.getBookingDate().isAfter(end))
                        .collect(Collectors.toList());
                break;
            case "ALL":
            default:
                bookingsToExport = new ArrayList<>(allBookings);
                break;
        }

        if (bookingsToExport.isEmpty()) {
            ToastUtil.error("No bookings found in the selected range to export.");
            return;
        }

        try {
            byte[] fileData = null;
            fileName = "my_bookings_" + System.currentTimeMillis();
            String contentType = "";

            switch (exportFormat) {
                case "EXCEL":
                    fileData = bookingExportService.exportToExcel(bookingsToExport);
                    fileName += ".xlsx";
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    break;
                case "PDF":
                    fileData = bookingExportService.exportToPdf(bookingsToExport);
                    fileName += ".pdf";
                    contentType = "application/pdf";
                    break;
                case "XML":
                    fileData = bookingExportService.exportToXml(bookingsToExport);
                    fileName += ".xml";
                    contentType = "text/xml";
                    break;
                default:
                    ToastUtil.error("Invalid export format.");
                    return;
            }

            if (fileData != null) {
                org.zkoss.zul.Filedownload.save(fileData, contentType, fileName);
                this.exportDialogOpen = false;
                ToastUtil.success("Export successful! Download started.");
            }
        } catch (Exception e) {
            log.error("Error occurred while exporting user bookings", e);
            ToastUtil.error("An error occurred during export: " + e.getMessage());
        }

    }


    public List<com.meetingroom.model.BookingInvitee> getInvitations() {
        return invitations;
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

    public void setExportDialogOpen(boolean exportDialogOpen) {
        this.exportDialogOpen = exportDialogOpen;
    }

    public boolean isExportDialogOpen() {
        return exportDialogOpen;
    }

    public String getExportScope() {
        return exportScope;
    }

    public Date getExportStartDate() {
        return exportStartDate;
    }

    public Date getExportEndDate() {
        return exportEndDate;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportScope(String exportScope) {
        this.exportScope = exportScope;
    }

    public void setExportStartDate(Date exportStartDate) {
        this.exportStartDate = exportStartDate;
    }

    public void setExportEndDate(Date exportEndDate) {
        this.exportEndDate = exportEndDate;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public void setFilterDate(Date filterDate) { this.filterDate = filterDate; }
}
