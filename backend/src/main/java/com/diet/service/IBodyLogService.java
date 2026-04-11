package com.diet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diet.entity.BodyLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IBodyLogService extends IService<BodyLog> {

    List<Map<String, Object>> getChartData(Long userId, int days);
}
