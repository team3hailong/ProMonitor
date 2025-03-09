package com.promonitor.model.interfaces;

import java.time.LocalDateTime;
import java.util.Map;

public interface IReportable {
    Map<String, Object> generateReportData();
    LocalDateTime getReportStartTime();
    LocalDateTime getReportEndTime();
}