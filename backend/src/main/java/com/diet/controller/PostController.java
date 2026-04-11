package com.diet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.Result;
import com.diet.entity.*;
import com.diet.mapper.CommentLikeMapper;
import com.diet.mapper.PostCommentMapper;
import com.diet.mapper.PostLikeMapper;
import com.diet.mapper.PostMapper;
import com.diet.mapper.UserMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
@Tag(name = "帖子接口")
@Slf4j
public class PostController {

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostCommentMapper postCommentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;

    @PostMapping
    public Result<Post> create(@AuthenticationPrincipal Long userId, @RequestBody CreateReq req) {
        if (userId == null)
            return Result.error(401, "Unauthorized");
        if (req == null || req.getTitle() == null || req.getTitle().isBlank() || req.getContent() == null
                || req.getContent().isBlank()) {
            return Result.error(400, "title/content is required");
        }
        Post p = new Post();
        p.setUserId(userId);
        p.setTitle(req.getTitle().trim());
        p.setContent(req.getContent().trim());
        p.setStatus("PENDING");
        p.setDeleted(0);
        p.setLikeCount(0);
        p.setCreateTime(LocalDateTime.now());
        p.setUpdateTime(LocalDateTime.now());
        postMapper.insert(p);
        log.info("[业务-帖子] 提交帖子成功, userId={}, postId={}", userId, p.getId());
        return Result.success(p);
    }

    @GetMapping("/my")
    public Result<Map<String, Object>> myPosts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        if (userId == null)
            return Result.error(401, "Unauthorized");
        Page<Post> p = postMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .eq(Post::getDeleted, 0)
                        .orderByDesc(Post::getCreateTime));
        return Result.success(Map.of(
                "records", p.getRecords(),
                "total", p.getTotal(),
                "pages", p.getPages(),
                "current", p.getCurrent()));
    }

    @GetMapping("/public")
    public Result<Map<String, Object>> publicPosts(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Post> qw = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, "APPROVED")
                .eq(Post::getDeleted, 0)
                .orderByDesc(Post::getCreateTime);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Post::getTitle, keyword).or().like(Post::getContent, keyword));
        }
        Page<Post> p = postMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> rows = p.getRecords().stream().map(this::postWithAuthor).toList();
        return Result.success(Map.of(
                "records", rows,
                "total", p.getTotal(),
                "pages", p.getPages(),
                "current", p.getCurrent()));
    }

    /**
     * 帖子详情（社区）：含是否已点赞、作者昵称。
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        Post p = postMapper.selectById(id);
        if (p == null || Integer.valueOf(1).equals(p.getDeleted())) {
            return Result.error(404, "Post not found");
        }
        if (!"APPROVED".equals(p.getStatus())) {
            if (userId == null || !userId.equals(p.getUserId())) {
                return Result.error(403, "帖子未通过审核或无权查看");
            }
        }
        boolean liked = false;
        if (userId != null) {
            liked = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                    .eq(PostLike::getPostId, id)
                    .eq(PostLike::getUserId, userId)) > 0;
        }
        User author = userMapper.selectById(p.getUserId());
        String nickname = author != null && author.getNickname() != null ? author.getNickname() : "用户";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("post", p);
        m.put("liked", liked);
        m.put("authorNickname", nickname);
        return Result.success(m);
    }

    @PostMapping("/{id}/like")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> togglePostLike(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        if (userId == null)
            return Result.error(401, "Unauthorized");
        Post p = postMapper.selectById(id);
        if (p == null || Integer.valueOf(1).equals(p.getDeleted()) || !"APPROVED".equals(p.getStatus())) {
            return Result.error(404, "Post not found");
        }
        LambdaQueryWrapper<PostLike> w = new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, id)
                .eq(PostLike::getUserId, userId);
        long cnt = postLikeMapper.selectCount(w);
        boolean nowLiked;
        if (cnt > 0) {
            postLikeMapper.delete(w);
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, id)
                    .setSql("like_count = GREATEST(IFNULL(like_count,0) - 1, 0)"));
            nowLiked = false;
        } else {
            PostLike pl = new PostLike();
            pl.setUserId(userId);
            pl.setPostId(id);
            pl.setCreateTime(LocalDateTime.now());
            postLikeMapper.insert(pl);
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, id)
                    .setSql("like_count = IFNULL(like_count,0) + 1"));
            nowLiked = true;
        }
        Post fresh = postMapper.selectById(id);
        return Result.success(Map.of(
                "liked", nowLiked,
                "likeCount", fresh != null && fresh.getLikeCount() != null ? fresh.getLikeCount() : 0));
    }

    @GetMapping("/{id}/comments")
    public Result<List<Map<String, Object>>> listComments(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        Post p = postMapper.selectById(id);
        if (p == null || Integer.valueOf(1).equals(p.getDeleted())) {
            return Result.error(404, "Post not found");
        }
        // 与 detail 一致：公开列表均为 APPROVED；未审核/驳回帖仅作者可拉取评论，避免详情成功而评论 403 造成前端连环报错
        if (!"APPROVED".equals(p.getStatus())) {
            if (userId == null || !userId.equals(p.getUserId())) {
                return Result.error(403, "无权查看");
            }
        }
        List<PostComment> list = postCommentMapper.selectList(new LambdaQueryWrapper<PostComment>()
                .eq(PostComment::getPostId, id)
                .orderByAsc(PostComment::getCreateTime));
        Set<Long> uids = list.stream().map(PostComment::getUserId).collect(Collectors.toSet());
        Map<Long, String> names = nicknames(uids);
        Set<Long> likedCommentIds = new HashSet<>();
        if (userId != null && !list.isEmpty()) {
            List<Long> cids = list.stream().map(PostComment::getId).toList();
            List<CommentLike> likes = commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getUserId, userId)
                    .in(CommentLike::getCommentId, cids));
            likedCommentIds = likes.stream().map(CommentLike::getCommentId).collect(Collectors.toSet());
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (PostComment c : list) {
            boolean liked = userId != null && likedCommentIds.contains(c.getId());
            out.add(commentRow(c, names.getOrDefault(c.getUserId(), "用户"), liked));
        }
        return Result.success(out);
    }

    @PostMapping("/{id}/comments")
    @Transactional(rollbackFor = Exception.class)
    public Result<PostComment> addComment(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestBody CommentReq req) {
        if (userId == null)
            return Result.error(401, "Unauthorized");
        Post p = postMapper.selectById(id);
        if (p == null || Integer.valueOf(1).equals(p.getDeleted()) || !"APPROVED".equals(p.getStatus())) {
            return Result.error(404, "Post not found");
        }
        String content = req != null && req.getContent() != null ? req.getContent().trim() : "";
        if (content.isEmpty() || content.length() > 2000) {
            return Result.error(400, "评论内容无效");
        }
        Long parentId = req != null ? req.getParentId() : null;
        if (parentId != null) {
            PostComment parent = postCommentMapper.selectById(parentId);
            if (parent == null || !id.equals(parent.getPostId())) {
                return Result.error(400, "回复目标不存在");
            }
        }
        PostComment c = new PostComment();
        c.setPostId(id);
        c.setUserId(userId);
        c.setParentId(parentId);
        c.setContent(content);
        c.setLikeCount(0);
        c.setCreateTime(LocalDateTime.now());
        postCommentMapper.insert(c);
        return Result.success(c);
    }

    @PostMapping("/comment/{commentId}/like")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> toggleCommentLike(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        if (userId == null)
            return Result.error(401, "Unauthorized");
        PostComment c = postCommentMapper.selectById(commentId);
        if (c == null) {
            return Result.error(404, "Comment not found");
        }
        LambdaQueryWrapper<CommentLike> w = new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, userId);
        long cnt = commentLikeMapper.selectCount(w);
        boolean nowLiked;
        if (cnt > 0) {
            commentLikeMapper.delete(w);
            postCommentMapper.update(null, new LambdaUpdateWrapper<PostComment>()
                    .eq(PostComment::getId, commentId)
                    .setSql("like_count = GREATEST(IFNULL(like_count,0) - 1, 0)"));
            nowLiked = false;
        } else {
            CommentLike cl = new CommentLike();
            cl.setUserId(userId);
            cl.setCommentId(commentId);
            cl.setCreateTime(LocalDateTime.now());
            commentLikeMapper.insert(cl);
            postCommentMapper.update(null, new LambdaUpdateWrapper<PostComment>()
                    .eq(PostComment::getId, commentId)
                    .setSql("like_count = IFNULL(like_count,0) + 1"));
            nowLiked = true;
        }
        PostComment fresh = postCommentMapper.selectById(commentId);
        return Result.success(Map.of(
                "liked", nowLiked,
                "likeCount", fresh != null && fresh.getLikeCount() != null ? fresh.getLikeCount() : 0));
    }

    private Map<String, Object> postWithAuthor(Post post) {
        User u = userMapper.selectById(post.getUserId());
        String nickname = u != null && u.getNickname() != null ? u.getNickname() : "用户";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("post", post);
        m.put("authorNickname", nickname);
        return m;
    }

    private Map<Long, String> nicknames(Set<Long> ids) {
        if (ids.isEmpty())
            return Map.of();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, ids));
        Map<Long, String> m = new HashMap<>();
        for (User u : users) {
            m.put(u.getId(), u.getNickname() != null ? u.getNickname() : "用户");
        }
        return m;
    }

    private Map<String, Object> commentRow(PostComment c, String nickname, boolean liked) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("postId", c.getPostId());
        m.put("userId", c.getUserId());
        m.put("parentId", c.getParentId());
        m.put("content", c.getContent());
        m.put("likeCount", c.getLikeCount() != null ? c.getLikeCount() : 0);
        m.put("createTime", c.getCreateTime());
        m.put("nickname", nickname);
        m.put("liked", liked);
        return m;
    }

    @Data
    public static class CreateReq {
        private String title;
        private String content;
    }

    @Data
    public static class CommentReq {
        private String content;
        private Long parentId;
    }
}
