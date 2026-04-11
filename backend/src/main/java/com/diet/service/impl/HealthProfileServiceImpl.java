package com.diet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.HealthProfile;
import com.diet.mapper.HealthProfileMapper;
import com.diet.service.IHealthProfileService;
import org.springframework.stereotype.Service;

@Service
public class HealthProfileServiceImpl extends ServiceImpl<HealthProfileMapper, HealthProfile> implements IHealthProfileService {
}
