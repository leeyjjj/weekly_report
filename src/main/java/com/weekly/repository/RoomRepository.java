package com.weekly.repository;

import com.weekly.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByActiveTrueOrderByNameAsc();

    List<Room> findAllByOrderByNameAsc();
}
