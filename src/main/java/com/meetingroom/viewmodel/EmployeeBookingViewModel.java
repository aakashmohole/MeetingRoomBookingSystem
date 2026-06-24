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
import com.meetingroom.util.ToastUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
@Slf4j
public class EmployeeBookingViewModel {
    @WireVariable
    private RoomService roomService;

    @WireVariable
    private BookingService bookingService;

    private User currentUser;
    private List<Room> rooms;
    private List<Booking> myBookings;

    // Filter & Booking State variables
    private Room selectedRoom;
    private Date selectDate = new Date(); // Bound to ZK datebox
    private String startTime = "09:00";   // Bound to start time select (HH:mm)
    private String endTime = "10:00";     // Bound to end time select (HH:mm)
    private String purpose;

    private String successMessage;
    private String errorMessage;

    @Init
    public void init() {
        currentUser = (User) Sessions.getCurrent().getAttribute("currentUser");
        if (currentUser == null) {
            Executions.sendRedirect("login.zul");
            return;
        }
        rooms = roomService.getAllRooms();
        myBookings = bookingService.getBookingsByUser(currentUser.getId());
    }

    public boolean isRoomBusy(Room room) {
        if (room == null || selectDate == null || startTime == null || endTime == null) {
            return false;
        }
        try {
            LocalDate date = selectDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);
            if (start.isAfter(end) || start.equals(end)) {
                return false;
            }
            List<Booking> roomBookings = bookingService.getBookingsByRoom(room.getId());
            return roomBookings.stream()
                    .anyMatch(b -> "CONFIRMED".equals(b.getStatus()) &&
                            b.getBookingDate().isEqual(date) &&
                            b.getStartTime().isBefore(end) &&
                            b.getEndTime().isAfter(start));
        } catch (Exception e) {
            log.error("Error checking room status", e);
            return false;
        }
    }

    @Command
    @NotifyChange({"rooms", "myBookings", "successMessage", "errorMessage", "purpose"})
    public void bookSelectedRoom(@BindingParam("room") Room room) {
        successMessage = null;
        errorMessage = null;

        if (selectDate == null) {
            errorMessage = "Please select a date.";
            ToastUtil.warning("Please select a date.");
            return;
        }
        if (purpose == null || purpose.trim().isEmpty()) {
            errorMessage = "Please specify the purpose of this meeting.";
            ToastUtil.warning("Please specify the purpose of this meeting.");
            return;
        }

        try {
            LocalDate date = selectDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);

            Booking booking = Booking.builder()
                    .user(currentUser)
                    .room(room)
                    .bookingDate(date)
                    .startTime(start)
                    .endTime(end)
                    .purpose(purpose.trim())
                    .build();

            bookingService.createBooking(booking);
            successMessage = "Successfully booked room: " + room.getName();
            ToastUtil.success("Successfully booked room: " + room.getName());
            purpose = ""; // Clear input
            myBookings = bookingService.getBookingsByUser(currentUser.getId());
        } catch (Exception e) {
            errorMessage = e.getMessage();
            ToastUtil.error("Failed to book room: " + e.getMessage());
        }
    }

    @Command
    @NotifyChange({"rooms", "myBookings", "successMessage", "errorMessage"})
    public void cancelBooking(@BindingParam("id") Long id) {
        bookingService.cancelBooking(id);
        myBookings = bookingService.getBookingsByUser(currentUser.getId());
        successMessage = "Booking cancelled successfully.";
        errorMessage = null;
        ToastUtil.success("Booking cancelled successfully.");
    }

    // Getters and Setters
    public List<Room> getRooms() {
        return rooms;
    }

    public List<Booking> getMyBookings() {
        return myBookings;
    }

    public Date getSelectDate() {
        return selectDate;
    }

    @NotifyChange("rooms")
    public void setSelectDate(Date selectDate) {
        this.selectDate = selectDate;
    }

    public String getStartTime() {
        return startTime;
    }

    @NotifyChange("rooms")
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    @NotifyChange("rooms")
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
