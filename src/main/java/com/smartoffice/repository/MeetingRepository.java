package com.smartoffice.repository;

import com.smartoffice.entity.Meeting;
import com.smartoffice.entity.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByOrganizerId(Long organizerId);

    List<Meeting> findByStartDateTimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<Meeting> findByStatus(MeetingStatus status);

    List<Meeting> findByStartDateTimeGreaterThanEqual(LocalDateTime dateTime);
}
