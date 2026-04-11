package com.diet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.BodyLog;
import com.diet.mapper.BodyLogMapper;
import com.diet.service.IBodyLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BodyLogServiceImpl extends ServiceImpl<BodyLogMapper, BodyLog> implements IBodyLogService {

    @Override
    public List<Map<String, Object>> getChartData(Long userId, int days) {
        LocalDate start = LocalDate.now().minusDays(days);
        List<BodyLog> logs = lambdaQuery()
                .eq(BodyLog::getUserId, userId)
                .ge(BodyLog::getLogDate, start)
                .orderByAsc(BodyLog::getLogDate)
                .list();
        return logs.stream()
                .map(log -> Map.<String, Object>of(
                        "date", log.getLogDate().toString(),
                        "weight", log.getWeight().doubleValue()
                ))
                .collect(Collectors.toList());
    }
}
