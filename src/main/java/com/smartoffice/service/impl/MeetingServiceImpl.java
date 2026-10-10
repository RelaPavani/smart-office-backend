package com.smartoffice.service.impl;

import com.smartoffice.dto.meeting.MeetingCreateRequest;
import com.smartoffice.dto.meeting.MeetingParticipantCreateRequest;
import com.smartoffice.dto.meeting.MeetingParticipantResponse;
import com.smartoffice.dto.meeting.MeetingResponse;
import com.smartoffice.dto.meeting.MeetingUpdateRequest;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.Meeting;
import com.smartoffice.entity.MeetingParticipant;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.entity.enums.MeetingStatus;
import com.smartoffice.entity.enums.ParticipantStatus;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.exception.ConflictException;
import com.smartoffice.exception.ResourceNotFoundException;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.repository.MeetingParticipantRepository;
import com.smartoffice.repository.MeetingRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.MeetingService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final EmployeeRepository employeeRepository;

    public MeetingServiceImpl(
            MeetingRepository meetingRepository,
            MeetingParticipantRepository meetingParticipantRepository,
            EmployeeRepository employeeRepository) {
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public MeetingResponse createMeeting(MeetingCreateRequest request, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Access denied: Employees cannot schedule meetings");
        }

        if (request.getEndDateTime().isBefore(request.getStartDateTime())
                || request.getEndDateTime().isEqual(request.getStartDateTime())) {
            throw new BadRequestException("Meeting end time must be after start time");
        }

        if (request.getStartDateTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BadRequestException("Meeting start time cannot be in the past");
        }

        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);

        Employee organizer;
        if (currentUser.getRole() == Role.MANAGER) {
            if (currentEmp == null) {
                throw new AccessDeniedException("Manager employee record not found");
            }
            organizer = currentEmp;
        } else {
            // ADMIN
            if (request.getOrganizerId() != null) {
                organizer = employeeRepository.findById(request.getOrganizerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getOrganizerId()));
            } else if (currentEmp != null) {
                organizer = currentEmp;
            } else {
                throw new BadRequestException("Organizer employee ID is required");
            }
        }

        if (organizer.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BadRequestException("Organizer must be an active employee");
        }

        Meeting meeting = Meeting.builder()
                .title(request.getTitle().trim())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .agenda(request.getAgenda())
                .status(request.getStatus() != null ? request.getStatus() : MeetingStatus.SCHEDULED)
                .organizer(organizer)
                .build();

        Meeting saved = meetingRepository.save(meeting);

        // Automatically add organizer as participant
        MeetingParticipant organizerParticipant = MeetingParticipant.builder()
                .meeting(saved)
                .employee(organizer)
                .attendanceStatus(ParticipantStatus.ACCEPTED)
                .build();
        meetingParticipantRepository.save(organizerParticipant);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingResponse getMeetingById(Long id, UserPrincipal currentUser) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", "id", id));

        validateMeetingAccess(meeting, currentUser);

        return mapToResponse(meeting);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> listMeetings(MeetingStatus status, Long organizerId, LocalDateTime from, LocalDateTime to, UserPrincipal currentUser) {
        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);

        List<Meeting> allMeetings = meetingRepository.findAll();

        return allMeetings.stream()
                .filter(m -> hasMeetingReadPermission(m, currentUser, currentEmp))
                .filter(m -> status == null || m.getStatus() == status)
                .filter(m -> organizerId == null || m.getOrganizer().getId().equals(organizerId))
                .filter(m -> from == null || !m.getStartDateTime().isBefore(from))
                .filter(m -> to == null || !m.getEndDateTime().isAfter(to))
                .sorted(Comparator.comparing(Meeting::getStartDateTime).reversed())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> getMyMeetings(UserPrincipal currentUser) {
        Employee currentEmp = resolveCurrentEmployee(currentUser);

        List<MeetingParticipant> myParticipations = meetingParticipantRepository.findByEmployeeId(currentEmp.getId());
        Set<Long> meetingIds = myParticipations.stream()
                .map(mp -> mp.getMeeting().getId())
                .collect(Collectors.toSet());

        List<Meeting> organized = meetingRepository.findByOrganizerId(currentEmp.getId());
        for (Meeting m : organized) {
            meetingIds.add(m.getId());
        }

        List<Meeting> meetings = meetingRepository.findAllById(meetingIds);
        return meetings.stream()
                .sorted(Comparator.comparing(Meeting::getStartDateTime).reversed())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MeetingResponse updateMeeting(Long id, MeetingUpdateRequest request, UserPrincipal currentUser) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", "id", id));

        validateMeetingModifyPermission(meeting, currentUser);

        LocalDateTime newStart = request.getStartDateTime() != null ? request.getStartDateTime() : meeting.getStartDateTime();
        LocalDateTime newEnd = request.getEndDateTime() != null ? request.getEndDateTime() : meeting.getEndDateTime();

        if (newEnd.isBefore(newStart) || newEnd.isEqual(newStart)) {
            throw new BadRequestException("Meeting end time must be after start time");
        }

        if (StringUtils.hasText(request.getTitle())) {
            meeting.setTitle(request.getTitle().trim());
        }
        if (request.getStartDateTime() != null) {
            meeting.setStartDateTime(request.getStartDateTime());
        }
        if (request.getEndDateTime() != null) {
            meeting.setEndDateTime(request.getEndDateTime());
        }
        if (request.getAgenda() != null) {
            meeting.setAgenda(request.getAgenda());
        }
        if (request.getStatus() != null) {
            meeting.setStatus(request.getStatus());
        }

        Meeting saved = meetingRepository.save(meeting);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public MeetingResponse updateMeetingStatus(Long id, MeetingStatus status, UserPrincipal currentUser) {
        if (status == null) {
            throw new BadRequestException("Meeting status is required");
        }

        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", "id", id));

        validateMeetingModifyPermission(meeting, currentUser);

        meeting.setStatus(status);
        Meeting saved = meetingRepository.save(meeting);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public MeetingParticipantResponse addParticipant(Long meetingId, MeetingParticipantCreateRequest request, UserPrincipal currentUser) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", "id", meetingId));

        validateMeetingModifyPermission(meeting, currentUser);

        Long empId = request.getEmployeeId();
        Employee employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", empId));

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BadRequestException("Cannot invite an inactive employee to a meeting");
        }

        if (meetingParticipantRepository.existsByMeetingIdAndEmployeeId(meetingId, empId)) {
            throw new ConflictException("Employee is already a participant of this meeting");
        }

        ParticipantStatus initialStatus = request.getAttendanceStatus() != null ? request.getAttendanceStatus() : ParticipantStatus.INVITED;

        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .employee(employee)
                .attendanceStatus(initialStatus)
                .build();

        MeetingParticipant saved = meetingParticipantRepository.save(participant);
        return mapToParticipantResponse(saved);
    }

    @Override
    @Transactional
    public void removeParticipant(Long meetingId, Long employeeId, UserPrincipal currentUser) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", "id", meetingId));

        validateMeetingModifyPermission(meeting, currentUser);

        if (meeting.getOrganizer().getId().equals(employeeId)) {
            throw new BadRequestException("Cannot remove the organizer from the meeting participants");
        }

        MeetingParticipant participant = meetingParticipantRepository.findByMeetingIdAndEmployeeId(meetingId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting participant not found for meeting: " + meetingId + " and employee: " + employeeId));

        meetingParticipantRepository.delete(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingParticipantResponse> getMeetingParticipants(Long meetingId, UserPrincipal currentUser) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", "id", meetingId));

        validateMeetingAccess(meeting, currentUser);

        return meetingParticipantRepository.findByMeetingId(meetingId).stream()
                .map(this::mapToParticipantResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MeetingParticipantResponse updateParticipantStatus(Long meetingId, Long employeeId, ParticipantStatus status, UserPrincipal currentUser) {
        if (status == null) {
            throw new BadRequestException("Attendance status is required");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", "id", meetingId));

        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);

        boolean isSelf = currentEmp != null && currentEmp.getId().equals(employeeId);
        boolean isOrganizer = currentEmp != null && meeting.getOrganizer().getId().equals(currentEmp.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isSelf && !isOrganizer && !isAdmin) {
            throw new AccessDeniedException("Access denied: You cannot change another participant's attendance status");
        }

        MeetingParticipant participant = meetingParticipantRepository.findByMeetingIdAndEmployeeId(meetingId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting participant not found for employee id: " + employeeId));

        participant.setAttendanceStatus(status);
        MeetingParticipant saved = meetingParticipantRepository.save(participant);
        return mapToParticipantResponse(saved);
    }

    private void validateMeetingAccess(Meeting meeting, UserPrincipal currentUser) {
        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);
        if (!hasMeetingReadPermission(meeting, currentUser, currentEmp)) {
            throw new AccessDeniedException("Access denied: You are not authorized to view this meeting");
        }
    }

    private boolean hasMeetingReadPermission(Meeting meeting, UserPrincipal currentUser, Employee currentEmp) {
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }

        if (currentEmp == null) {
            return false;
        }

        if (meeting.getOrganizer().getId().equals(currentEmp.getId())) {
            return true;
        }

        if (meetingParticipantRepository.existsByMeetingIdAndEmployeeId(meeting.getId(), currentEmp.getId())) {
            return true;
        }

        if (currentUser.getRole() == Role.MANAGER) {
            return isWithinManagerScope(currentEmp, meeting.getOrganizer());
        }

        return false;
    }

    private void validateMeetingModifyPermission(Meeting meeting, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Access denied: Employees cannot modify meetings");
        }

        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);
        if (currentEmp == null) {
            throw new AccessDeniedException("Manager employee record not found");
        }

        if (meeting.getOrganizer().getId().equals(currentEmp.getId())) {
            return;
        }

        if (isWithinManagerScope(currentEmp, meeting.getOrganizer())) {
            return;
        }

        throw new AccessDeniedException("Access denied: You can only modify meetings you organized or manage");
    }

    private boolean isWithinManagerScope(Employee managerEmp, Employee targetEmp) {
        if (managerEmp.getId().equals(targetEmp.getId())) {
            return true;
        }
        boolean isDirectReport = targetEmp.getManager() != null && targetEmp.getManager().getId().equals(managerEmp.getId());
        boolean isSameDept = targetEmp.getDepartment() != null && managerEmp.getDepartment() != null
                && targetEmp.getDepartment().getId().equals(managerEmp.getDepartment().getId());
        return isDirectReport || isSameDept;
    }

    private Employee resolveCurrentEmployee(UserPrincipal currentUser) {
        return employeeRepository.findByUserId(currentUser.getId())
                .or(() -> employeeRepository.findByEmail(currentUser.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee profile not found for user: " + currentUser.getUsername()));
    }

    private Employee resolveCurrentEmployeeOrNull(UserPrincipal currentUser) {
        return employeeRepository.findByUserId(currentUser.getId())
                .or(() -> employeeRepository.findByEmail(currentUser.getEmail()))
                .orElse(null);
    }

    private MeetingResponse mapToResponse(Meeting m) {
        return MeetingResponse.builder()
                .id(m.getId())
                .title(m.getTitle())
                .startDateTime(m.getStartDateTime())
                .endDateTime(m.getEndDateTime())
                .agenda(m.getAgenda())
                .status(m.getStatus())
                .organizerId(m.getOrganizer() != null ? m.getOrganizer().getId() : null)
                .organizerName(m.getOrganizer() != null ? m.getOrganizer().getFirstName() + " " + m.getOrganizer().getLastName() : null)
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private MeetingParticipantResponse mapToParticipantResponse(MeetingParticipant mp) {
        return MeetingParticipantResponse.builder()
                .id(mp.getId())
                .meetingId(mp.getMeeting().getId())
                .meetingTitle(mp.getMeeting().getTitle())
                .employeeId(mp.getEmployee().getId())
                .employeeName(mp.getEmployee().getFirstName() + " " + mp.getEmployee().getLastName())
                .employeeCode(mp.getEmployee().getEmployeeCode())
                .attendanceStatus(mp.getAttendanceStatus())
                .createdAt(mp.getCreatedAt())
                .updatedAt(mp.getUpdatedAt())
                .build();
    }
}
