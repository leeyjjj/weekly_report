package com.weekly.service;

import com.weekly.entity.Room;
import com.weekly.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaults() {
        if (roomRepository.count() > 0) return;
        log.info("Seeding default room...");
        Room room = new Room();
        room.setName("회의실1");
        room.setLocation("본사");
        room.setCapacity(10);
        room.setDescription("기본 회의실");
        roomRepository.save(room);
    }

    public List<Room> findAllActive() {
        return roomRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<Room> findAll() {
        return roomRepository.findAllByOrderByNameAsc();
    }

    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회의실을 찾을 수 없습니다: " + id));
    }

    @Transactional
    public Room save(Room room) {
        return roomRepository.save(room);
    }

    @Transactional
    public void deactivate(Long id) {
        Room room = findById(id);
        room.setActive(false);
        roomRepository.save(room);
    }

    @Transactional
    public void activate(Long id) {
        Room room = findById(id);
        room.setActive(true);
        roomRepository.save(room);
    }
}
