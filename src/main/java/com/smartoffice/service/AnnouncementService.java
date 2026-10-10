package com.smartoffice.service;

import com.smartoffice.dto.announcement.AnnouncementCreateRequest;
import com.smartoffice.dto.announcement.AnnouncementResponse;
import com.smartoffice.dto.announcement.AnnouncementUpdateRequest;
import com.smartoffice.security.UserPrincipal;

import java.util.List;

public interface AnnouncementService {

    AnnouncementResponse createAnnouncement(AnnouncementCreateRequest request, UserPrincipal currentUser);

    AnnouncementResponse getAnnouncementById(Long id, UserPrincipal currentUser);

    List<AnnouncementResponse> listAnnouncements(Boolean active, UserPrincipal currentUser);

    List<AnnouncementResponse> getPublishedAnnouncements();

    AnnouncementResponse updateAnnouncement(Long id, AnnouncementUpdateRequest request, UserPrincipal currentUser);

    AnnouncementResponse togglePublish(Long id, Boolean active, UserPrincipal currentUser);

    void deleteAnnouncement(Long id, UserPrincipal currentUser);
}
