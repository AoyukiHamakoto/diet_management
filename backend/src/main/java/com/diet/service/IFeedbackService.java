package com.diet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.diet.entity.Feedback;

import java.util.Map;

public interface IFeedbackService extends IService<Feedback> {

    Feedback submitFeedback(Long userId, Long planId, int tasteRating, Integer satietyRating,
                            Integer difficultyRating, java.util.List<String> tags, String comment);

    IPage<Map<String, Object>> getHistory(Long userId, int page, int size);

    boolean shouldOfferReplace(Long userId, Long planId);

    /**
     * 简易「饮食执行反馈」简报：需至少 3 个不同日期有执行记录（非仅 PLANNED）才返回完整报告。
     */
    Map<String, Object> getExecutionFeedbackReport(Long userId);
}
