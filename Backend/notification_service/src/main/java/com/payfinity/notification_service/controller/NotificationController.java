package com.payfinity.notification_service.controller;

import com.payfinity.notification_service.service.NotificationService;
import com.payfinity.notification_service.entity.Notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@RestController
@RequestMapping("/api/notify")
@CrossOrigin(origins = {"http://localhost:5173", "https://payfinity-jgpd.vercel.app", "https://payfinity-jgpd.vercel.app/"})
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @PostMapping
    public Notification sendNotification(@RequestBody Notification notification) {
        log.info("Send notification request received for user: {}", notification.getUserId());
        return notificationService.sendNotification(notification);
    }

    @GetMapping("/{userId}")
    public List<Notification> getNotificationsByUser(@PathVariable Long userId) {
        log.info("Get notifications request received for user: {}", userId);
        return notificationService.getNotificationsByUserId(userId);
    }

    @DeleteMapping("/user/{userId}")
    public void clearNotifications(@PathVariable Long userId) {
        log.info("Clear notifications request received for user: {}", userId);
        notificationService.clearNotifications(userId);
    }
}