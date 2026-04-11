package com.diet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diet.entity.SystemNotification;

import java.util.List;

public interface ISystemNotificationService extends IService<SystemNotification> {

    void notify(Long userId, String message);

    List<SystemNotification> getUnread(Long userId);

    void markRead(Long id, Long userId);
}
