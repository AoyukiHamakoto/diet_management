package com.diet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diet.entity.UserPreference;

import java.util.Map;

public interface IUserPreferenceService extends IService<UserPreference> {

    UserPreference getOrCreate(Long userId);

    void reset(Long userId);

    double getCalorieMultiplier(Long userId);

    Map<String, Double> getTagWeights(Long userId);
}
