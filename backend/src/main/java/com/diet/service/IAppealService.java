package com.diet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.diet.entity.Appeal;

public interface IAppealService extends IService<Appeal> {

    /**
     * 用户提交申诉，校验 target 存在、属于用户、状态为 REJECTED、无未处理申诉
     */
    Appeal submit(Long userId, String targetType, Long targetId, String reason);

    /**
     * 是否有该 target 的未处理申诉（PENDING 或 PROCESSING）
     */
    boolean hasPendingAppeal(String targetType, Long targetId);

    /**
     * 管理员通过申诉：恢复 target 状态为 APPROVED，更新 appeal，发通知
     */
    void resolve(Long appealId, Long adminId, String adminComment);

    /**
     * 管理员拒绝申诉：维持 target 不变，更新 appeal，发通知
     */
    void reject(Long appealId, Long adminId, String adminComment);
}
