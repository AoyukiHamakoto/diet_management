package com.diet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.SystemNotification;
import com.diet.mapper.SystemNotificationMapper;
import com.diet.service.ISystemNotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemNotificationServiceImpl extends ServiceImpl<SystemNotificationMapper, SystemNotification> implements ISystemNotificationService {

    @Override
    public void notify(Long userId, String message) {
        SystemNotification n = new SystemNotification();
        n.setUserId(userId);
        n.setMessage(message);
        n.setIsRead(false);
        save(n);
    }

    @Override
    public List<SystemNotification> getUnread(Long userId) {
        return lambdaQuery()
                .eq(SystemNotification::getUserId, userId)
                .eq(SystemNotification::getIsRead, false)
                .orderByDesc(SystemNotification::getCreateTime)
                .list();
    }

    @Override
    public void markRead(Long id, Long userId) {
        SystemNotification n = getById(id);
        if (n != null && n.getUserId().equals(userId)) {
            n.setIsRead(true);
            updateById(n);
        }
    }
}
