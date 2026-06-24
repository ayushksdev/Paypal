package com.payfinity.notification_service.service;

import com.payfinity.notification_service.entity.Notification;
import com.payfinity.notification_service.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {


    @Autowired
    private NotificationRepository notificationRepository;
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Override
    public Notification sendNotification(Notification notification) {
        log.info("Saving notification to database for user: {}", notification.getUserId());
        notification.setSentAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getNotificationsByUserId(Long userId) {
        log.info("Fetching notifications from database for user: {}", userId);
        return notificationRepository.findByUserId(userId);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void clearNotifications(Long userId) {
        log.info("Deleting notifications from database for user: {}", userId);
        notificationRepository.deleteByUserId(userId);
    }
}