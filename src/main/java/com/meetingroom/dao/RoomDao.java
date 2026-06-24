package com.meetingroom.dao;

import com.meetingroom.model.Room;

import java.util.List;

public interface RoomDao {
    void save(Room room);
    void update(Room room);
    void delete(Long id);

    Room findById(Long id);
    Room findByName(String name);
    List<Room> findAll();
}
