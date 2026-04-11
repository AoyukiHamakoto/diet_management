package com.diet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.DietTag;
import com.diet.mapper.DietTagMapper;
import com.diet.service.IDietTagService;
import org.springframework.stereotype.Service;

@Service
public class DietTagServiceImpl extends ServiceImpl<DietTagMapper, DietTag> implements IDietTagService {
}
