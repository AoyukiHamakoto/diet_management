package com.diet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.Result;
import com.diet.entity.OrderRecord;
import com.diet.mapper.OrderRecordMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@Tag(name = "管理员订单管理接口")
public class AdminOrderController {

    private final OrderRecordMapper orderRecordMapper;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<OrderRecord> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(OrderRecord::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(OrderRecord::getOrderNo, keyword).or().like(OrderRecord::getItemName, keyword));
        }
        qw.orderByDesc(OrderRecord::getCreateTime);
        Page<OrderRecord> p = orderRecordMapper.selectPage(new Page<>(page, size), qw);
        return Result.success(Map.of(
                "records", p.getRecords(),
                "total", p.getTotal(),
                "pages", p.getPages(),
                "current", p.getCurrent()
        ));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusReq req) {
        if (req == null || req.getStatus() == null || req.getStatus().isBlank()) {
            return Result.error(400, "status is required");
        }
        OrderRecord rec = orderRecordMapper.selectById(id);
        if (rec == null) {
            return Result.error(404, "Order not found");
        }
        rec.setStatus(req.getStatus().trim().toUpperCase());
        orderRecordMapper.updateById(rec);
        return Result.success();
    }

    @Data
    public static class UpdateStatusReq {
        private String status;
    }
}

