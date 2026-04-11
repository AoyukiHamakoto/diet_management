package com.diet.controller;

import com.diet.common.Result;
import com.diet.entity.Feedback;
import com.diet.service.IFeedbackService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final IFeedbackService feedbackService;

    @PostMapping
    public Result<FeedbackResponse> submit(@AuthenticationPrincipal Long userId,
                                           @Valid @RequestBody FeedbackRequest req) {
        if (userId == null) return Result.error(401, "Unauthorized");
        Feedback f = feedbackService.submitFeedback(
                userId,
                req.getPlanId(),
                req.getRating(),
                req.getSatietyRating(),
                req.getDifficultyRating(),
                req.getTags(),
                req.getComment());
        boolean offerReplace = feedbackService.shouldOfferReplace(userId, req.getPlanId());
        return Result.success(new FeedbackResponse(f, offerReplace));
    }

    @GetMapping("/history")
    public Result<Map<String, Object>> history(@AuthenticationPrincipal Long userId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        if (userId == null) return Result.error(401, "Unauthorized");
        var p = feedbackService.getHistory(userId, page, size);
        return Result.success(Map.of(
                "records", p.getRecords(),
                "total", p.getTotal(),
                "pages", p.getPages()
        ));
    }

    @GetMapping("/offer-replace")
    public Result<Boolean> offerReplace(@AuthenticationPrincipal Long userId, @RequestParam Long planId) {
        if (userId == null) return Result.error(401, "Unauthorized");
        return Result.success(feedbackService.shouldOfferReplace(userId, planId));
    }

    /**
     * 简易饮食执行反馈简报（需求 UC-007：至少 3 天执行数据）。
     */
    @GetMapping("/report")
    public Result<Map<String, Object>> report(@AuthenticationPrincipal Long userId) {
        if (userId == null) return Result.error(401, "Unauthorized");
        return Result.success(feedbackService.getExecutionFeedbackReport(userId));
    }

    @Data
    public static class FeedbackRequest {
        private Long planId;
        private Integer rating = 5;
        private Integer satietyRating;
        private Integer difficultyRating;
        private List<String> tags;
        private String comment;
    }

    @Data
    public static class FeedbackResponse {
        private Feedback feedback;
        private boolean offerReplace;
        public FeedbackResponse(Feedback f, boolean offer) {
            this.feedback = f;
            this.offerReplace = offer;
        }
    }
}
