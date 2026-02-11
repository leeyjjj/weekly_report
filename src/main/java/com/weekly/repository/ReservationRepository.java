package com.weekly.repository;

import com.weekly.entity.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = {"room", "user"})
    Optional<Reservation> findById(Long id);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reservation r " +
            "WHERE r.room.id = :roomId AND r.cancelled = false " +
            "AND r.startTime < :endTime AND r.endTime > :startTime")
    boolean existsOverlap(@Param("roomId") Long roomId,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reservation r " +
            "WHERE r.room.id = :roomId AND r.cancelled = false AND r.id != :excludeId " +
            "AND r.startTime < :endTime AND r.endTime > :startTime")
    boolean existsOverlapExcluding(@Param("roomId") Long roomId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime,
                                    @Param("excludeId") Long excludeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId AND r.cancelled = false " +
            "AND r.startTime < :endTime AND r.endTime > :startTime")
    List<Reservation> findOverlappingWithLock(@Param("roomId") Long roomId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    @EntityGraph(attributePaths = {"room", "user"})
    @Query("SELECT r FROM Reservation r WHERE r.cancelled = false " +
            "AND r.startTime < :endTime AND r.endTime > :startTime " +
            "ORDER BY r.startTime ASC")
    List<Reservation> findAllActiveInRange(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    @EntityGraph(attributePaths = {"room", "user"})
    @Query("SELECT r FROM Reservation r WHERE r.recurringGroupId = :groupId " +
            "AND r.cancelled = false AND r.startTime >= :fromTime " +
            "ORDER BY r.startTime ASC")
    List<Reservation> findFutureByRecurringGroup(@Param("groupId") String groupId,
                                                  @Param("fromTime") LocalDateTime fromTime);
}
