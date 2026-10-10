package com.smartoffice.repository;

import com.smartoffice.entity.MeetingParticipant;
import com.smartoffice.entity.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    List<MeetingParticipant> findByMeetingId(Long meetingId);

    List<MeetingParticipant> findByEmployeeId(Long employeeId);

    Optional<MeetingParticipant> findByMeetingIdAndEmployeeId(Long meetingId, Long employeeId);

    boolean existsByMeetingIdAndEmployeeId(Long meetingId, Long employeeId);

    List<MeetingParticipant> findByMeetingIdAndAttendanceStatus(Long meetingId, ParticipantStatus attendanceStatus);

    List<MeetingParticipant> findByEmployeeIdAndMeetingStartDateTimeGreaterThanEqual(Long employeeId, LocalDateTime dateTime);
}
