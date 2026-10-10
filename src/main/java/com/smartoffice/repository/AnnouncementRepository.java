package com.smartoffice.repository;

import com.smartoffice.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByActive(boolean active);

    List<Announcement> findByActiveTrueAndExpiryDateGreaterThanEqual(LocalDate date);

    List<Announcement> findByPublishedById(Long userId);

    List<Announcement> findByActiveTrueOrderByPublishedAtDesc();
}
