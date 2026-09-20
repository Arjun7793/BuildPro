package com.example.buildpro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// Backs the admin dashboard's lead counters (see AdminStatsController /
// static/admin/index.html) - a handful of counts, not the leads themselves.
@Data
@AllArgsConstructor
public class LeadStats {
    private long total;
    private long today;
    private long thisWeek;
}
