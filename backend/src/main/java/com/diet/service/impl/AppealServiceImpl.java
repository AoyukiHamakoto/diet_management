package com.diet.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.Appeal;
import com.diet.entity.MealPlan;
import com.diet.entity.Post;
import com.diet.entity.Recipe;
import com.diet.mapper.AppealMapper;
import com.diet.mapper.PostMapper;
import com.diet.service.IAppealService;
import com.diet.service.IMealPlanService;
import com.diet.service.IRecipeService;
import com.diet.service.ISystemNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppealServiceImpl extends ServiceImpl<AppealMapper, Appeal> implements IAppealService {

    private final IMealPlanService mealPlanService;
    private final IRecipeService recipeService;
    private final PostMapper postMapper;
    private final ISystemNotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Appeal submit(Long userId, String targetType, Long targetId, String reason) {
        Appeal.TargetType type = parseTargetType(targetType);
        validateTargetForAppeal(userId, type, targetId);
        if (hasPendingAppeal(targetType, targetId)) {
            throw new IllegalArgumentException("该对象已有未处理的申诉");
        }
        Appeal appeal = new Appeal();
        appeal.setUserId(userId);
        appeal.setTargetType(type);
        appeal.setTargetId(targetId);
        appeal.setReason(reason);
        appeal.setStatus(Appeal.Status.PENDING);
        save(appeal);
        notificationService.notify(userId, "申诉已提交，请等待处理");
        return appeal;
    }

    @Override
    public boolean hasPendingAppeal(String targetType, Long targetId) {
        return lambdaQuery()
                .eq(Appeal::getTargetType, Appeal.TargetType.valueOf(targetType))
                .eq(Appeal::getTargetId, targetId)
                .in(Appeal::getStatus, Appeal.Status.PENDING, Appeal.Status.PROCESSING)
                .exists();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolve(Long appealId, Long adminId, String adminComment) {
        Appeal appeal = getById(appealId);
        if (appeal == null)
            throw new IllegalArgumentException("申诉不存在");
        if (appeal.getStatus() != Appeal.Status.PENDING && appeal.getStatus() != Appeal.Status.PROCESSING) {
            throw new IllegalArgumentException("该申诉已处理");
        }
        appeal.setStatus(Appeal.Status.RESOLVED);
        appeal.setAdminId(adminId);
        appeal.setAdminComment(adminComment);
        updateById(appeal);

        if (appeal.getTargetType() == Appeal.TargetType.MEAL_PLAN) {
            MealPlan plan = mealPlanService.getById(appeal.getTargetId());
            if (plan != null && plan.getAuditStatus() == MealPlan.AuditStatus.REJECTED) {
                plan.setAuditStatus(MealPlan.AuditStatus.APPROVED);
                plan.setAuditComment(null);
                mealPlanService.updateById(plan);
            }
        } else {
            if (appeal.getTargetType() == Appeal.TargetType.RECIPE) {
                Recipe recipe = recipeService.getById(appeal.getTargetId());
                if (recipe != null && recipe.getStatus() == Recipe.RecipeStatus.REJECTED) {
                    recipe.setStatus(Recipe.RecipeStatus.APPROVED);
                    recipe.setRejectReason(null);
                    recipeService.updateById(recipe);
                }
            } else if (appeal.getTargetType() == Appeal.TargetType.POST) {
                Post post = postMapper.selectById(appeal.getTargetId());
                if (post != null && "REJECTED".equals(post.getStatus())) {
                    post.setStatus("APPROVED");
                    post.setRejectReason(null);
                    postMapper.updateById(post);
                }
            }
        }
        notificationService.notify(appeal.getUserId(),
                "您的申诉已通过，相关" + (appeal.getTargetType() == Appeal.TargetType.MEAL_PLAN ? "计划" : "菜谱") + "已恢复。管理员意见："
                        + (adminComment != null ? adminComment : "无"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long appealId, Long adminId, String adminComment) {
        if (adminComment == null || adminComment.isBlank()) {
            throw new IllegalArgumentException("拒绝申诉必须填写处理意见");
        }
        Appeal appeal = getById(appealId);
        if (appeal == null)
            throw new IllegalArgumentException("申诉不存在");
        if (appeal.getStatus() != Appeal.Status.PENDING && appeal.getStatus() != Appeal.Status.PROCESSING) {
            throw new IllegalArgumentException("该申诉已处理");
        }
        appeal.setStatus(Appeal.Status.REJECTED);
        appeal.setAdminId(adminId);
        appeal.setAdminComment(adminComment);
        updateById(appeal);
        notificationService.notify(appeal.getUserId(), "您的申诉未通过。管理员意见：" + adminComment);
    }

    private static Appeal.TargetType parseTargetType(String targetType) {
        try {
            return Appeal.TargetType.valueOf(targetType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("targetType 必须为 MEAL_PLAN、RECIPE 或 POST");
        }
    }

    private void validateTargetForAppeal(Long userId, Appeal.TargetType type, Long targetId) {
        if (type == Appeal.TargetType.MEAL_PLAN) {
            MealPlan plan = mealPlanService.getById(targetId);
            if (plan == null)
                throw new IllegalArgumentException("计划不存在");
            if (!plan.getUserId().equals(userId))
                throw new IllegalArgumentException("只能对自己的计划申诉");
            if (plan.getAuditStatus() != MealPlan.AuditStatus.REJECTED) {
                throw new IllegalArgumentException("只能对被拒绝的计划提交申诉");
            }
        } else if (type == Appeal.TargetType.RECIPE) {
            Recipe recipe = recipeService.getById(targetId);
            if (recipe == null)
                throw new IllegalArgumentException("菜谱不存在");
            if (!recipe.getCreatorId().equals(userId))
                throw new IllegalArgumentException("只能对自己的菜谱申诉");
            if (recipe.getStatus() != Recipe.RecipeStatus.REJECTED) {
                throw new IllegalArgumentException("只能对被拒绝的菜谱提交申诉");
            }
        } else {
            Post post = postMapper.selectById(targetId);
            if (post == null || Integer.valueOf(1).equals(post.getDeleted()))
                throw new IllegalArgumentException("帖子不存在");
            if (!post.getUserId().equals(userId))
                throw new IllegalArgumentException("只能对自己的帖子申诉");
            if (!"REJECTED".equals(post.getStatus())) {
                throw new IllegalArgumentException("只能对被拒绝的帖子提交申诉");
            }
        }
    }
}
