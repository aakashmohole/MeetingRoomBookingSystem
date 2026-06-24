package com.meetingroom.service;

import com.meetingroom.model.Room;

import java.util.List;

public interface RoomService {
    void addRoom(Room room) throws Exception;
    void updateRoom(Room room) throws Exception;
    void deleteRoom(Long roomId);
    Room getRoomById(Long roomId);
    List<Room> getAllRooms();

}
