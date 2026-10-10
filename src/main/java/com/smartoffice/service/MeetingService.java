package com.smartoffice.service;

import com.smartoffice.dto.meeting.MeetingCreateRequest;
import com.smartoffice.dto.meeting.MeetingParticipantCreateRequest;
import com.smartoffice.dto.meeting.MeetingParticipantResponse;
import com.smartoffice.dto.meeting.MeetingResponse;
import com.smartoffice.dto.meeting.MeetingUpdateRequest;
import com.smartoffice.entity.enums.MeetingStatus;
import com.smartoffice.entity.enums.ParticipantStatus;
import com.smartoffice.security.UserPrincipal;

import java.time.LocalDateTime;
import java.util.List;

public interface MeetingService {

    MeetingResponse createMeeting(MeetingCreateRequest request, UserPrincipal currentUser);

    MeetingResponse getMeetingById(Long id, UserPrincipal currentUser);

    List<MeetingResponse> listMeetings(MeetingStatus status, Long organizerId, LocalDateTime from, LocalDateTime to, UserPrincipal currentUser);

    List<MeetingResponse> getMyMeetings(UserPrincipal currentUser);

    MeetingResponse updateMeeting(Long id, MeetingUpdateRequest request, UserPrincipal currentUser);

    MeetingResponse updateMeetingStatus(Long id, MeetingStatus status, UserPrincipal currentUser);

    MeetingParticipantResponse addParticipant(Long meetingId, MeetingParticipantCreateRequest request, UserPrincipal currentUser);

    void removeParticipant(Long meetingId, Long employeeId, UserPrincipal currentUser);

    List<MeetingParticipantResponse> getMeetingParticipants(Long meetingId, UserPrincipal currentUser);

    MeetingParticipantResponse updateParticipantStatus(Long meetingId, Long employeeId, ParticipantStatus status, UserPrincipal currentUser);
}
