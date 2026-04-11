package com.diet.controller;

import com.diet.common.Result;
import com.diet.dto.RuleBackupItemDTO;
import com.diet.dto.RuleContentDTO;
import com.diet.dto.RuleFileDTO;
import com.diet.dto.RuleValidateDTO;
import com.diet.service.RuleFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/rules")
@RequiredArgsConstructor
@Tag(name = "管理员-规则引擎")
public class AdminRulesController {

    private final RuleFileService ruleFileService;

    @GetMapping
    @Operation(summary = "规则文件列表")
    public Result<List<RuleFileDTO>> list() {
        return Result.success(ruleFileService.list());
    }

    @GetMapping("/content")
    @Operation(summary = "获取规则文件内容")
    public Result<RuleContentDTO> getContent(@RequestParam String name) {
        try {
            return Result.success(ruleFileService.getContent(name));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "读取失败: " + e.getMessage());
        }
    }

    @PutMapping
    @Operation(summary = "保存规则并发布")
    public Result<Void> save(@RequestBody RuleContentDTO body) {
        if (body == null || body.getName() == null) {
            return Result.error(400, "缺少 name");
        }
        try {
            ruleFileService.save(body.getName(), body.getContent());
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "保存失败: " + e.getMessage());
        }
    }

    @PostMapping("/validate")
    @Operation(summary = "校验规则内容（不写文件）")
    public Result<RuleValidateDTO> validate(@RequestBody RuleContentDTO body) {
        if (body == null || body.getName() == null) {
            return Result.error(400, "缺少 name");
        }
        RuleValidateDTO dto = ruleFileService.validate(body.getName(), body.getContent());
        return Result.success(dto);
    }

    @GetMapping("/backups")
    @Operation(summary = "备份列表")
    public Result<List<RuleBackupItemDTO>> listBackups(@RequestParam String name) {
        try {
            return Result.success(ruleFileService.listBackups(name));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @PostMapping("/restore")
    @Operation(summary = "从备份恢复")
    public Result<Void> restore(@RequestBody RestoreRequest body) {
        if (body == null || body.getName() == null || body.getBackupFilename() == null) {
            return Result.error(400, "缺少 name 或 backupFilename");
        }
        try {
            ruleFileService.restore(body.getName(), body.getBackupFilename());
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "恢复失败: " + e.getMessage());
        }
    }

    @Data
    public static class RestoreRequest {
        private String name;
        private String backupFilename;
    }
}
