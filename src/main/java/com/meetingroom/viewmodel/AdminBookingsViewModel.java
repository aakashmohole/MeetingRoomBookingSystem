package com.meetingroom.viewmodel;

import com.meetingroom.model.Booking;
import com.meetingroom.model.User;
import com.meetingroom.service.BookingExportService;
import com.meetingroom.service.BookingService;
import com.meetingroom.util.ToastUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.atp.Switch;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import java.awt.print.Book;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
@Slf4j
public class AdminBookingsViewModel {

    @WireVariable
    private BookingService bookingService;

    @WireVariable
    private BookingExportService bookingExportService;


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

    // Remarks dialog state
    private boolean remarksDialogOpen = false;
    private Booking selectedBooking;
    private String remarksText = "";
    private String pendingStatus = "";
    private String dialogTitle = "";

    // dailog controls
    private boolean exportDialogOpen = false;
    private String exportScope = "ALL";
    private Date exportStartDate;
    private Date exportEndDate;
    private String exportFormat = "EXCEL";




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
    @NotifyChange({"remarksDialogOpen", "selectedBooking", "remarksText", "pendingStatus", "dialogTitle"})
    public void openRemarksDialog(@BindingParam("booking") Booking booking, @BindingParam("status") String status) {
        if (booking == null || status == null) return;
        this.selectedBooking = booking;
        this.pendingStatus = status;
        this.remarksText = "";
        this.dialogTitle = "CONFIRMED".equalsIgnoreCase(status) ? "Approve Booking" : "Cancel Booking";
        this.remarksDialogOpen = true;
    }

    @Command
    @NotifyChange({"remarksDialogOpen", "selectedBooking", "remarksText", "pendingStatus"})
    public void closeRemarksDialog() {
        this.remarksDialogOpen = false;
        this.selectedBooking = null;
        this.remarksText = "";
        this.pendingStatus = "";
    }

    @Command
    @NotifyChange({"filteredBookings", "totalBookings", "activeBookings", "cancelledBookings", "remarksDialogOpen", "selectedBooking", "remarksText"})
    public void submitStatusUpdate() {
        if (selectedBooking == null || pendingStatus == null || pendingStatus.isEmpty()) {
            ToastUtil.error("No active booking selected.");
            return;
        }
        try {
            selectedBooking.setStatus(pendingStatus);
            selectedBooking.setAdminRemarks(remarksText != null ? remarksText.trim() : "");
            selectedBooking.setNotificationRead(false); // Mark as unread so employee gets notified
            bookingService.updateBooking(selectedBooking);
            
            String action = "CONFIRMED".equalsIgnoreCase(pendingStatus) ? "approved" : "cancelled";
            ToastUtil.success("Booking for " + selectedBooking.getRoom().getName() + " has been " + action + ".");
            
            closeRemarksDialog();
            loadData();
        } catch (Exception e) {
            log.error("Failed to update booking status", e);
            ToastUtil.error("Failed to update booking status: " + e.getMessage());
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
            log.error("Failed to cancel recurring series", e);
            ToastUtil.error("Failed to cancel series: " + e.getMessage());
        }
    }


    @Command
    @NotifyChange("exportDialogOpen")
    public void openExportDialog(){
        this.exportScope = "ALL";
        this.exportStartDate = null;
        this.exportEndDate =  null;
        this.exportFormat = "EXCEL";
        this.exportDialogOpen = true;
    }

    @Command
    @NotifyChange("exportDialogOpen")
    public void closeExportDialog(){
        this.exportDialogOpen = false;
    }

    @Command
    @NotifyChange({"exportDialogOpen", "exportScope"})
    public void changeExportScope() {
        // Triggers visibility changes for range boxes
    }

    @Command
    @NotifyChange("exportDialogOpen")
    public void performExport(){
        List<Booking> bookingsToExport;
        String fileName = "";

        if ("FILTERED".equals(exportScope)) {
            bookingsToExport = new ArrayList<>(filteredBookings);
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            fileName = "booking_export_filtered_" + today.format(formatter);
        } else if("RANGE".equals(exportScope)){
            if(exportStartDate == null || exportEndDate == null){
                ToastUtil.error("Please select both Start and End Dates for exporting.");
                return;
            }
            if (exportStartDate.after(exportEndDate)) {
                ToastUtil.error("Start Date cannot be after End Date.");
                return;
            }

            LocalDate start = exportStartDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate end = exportEndDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            bookingsToExport = bookingService.getBookingsByDateRange(start, end);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            fileName = "booking_export_" + start.format(formatter) + "_" + end.format(formatter);
        }else{
            bookingsToExport = bookingService.getAllBookings();
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            fileName = "booking_export_" + today.format(formatter);
        }
        if (bookingsToExport.isEmpty()) {
            ToastUtil.error("No bookings found in the selected range to export.");
            return;
        }

        try{
            byte[] fileData = null;
//            String fileName = "booking_export_" + System.currentTimeMillis();
            String contentType = "";

            switch (exportFormat){
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
            log.error("Error occurred while exporting bookings", e);
            ToastUtil.error("An error occurred during export: " + e.getMessage());
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

    public boolean isExportDialogOpen() {
        return exportDialogOpen;
    }

    public String getExportScope() {
        return exportScope;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public void setExportEndDate(Date exportEndDate) {
        this.exportEndDate = exportEndDate;
    }

    public void setExportStartDate(Date exportStartDate) {
        this.exportStartDate = exportStartDate;
    }

    public void setExportScope(String exportScope) {
        this.exportScope = exportScope;
    }

    public void setExportDialogOpen(boolean exportDialogOpen) {
        this.exportDialogOpen = exportDialogOpen;
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

    // Dialog Getters and Setters
    public boolean isRemarksDialogOpen() { return remarksDialogOpen; }
    public Booking getSelectedBooking() { return selectedBooking; }
    public String getRemarksText() { return remarksText; }
    public void setRemarksText(String remarksText) { this.remarksText = remarksText; }
    public String getPendingStatus() { return pendingStatus; }
    public String getDialogTitle() { return dialogTitle; }
}
