package com.meetingroom.service;

import com.meetingroom.dao.RoomDao;
import com.meetingroom.model.Room;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("roomService")
@Transactional
@Slf4j
public class RoomServiceImpl implements RoomService{

    private final RoomDao roomDao;

    public RoomServiceImpl(RoomDao roomDao) {
        this.roomDao = roomDao;
    }

    @Override
    public void addRoom(Room room) throws Exception {
        log.info("Attempting to add room: {}", room.getName());
        if(roomDao.findByName(room.getName()) != null){
            log.warn("Room name already exists: {}", room.getName());
            throw new Exception("Room name already exists.");
        }
        roomDao.save(room);
        log.debug("Successfully saved room: {}", room.getName());
    }

    @Override
    public void updateRoom(Room room) throws Exception {
        log.info("Attempting to update room ID: {}", room.getId());
        Room existing = roomDao.findByName(room.getName());
        if (existing != null && !existing.getId().equals(room.getId())) {
            log.warn("Another room with same name exists: {}", room.getName());
            throw new Exception("Another room with this name already exists.");
        }
        Room persistentRoom = roomDao.findById(room.getId());
        if (persistentRoom != null) {
            persistentRoom.setName(room.getName());
            persistentRoom.setCapacity(room.getCapacity());
            persistentRoom.setLocation(room.getLocation());
            persistentRoom.setAmenities(room.getAmenities());
            roomDao.update(persistentRoom);
        }
        log.debug("Successfully updated room: {}", room.getName());
    }

    @Override
    public void deleteRoom(Long roomId) {
        log.info("Deleting room ID: {}", roomId);
        roomDao.delete(roomId);
    }

    @Override
    public Room getRoomById(Long roomId) {
        return roomDao.findById(roomId);
    }

    @Override
    public List<Room> getAllRooms() {
        return roomDao.findAll();
    }
}
