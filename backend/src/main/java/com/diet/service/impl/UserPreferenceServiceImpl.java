package com.diet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.UserPreference;
import com.diet.mapper.UserPreferenceMapper;
import com.diet.service.IUserPreferenceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@Service
public class UserPreferenceServiceImpl extends ServiceImpl<UserPreferenceMapper, UserPreference> implements IUserPreferenceService {

    @Override
    public UserPreference getOrCreate(Long userId) {
        UserPreference p = lambdaQuery().eq(UserPreference::getUserId, userId).one();
        if (p == null) {
            p = new UserPreference();
            p.setUserId(userId);
            p.setCalorieMultiplier(BigDecimal.ONE);
            p.setTagWeights(Collections.emptyMap());
            p.setAdjustmentCount(0);
            p.setLearningProgress(0);
            save(p);
        }
        return p;
    }

    @Override
    public void reset(Long userId) {
        UserPreference p = getOrCreate(userId);
        p.setCalorieMultiplier(BigDecimal.ONE);
        p.setTagWeights(Collections.emptyMap());
        p.setAdjustmentCount(0);
        p.setLearningProgress(0);
        updateById(p);
    }

    @Override
    public double getCalorieMultiplier(Long userId) {
        UserPreference p = lambdaQuery().eq(UserPreference::getUserId, userId).one();
        return p != null && p.getCalorieMultiplier() != null
                ? p.getCalorieMultiplier().doubleValue() : 1.0;
    }

    @Override
    public Map<String, Double> getTagWeights(Long userId) {
        UserPreference p = lambdaQuery().eq(UserPreference::getUserId, userId).one();
        return p != null && p.getTagWeights() != null ? p.getTagWeights() : Collections.emptyMap();
    }
}
