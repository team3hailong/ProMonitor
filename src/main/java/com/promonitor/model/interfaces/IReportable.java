package com.promonitor.model.interfaces;

import java.util.Map;

public interface IReportable {
    Map<String, Object> generateReportData();
}