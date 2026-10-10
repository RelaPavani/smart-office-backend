package com.smartoffice.service;

import com.smartoffice.dto.meeting.MeetingCreateRequest;
import com.smartoffice.dto.meeting.MeetingParticipantCreateRequest;
import com.smartoffice.dto.meeting.MeetingParticipantResponse;
import com.smartoffice.dto.meeting.MeetingResponse;
import com.smartoffice.dto.meeting.MeetingUpdateRequest;
import com.smartoffice.entity.Department;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.Meeting;
import com.smartoffice.entity.MeetingParticipant;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.entity.enums.MeetingStatus;
import com.smartoffice.entity.enums.ParticipantStatus;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.exception.ConflictException;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.repository.MeetingParticipantRepository;
import com.smartoffice.repository.MeetingRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.impl.MeetingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal managerPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal nonParticipantPrincipal;

    private Department deptEngineering;
    private Employee organizerEmp;
    private Employee attendeeEmp;
    private Employee outsiderEmp;
    private Meeting testMeeting;
    private MeetingParticipant participant;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(1L, "admin", "admin@smartoffice.com", "pass", Role.ADMIN, true, Collections.emptyList());
        managerPrincipal = new UserPrincipal(2L, "manager", "manager@smartoffice.com", "pass", Role.MANAGER, true, Collections.emptyList());
        employeePrincipal = new UserPrincipal(3L, "emp1", "emp1@smartoffice.com", "pass", Role.EMPLOYEE, true, Collections.emptyList());
        nonParticipantPrincipal = new UserPrincipal(4L, "emp2", "emp2@smartoffice.com", "pass", Role.EMPLOYEE, true, Collections.emptyList());

        deptEngineering = Department.builder().id(10L).name("Engineering").active(true).build();

        organizerEmp = Employee.builder()
                .id(100L)
                .employeeCode("MGR001")
                .firstName("Alice")
                .lastName("Organizer")
                .email("manager@smartoffice.com")
                .department(deptEngineering)
                .status(EmployeeStatus.ACTIVE)
                .build();

        attendeeEmp = Employee.builder()
                .id(101L)
                .employeeCode("EMP001")
                .firstName("Bob")
                .lastName("Attendee")
                .email("emp1@smartoffice.com")
                .department(deptEngineering)
                .manager(organizerEmp)
                .status(EmployeeStatus.ACTIVE)
                .build();

        outsiderEmp = Employee.builder()
                .id(102L)
                .employeeCode("EMP002")
                .firstName("Charlie")
                .lastName("Outsider")
                .email("emp2@smartoffice.com")
                .status(EmployeeStatus.ACTIVE)
                .build();

        testMeeting = Meeting.builder()
                .id(200L)
                .title("Sprint Planning")
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .agenda("Plan Sprint 1")
                .status(MeetingStatus.SCHEDULED)
                .organizer(organizerEmp)
                .build();

        participant = MeetingParticipant.builder()
                .id(300L)
                .meeting(testMeeting)
                .employee(attendeeEmp)
                .attendanceStatus(ParticipantStatus.INVITED)
                .build();
    }

    @Test
    @DisplayName("Manager schedules meeting successfully")
    void testManagerScheduleMeeting() {
        MeetingCreateRequest request = MeetingCreateRequest.builder()
                .title("Sprint Planning")
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .agenda("Sprint 1")
                .organizerId(100L)
                .status(MeetingStatus.SCHEDULED)
                .build();

        when(employeeRepository.findByUserId(2L)).thenReturn(Optional.of(organizerEmp));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> {
            Meeting m = invocation.getArgument(0);
            m.setId(201L);
            return m;
        });

        MeetingResponse response = meetingService.createMeeting(request, managerPrincipal);

        assertNotNull(response);
        assertEquals(201L, response.getId());
        assertEquals("Sprint Planning", response.getTitle());
        verify(meetingParticipantRepository).save(any(MeetingParticipant.class));
    }

    @Test
    @DisplayName("Employee cannot schedule meetings")
    void testEmployeeScheduleMeetingThrowsAccessDenied() {
        MeetingCreateRequest request = MeetingCreateRequest.builder()
                .title("Unauthorized Meeting")
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .organizerId(101L)
                .build();

        assertThrows(AccessDeniedException.class, () -> meetingService.createMeeting(request, employeePrincipal));
    }

    @Test
    @DisplayName("Validate start date must be before end date")
    void testInvalidMeetingTimeThrows() {
        MeetingCreateRequest request = MeetingCreateRequest.builder()
                .title("Invalid Timing")
                .startDateTime(LocalDateTime.now().plusDays(2))
                .endDateTime(LocalDateTime.now().plusDays(1))
                .organizerId(100L)
                .build();

        assertThrows(BadRequestException.class, () -> meetingService.createMeeting(request, managerPrincipal));
    }

    @Test
    @DisplayName("Add participant successfully")
    void testAddParticipantSuccess() {
        MeetingParticipantCreateRequest request = MeetingParticipantCreateRequest.builder()
                .meetingId(200L)
                .employeeId(101L)
                .attendanceStatus(ParticipantStatus.INVITED)
                .build();

        when(meetingRepository.findById(200L)).thenReturn(Optional.of(testMeeting));
        when(employeeRepository.findByUserId(2L)).thenReturn(Optional.of(organizerEmp));
        when(employeeRepository.findById(101L)).thenReturn(Optional.of(attendeeEmp));
        when(meetingParticipantRepository.existsByMeetingIdAndEmployeeId(200L, 101L)).thenReturn(false);
        when(meetingParticipantRepository.save(any(MeetingParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MeetingParticipantResponse response = meetingService.addParticipant(200L, request, managerPrincipal);

        assertNotNull(response);
        assertEquals(101L, response.getEmployeeId());
    }

    @Test
    @DisplayName("Prevent duplicate meeting participants")
    void testPreventDuplicateParticipantThrows() {
        MeetingParticipantCreateRequest request = MeetingParticipantCreateRequest.builder()
                .meetingId(200L)
                .employeeId(101L)
                .build();

        when(meetingRepository.findById(200L)).thenReturn(Optional.of(testMeeting));
        when(employeeRepository.findByUserId(2L)).thenReturn(Optional.of(organizerEmp));
        when(employeeRepository.findById(101L)).thenReturn(Optional.of(attendeeEmp));
        when(meetingParticipantRepository.existsByMeetingIdAndEmployeeId(200L, 101L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> meetingService.addParticipant(200L, request, managerPrincipal));
    }

    @Test
    @DisplayName("Employee participant can view meeting details")
    void testEmployeeParticipantCanViewMeeting() {
        when(meetingRepository.findById(200L)).thenReturn(Optional.of(testMeeting));
        when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(attendeeEmp));
        when(meetingParticipantRepository.existsByMeetingIdAndEmployeeId(200L, 101L)).thenReturn(true);

        MeetingResponse response = meetingService.getMeetingById(200L, employeePrincipal);

        assertNotNull(response);
        assertEquals(200L, response.getId());
    }

    @Test
    @DisplayName("Non-participant employee cannot view meeting")
    void testNonParticipantCannotViewMeetingThrows() {
        when(meetingRepository.findById(200L)).thenReturn(Optional.of(testMeeting));
        when(employeeRepository.findByUserId(4L)).thenReturn(Optional.of(outsiderEmp));
        when(meetingParticipantRepository.existsByMeetingIdAndEmployeeId(200L, 102L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> meetingService.getMeetingById(200L, nonParticipantPrincipal));
    }

    @Test
    @DisplayName("Organizer cancels meeting")
    void testOrganizerCancelsMeeting() {
        when(meetingRepository.findById(200L)).thenReturn(Optional.of(testMeeting));
        when(employeeRepository.findByUserId(2L)).thenReturn(Optional.of(organizerEmp));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MeetingResponse response = meetingService.updateMeetingStatus(200L, MeetingStatus.CANCELLED, managerPrincipal);

        assertEquals(MeetingStatus.CANCELLED, response.getStatus());
    }
}
