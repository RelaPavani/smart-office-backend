package com.smartoffice.service;

import com.smartoffice.dto.announcement.AnnouncementCreateRequest;
import com.smartoffice.dto.announcement.AnnouncementResponse;
import com.smartoffice.dto.announcement.AnnouncementUpdateRequest;
import com.smartoffice.entity.Announcement;
import com.smartoffice.entity.User;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.repository.AnnouncementRepository;
import com.smartoffice.repository.UserRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.impl.AnnouncementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnnouncementServiceImpl announcementService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal managerPrincipal;
    private UserPrincipal employeePrincipal;

    private User adminUser;
    private Announcement activeAnnouncement;
    private Announcement inactiveAnnouncement;
    private Announcement expiredAnnouncement;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(1L, "admin", "admin@smartoffice.com", "pass", Role.ADMIN, true, Collections.emptyList());
        managerPrincipal = new UserPrincipal(2L, "manager", "manager@smartoffice.com", "pass", Role.MANAGER, true, Collections.emptyList());
        employeePrincipal = new UserPrincipal(3L, "emp", "emp@smartoffice.com", "pass", Role.EMPLOYEE, true, Collections.emptyList());

        adminUser = User.builder().id(1L).username("admin").email("admin@smartoffice.com").role(Role.ADMIN).build();

        activeAnnouncement = Announcement.builder()
                .id(10L)
                .title("Office Annual Picnic")
                .content("Join us this Saturday for picnic.")
                .publishedBy(adminUser)
                .publishedAt(LocalDateTime.now())
                .expiryDate(LocalDate.now().plusDays(10))
                .active(true)
                .build();

        inactiveAnnouncement = Announcement.builder()
                .id(20L)
                .title("Draft Policy")
                .content("Draft under review.")
                .publishedBy(adminUser)
                .publishedAt(LocalDateTime.now())
                .active(false)
                .build();

        expiredAnnouncement = Announcement.builder()
                .id(30L)
                .title("Expired Event")
                .content("Past event.")
                .publishedBy(adminUser)
                .publishedAt(LocalDateTime.now().minusDays(10))
                .expiryDate(LocalDate.now().minusDays(1))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Admin creates announcement successfully")
    void testAdminCreateAnnouncement() {
        AnnouncementCreateRequest request = AnnouncementCreateRequest.builder()
                .title("Town Hall Meeting")
                .content("All-hands meeting next Friday.")
                .expiryDate(LocalDate.now().plusDays(14))
                .active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement a = invocation.getArgument(0);
            a.setId(100L);
            return a;
        });

        AnnouncementResponse response = announcementService.createAnnouncement(request, adminPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Town Hall Meeting", response.getTitle());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("Manager and Employee cannot create announcements")
    void testNonAdminCreateAnnouncementThrows() {
        AnnouncementCreateRequest request = AnnouncementCreateRequest.builder()
                .title("Unauthorized Announcement")
                .content("Content")
                .build();

        assertThrows(AccessDeniedException.class, () -> announcementService.createAnnouncement(request, managerPrincipal));
        assertThrows(AccessDeniedException.class, () -> announcementService.createAnnouncement(request, employeePrincipal));
    }

    @Test
    @DisplayName("Expiry date in the past throws BadRequestException")
    void testPastExpiryDateThrows() {
        AnnouncementCreateRequest request = AnnouncementCreateRequest.builder()
                .title("Past Announcement")
                .content("Content")
                .expiryDate(LocalDate.now().minusDays(2))
                .build();

        assertThrows(BadRequestException.class, () -> announcementService.createAnnouncement(request, adminPrincipal));
    }

    @Test
    @DisplayName("Employee can read active non-expired announcement")
    void testEmployeeCanReadActiveAnnouncement() {
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(activeAnnouncement));

        AnnouncementResponse response = announcementService.getAnnouncementById(10L, employeePrincipal);

        assertNotNull(response);
        assertEquals(10L, response.getId());
    }

    @Test
    @DisplayName("Employee cannot read inactive or expired announcement")
    void testEmployeeCannotReadInactiveOrExpired() {
        when(announcementRepository.findById(20L)).thenReturn(Optional.of(inactiveAnnouncement));
        assertThrows(AccessDeniedException.class, () -> announcementService.getAnnouncementById(20L, employeePrincipal));

        when(announcementRepository.findById(30L)).thenReturn(Optional.of(expiredAnnouncement));
        assertThrows(AccessDeniedException.class, () -> announcementService.getAnnouncementById(30L, employeePrincipal));
    }

    @Test
    @DisplayName("Admin can view any announcement including inactive")
    void testAdminCanReadInactiveAnnouncement() {
        when(announcementRepository.findById(20L)).thenReturn(Optional.of(inactiveAnnouncement));

        AnnouncementResponse response = announcementService.getAnnouncementById(20L, adminPrincipal);

        assertNotNull(response);
        assertFalse(response.isActive());
    }

    @Test
    @DisplayName("Published feed returns only active, non-expired announcements")
    void testPublishedFeedFiltering() {
        when(announcementRepository.findByActive(true)).thenReturn(List.of(activeAnnouncement, expiredAnnouncement));

        List<AnnouncementResponse> feed = announcementService.getPublishedAnnouncements();

        assertEquals(1, feed.size());
        assertEquals("Office Annual Picnic", feed.get(0).getTitle());
    }

    @Test
    @DisplayName("Admin can toggle publication status")
    void testAdminTogglePublish() {
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(activeAnnouncement));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnnouncementResponse response = announcementService.togglePublish(10L, false, adminPrincipal);

        assertFalse(response.isActive());
    }

    @Test
    @DisplayName("Employee cannot delete announcement")
    void testEmployeeCannotDeleteAnnouncement() {
        assertThrows(AccessDeniedException.class, () -> announcementService.deleteAnnouncement(10L, employeePrincipal));
    }

    @Test
    @DisplayName("Admin can delete announcement")
    void testAdminDeletesAnnouncement() {
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(activeAnnouncement));

        announcementService.deleteAnnouncement(10L, adminPrincipal);

        verify(announcementRepository).delete(activeAnnouncement);
    }
}
