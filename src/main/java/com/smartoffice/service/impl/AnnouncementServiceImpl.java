package com.smartoffice.service.impl;

import com.smartoffice.dto.announcement.AnnouncementCreateRequest;
import com.smartoffice.dto.announcement.AnnouncementResponse;
import com.smartoffice.dto.announcement.AnnouncementUpdateRequest;
import com.smartoffice.entity.Announcement;
import com.smartoffice.entity.User;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.exception.ResourceNotFoundException;
import com.smartoffice.repository.AnnouncementRepository;
import com.smartoffice.repository.UserRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.AnnouncementService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    public AnnouncementServiceImpl(AnnouncementRepository announcementRepository, UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementCreateRequest request, UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied: Only administrators can create announcements");
        }

        if (request.getExpiryDate() != null && request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Expiry date cannot be in the past");
        }

        User publisher = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        boolean isActive = request.getActive() == null || request.getActive();

        Announcement announcement = Announcement.builder()
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .publishedBy(publisher)
                .publishedAt(LocalDateTime.now())
                .expiryDate(request.getExpiryDate())
                .active(isActive)
                .build();

        Announcement saved = announcementRepository.save(announcement);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncementById(Long id, UserPrincipal currentUser) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (currentUser.getRole() != Role.ADMIN) {
            if (!announcement.isActive()) {
                throw new AccessDeniedException("Access denied: Announcement is not published");
            }
            if (announcement.getExpiryDate() != null && announcement.getExpiryDate().isBefore(LocalDate.now())) {
                throw new AccessDeniedException("Access denied: Announcement has expired");
            }
        }

        return mapToResponse(announcement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listAnnouncements(Boolean active, UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied: Only administrators can view full announcement management feed");
        }

        List<Announcement> announcements;
        if (active != null) {
            announcements = announcementRepository.findByActive(active);
        } else {
            announcements = announcementRepository.findAll();
        }

        return announcements.stream()
                .sorted(Comparator.comparing(Announcement::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getPublishedAnnouncements() {
        LocalDate today = LocalDate.now();
        List<Announcement> activeAnnouncements = announcementRepository.findByActive(true);

        return activeAnnouncements.stream()
                .filter(a -> a.getExpiryDate() == null || !a.getExpiryDate().isBefore(today))
                .sorted(Comparator.comparing(Announcement::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AnnouncementResponse updateAnnouncement(Long id, AnnouncementUpdateRequest request, UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied: Only administrators can update announcements");
        }

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (request.getExpiryDate() != null && request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Expiry date cannot be in the past");
        }

        if (StringUtils.hasText(request.getTitle())) {
            announcement.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null) {
            announcement.setContent(request.getContent().trim());
        }
        if (request.getExpiryDate() != null) {
            announcement.setExpiryDate(request.getExpiryDate());
        }
        if (request.getActive() != null) {
            announcement.setActive(request.getActive());
            if (announcement.isActive() && announcement.getPublishedAt() == null) {
                announcement.setPublishedAt(LocalDateTime.now());
            }
        }

        Announcement saved = announcementRepository.save(announcement);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AnnouncementResponse togglePublish(Long id, Boolean active, UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied: Only administrators can publish or unpublish announcements");
        }

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (active != null) {
            announcement.setActive(active);
        } else {
            announcement.setActive(!announcement.isActive());
        }

        if (announcement.isActive() && announcement.getPublishedAt() == null) {
            announcement.setPublishedAt(LocalDateTime.now());
        }

        Announcement saved = announcementRepository.save(announcement);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id, UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied: Only administrators can delete announcements");
        }

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        announcementRepository.delete(announcement);
    }

    private AnnouncementResponse mapToResponse(Announcement a) {
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .publishedByUserId(a.getPublishedBy() != null ? a.getPublishedBy().getId() : null)
                .publishedByUsername(a.getPublishedBy() != null ? a.getPublishedBy().getUsername() : null)
                .publishedAt(a.getPublishedAt())
                .expiryDate(a.getExpiryDate())
                .active(a.isActive())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
