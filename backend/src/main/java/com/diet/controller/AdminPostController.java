package com.diet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.Result;
import com.diet.entity.Post;
import com.diet.mapper.PostMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/admin/post")
@RequiredArgsConstructor
@Tag(name = "管理员帖子审核接口")
@Slf4j
public class AdminPostController {

    private final PostMapper postMapper;

    @GetMapping("/pending")
    public Result<Map<String, Object>> pending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        Page<Post> p = postMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, "PENDING")
                        .eq(Post::getDeleted, 0)
                        .orderByAsc(Post::getCreateTime));
        return Result.success(Map.of(
                "records", p.getRecords(),
                "total", p.getTotal(),
                "pages", p.getPages(),
                "current", p.getCurrent()
        ));
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @AuthenticationPrincipal Long adminId) {
        if (adminId == null) return Result.error(401, "Unauthorized");
        Post p = postMapper.selectById(id);
        if (p == null || Integer.valueOf(1).equals(p.getDeleted())) {
            return Result.error(404, "Post not found");
        }
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, id)
                .set(Post::getStatus, "APPROVED")
                .set(Post::getRejectReason, null)
                .set(Post::getReviewerId, adminId)
                .set(Post::getReviewTime, LocalDateTime.now()));
        log.info("[业务-帖子审核] 审核通过, adminId={}, postId={}", adminId, id);
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @AuthenticationPrincipal Long adminId, @RequestBody RejectReq req) {
        if (adminId == null) return Result.error(401, "Unauthorized");
        Post p = postMapper.selectById(id);
        if (p == null || Integer.valueOf(1).equals(p.getDeleted())) {
            return Result.error(404, "Post not found");
        }
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, id)
                .set(Post::getStatus, "REJECTED")
                .set(Post::getRejectReason, req != null ? req.getReason() : null)
                .set(Post::getReviewerId, adminId)
                .set(Post::getReviewTime, LocalDateTime.now()));
        log.info("[业务-帖子审核] 审核驳回, adminId={}, postId={}, reason={}", adminId, id, p.getRejectReason());
        return Result.success();
    }

    @Data
    public static class RejectReq {
        private String reason;
    }
}

