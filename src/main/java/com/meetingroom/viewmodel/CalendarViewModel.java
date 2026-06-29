package com.meetingroom.viewmodel;

import com.meetingroom.model.Booking;
import com.meetingroom.model.Room;
import com.meetingroom.model.User;
import com.meetingroom.service.BookingService;
import com.meetingroom.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import java.util.ArrayList;
import java.util.List;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
@Slf4j
public class CalendarViewModel {

    @WireVariable
    private RoomService roomService;

    @WireVariable
    private BookingService bookingService;

    private User currentUser;
    private List<Room> rooms = new ArrayList<>();
    private List<Booking> allBookings = new ArrayList<>();

    // State properties
    private Long selectedRoomId = -1L; // Defaults to -1 (All Rooms)
    private String eventsJson = "[]";
    private Booking selectedBooking;
    private boolean detailsModalOpen = false;

    @Init
    public void init() {
        currentUser = (User) Sessions.getCurrent().getAttribute("currentUser");
        if (currentUser == null) {
            Executions.sendRedirect("login.zul");
            return;
        }
        try {
            rooms = roomService.getAllRooms();
            allBookings = bookingService.getAllBookings();
            generateEventsJson();
        } catch (Exception e) {
            log.error("Failed to initialize CalendarViewModel", e);
        }
    }

    private void generateEventsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Booking b : allBookings) {
            // Apply room filter if a specific room is selected
            if (selectedRoomId != null && selectedRoomId > 0 && !b.getRoom().getId().equals(selectedRoomId)) {
                continue;
            }
            // Only show active (confirmed) bookings on the visual calendar
            if (!"CONFIRMED".equalsIgnoreCase(b.getStatus())) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;

            // Generate distinct premium background colors based on Room ID for high contrast
            String color;
            switch ((int) (b.getRoom().getId() % 6)) {
                case 0: color = "#2563eb"; break; // Blue
                case 1: color = "#7c3aed"; break; // Purple
                case 2: color = "#059669"; break; // Emerald
                case 3: color = "#d97706"; break; // Amber
                case 4: color = "#db2777"; break; // Pink
                default: color = "#0d9488"; break; // Teal
            }

            String title = escapeJson(b.getRoom().getName() + " - " + b.getPurpose());
            String startIso = b.getBookingDate().toString() + "T" + b.getStartTime().toString();
            String endIso = b.getBookingDate().toString() + "T" + b.getEndTime().toString();

            sb.append("{");
            sb.append("\"id\":").append(b.getId()).append(",");
            sb.append("\"title\":\"").append(title).append("\",");
            sb.append("\"start\":\"").append(startIso).append("\",");
            sb.append("\"end\":\"").append(endIso).append("\",");
            sb.append("\"color\":\"").append(color).append("\"");
            sb.append("}");
        }
        sb.append("]");
        this.eventsJson = sb.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\r", "")
                   .replace("\n", " ");
    }

    @Command
    @NotifyChange("eventsJson")
    public void applyFilter() {
        generateEventsJson();
        org.zkoss.zk.ui.util.Clients.evalJavaScript("updateCalendarEvents(" + eventsJson + ");");
    }

    @Command
    @NotifyChange({"selectedBooking", "detailsModalOpen"})
    public void openDetails(@BindingParam("bookingId") Long bookingId) {
        if (bookingId == null) return;
        // Search in local list
        for (Booking b : allBookings) {
            if (b.getId().equals(bookingId)) {
                this.selectedBooking = b;
                this.detailsModalOpen = true;
                break;
            }
        }
    }

    @Command
    @NotifyChange({"selectedBooking", "detailsModalOpen"})
    public void closeDetails() {
        this.selectedBooking = null;
        this.detailsModalOpen = false;
    }

    // Getters and Setters
    public List<Room> getRooms() {
        return rooms;
    }

    public Long getSelectedRoomId() {
        return selectedRoomId;
    }

    public void setSelectedRoomId(Long selectedRoomId) {
        this.selectedRoomId = selectedRoomId;
    }

    public String getEventsJson() {
        return eventsJson;
    }

    public Booking getSelectedBooking() {
        return selectedBooking;
    }

    public boolean isDetailsModalOpen() {
        return detailsModalOpen;
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
