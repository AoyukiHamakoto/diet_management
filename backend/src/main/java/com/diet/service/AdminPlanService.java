package com.diet.service;

import java.time.LocalDate;
import java.util.Map;

public interface AdminPlanService {

    Map<String, Object> listPlans(long page, long size, String auditStatus, Long userId, LocalDate planDate);

    Map<String, Object> getPlanDetail(Long planId);

    void approvePlan(Long planId, Long adminId);

    void rejectPlan(Long planId, Long adminId, String reason);
}
