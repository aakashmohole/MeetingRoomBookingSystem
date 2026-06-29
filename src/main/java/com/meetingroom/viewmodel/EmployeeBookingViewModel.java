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

    @WireVariable
    private com.meetingroom.service.UserService userService;

    private User currentUser;
    private List<Room> rooms;
    private List<Booking> myBookings;
    private List<User> colleagues;
    private java.util.Set<User> selectedColleagues = new java.util.HashSet<>();

    // Filter & Booking State variables
    private Room selectedRoom;
    private Date selectDate = new Date(); // Bound to ZK datebox
    private String startTime = "09:00";   // Bound to start time select (HH:mm)
    private String endTime = "10:00";     // Bound to end time select (HH:mm)
    private String purpose;

    private boolean recurring = false;
    private String recurrenceType = "DAILY";
    private Date recurrenceEndDate;

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
        colleagues = userService.getAllUsersExcept(currentUser.getId());
    }

    public boolean isRoomBusy(Room room) {
        if (room == null || selectDate == null || startTime == null || endTime == null) {
            return false;
        }
        try {
            LocalDate startLocalDate = selectDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);
            if (start.isAfter(end) || start.equals(end)) {
                return false;
            }

            if (recurring && recurrenceEndDate != null) {
                LocalDate endLocalDate = recurrenceEndDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if (endLocalDate.isBefore(startLocalDate)) {
                    return false;
                }

                LocalDate current = startLocalDate;
                while (!current.isAfter(endLocalDate)) {
                    boolean conflict = bookingService.hasOverlap(room.getId(), current, start, end, null);
                    if (conflict) {
                        return true;
                    }
                    if ("DAILY".equalsIgnoreCase(recurrenceType)) {
                        current = current.plusDays(1);
                    } else if ("WEEKLY".equalsIgnoreCase(recurrenceType)) {
                        current = current.plusWeeks(1);
                    } else {
                        break;
                    }
                }
                return false;
            } else {
                return bookingService.hasOverlap(room.getId(), startLocalDate, start, end, null);
            }
        } catch (Exception e) {
            log.error("Error checking room status", e);
            return false;
        }
    }

    @Command
    @NotifyChange({"rooms", "myBookings", "successMessage", "errorMessage", "purpose", "recurring", "recurrenceEndDate", "selectedColleagues"})
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

        LocalDate localEndDate = null;
        if (recurring) {
            if (recurrenceEndDate == null) {
                errorMessage = "Please specify a recurrence end date.";
                ToastUtil.warning("Please specify a recurrence end date.");
                return;
            }
            localEndDate = recurrenceEndDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate localStartDate = selectDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (localEndDate.isBefore(localStartDate)) {
                errorMessage = "Recurrence end date cannot be before booking date.";
                ToastUtil.warning("Recurrence end date cannot be before booking date.");
                return;
            }
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
                    .recurrenceType(recurring ? recurrenceType.toUpperCase() : null)
                    .build();

            List<Long> inviteeUserIds = new java.util.ArrayList<>();
            if (selectedColleagues != null) {
                for (User u : selectedColleagues) {
                    inviteeUserIds.add(u.getId());
                }
            }

            bookingService.createBooking(booking, localEndDate, inviteeUserIds);
            successMessage = "Successfully booked room: " + room.getName() + (recurring ? " (Recurring)" : "");
            ToastUtil.success(successMessage);
            purpose = ""; // Clear input
            recurring = false;
            recurrenceEndDate = null;
            if (selectedColleagues != null) {
                selectedColleagues.clear();
            }
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

    public boolean isRecurring() {
        return recurring;
    }

    @NotifyChange("rooms")
    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public String getRecurrenceType() {
        return recurrenceType;
    }

    @NotifyChange("rooms")
    public void setRecurrenceType(String recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    public Date getRecurrenceEndDate() {
        return recurrenceEndDate;
    }

    @NotifyChange("rooms")
    public void setRecurrenceEndDate(Date recurrenceEndDate) {
        this.recurrenceEndDate = recurrenceEndDate;
    }

    public List<User> getColleagues() {
        return colleagues;
    }

    public void setColleagues(List<User> colleagues) {
        this.colleagues = colleagues;
    }

    public java.util.Set<User> getSelectedColleagues() {
        return selectedColleagues;
    }

    @NotifyChange("rooms")
    public void setSelectedColleagues(java.util.Set<User> selectedColleagues) {
        this.selectedColleagues = selectedColleagues;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
