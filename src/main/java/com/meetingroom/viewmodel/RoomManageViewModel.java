package com.meetingroom.viewmodel;

import com.meetingroom.model.Room;
import com.meetingroom.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import java.util.List;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
@Slf4j
public class RoomManageViewModel {
    @WireVariable
    private RoomService roomService;

    private List<Room> rooms;
    private Room selectedRoom = new Room();
    private String feedbackMessage;
    private String errorFeedback;
    private boolean editing = false;

    @Init
    public void init() {
        rooms = roomService.getAllRooms();
    }

    @Command
    @NotifyChange({"rooms", "selectedRoom", "feedbackMessage", "errorFeedback", "editing"})
    public void saveRoom(){
        feedbackMessage = null;
        errorFeedback = null;

        try {
            if (selectedRoom.getName() == null || selectedRoom.getName().trim().isEmpty()) {
                errorFeedback = "Room name is required.";
                return;
            }
            if (selectedRoom.getCapacity() <= 0) {
                errorFeedback = "Capacity must be positive.";
                return;
            }
            if (selectedRoom.getLocation() == null || selectedRoom.getLocation().trim().isEmpty()) {
                errorFeedback = "Location is required.";
                return;
            }
            if (selectedRoom.getId() == null) {
                roomService.addRoom(selectedRoom);
                feedbackMessage = "Room added successfully!";
            } else {
                roomService.updateRoom(selectedRoom);
                feedbackMessage = "Room updated successfully!";
            }
            rooms = roomService.getAllRooms();
            selectedRoom = new Room();
            editing = false;
        } catch (Exception e) {
            errorFeedback = e.getMessage();
        }
    }

    @Command
    @NotifyChange({"selectedRoom", "editing", "errorFeedback", "feedbackMessage"})
    public void editRoom(@BindingParam("room") Room room) {
        selectedRoom = Room.builder()
                .id(room.getId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .location(room.getLocation())
                .amenities(room.getAmenities())
                .build();
        editing = true;
        feedbackMessage = null;
        errorFeedback = null;
    }

    @Command
    @NotifyChange({"rooms", "selectedRoom", "editing", "feedbackMessage", "errorFeedback"})
    public void deleteRoom(@BindingParam("id") Long id){
        roomService.deleteRoom(id);
        rooms = roomService.getAllRooms();
        selectedRoom = new Room();
        editing = false;
        feedbackMessage = "Room deleted successfully!";
        errorFeedback = null;
    }

    @Command
    @NotifyChange({"selectedRoom", "editing", "errorFeedback", "feedbackMessage"})
    public void cancelEdit() {
        selectedRoom = new Room();
        editing = false;
        feedbackMessage = null;
        errorFeedback = null;
    }

    public List<Room> getRooms() { return rooms; }
    public Room getSelectedRoom() { return selectedRoom; }
    public void setSelectedRoom(Room selectedRoom) { this.selectedRoom = selectedRoom; }
    public String getFeedbackMessage() { return feedbackMessage; }
    public String getErrorFeedback() { return errorFeedback; }
    public boolean isEditing() { return editing; }
}
