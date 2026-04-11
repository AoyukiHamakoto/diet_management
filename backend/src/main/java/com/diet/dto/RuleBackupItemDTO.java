package com.diet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleBackupItemDTO {
    /** Backup filename, e.g. diet-label-rules.drl.bak.20250101120000 */
    private String filename;
    private String timestamp;
}
