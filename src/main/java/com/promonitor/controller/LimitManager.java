package com.promonitor.controller;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.promonitor.model.Application;
import com.promonitor.model.ApplicationGroup;
import com.promonitor.model.Limit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LimitManager {
    private static final Logger logger = LoggerFactory.getLogger(LimitManager.class);

    private final Map<Object, Limit> limits;

    public LimitManager() {
        this.limits = new HashMap<>();
    }

    public void setLimit(Application application, Limit limit) {
        limits.put(application, limit);
        logger.debug("Đã đặt giới hạn cho ứng dụng {}: {}", application.getName(), limit);
    }

    public void setLimit(ApplicationGroup group, Limit limit) {
        limits.put(group, limit);
        logger.debug("Đã đặt giới hạn cho nhóm {}: {}", group.getName(), limit);
    }

    public void removeLimit(Application application) {
        boolean removed = limits.remove(application) != null;
        if (removed) {
            logger.debug("Đã xóa giới hạn cho ứng dụng: {}", application.getName());
        }
    }

    public void removeLimit(ApplicationGroup group) {
        boolean removed = limits.remove(group) != null;
        if (removed) {
            logger.debug("Đã xóa giới hạn cho nhóm: {}", group.getName());
        }
    }

    public boolean isLimitExceeded(Application application, Duration usageTime) {
        Limit limit = getLimit(application);
        
        if (limit != null) {
            if (limit.needsReset()) {
                limit.reset();
                logger.debug("Đã reset giới hạn cho ứng dụng: {}", application.getName());
                return false;
            }
            return limit.isExceeded(usageTime);
        }
        return false;
    }

    public boolean isGroupLimitExceeded(Application application, Map<ApplicationGroup, Duration> usageMap) {
        for (Map.Entry<Object, Limit> entry : limits.entrySet()) {
            if (entry.getKey() instanceof ApplicationGroup group) {
                if (group.containsApplication(application)) {
                    Limit limit = entry.getValue();
                    Duration groupUsage = usageMap.getOrDefault(group, Duration.ZERO);

                    if (limit.needsReset()) {
                        limit.reset();
                        logger.debug("Đã reset giới hạn cho nhóm: {}", group.getName());
                        continue;
                    }

                    if (limit.isExceeded(groupUsage)) {
                        logger.debug("Ứng dụng {} thuộc nhóm {} đã vượt quá giới hạn",
                                application.getName(), group.getName());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Limit getLimit(Application application) {
        Limit limit = limits.get(application);
        if (limit == null) {
            for (Map.Entry<Object, Limit> entry : limits.entrySet()) {
                if (entry.getKey() instanceof Application app && 
                    app.getName().equals(application.getName())) {
                    limit = entry.getValue();
                    break;
                }
            }
        }
        
        return limit;
    }

    public Limit getLimit(ApplicationGroup group) {
        return limits.get(group);
    }

    public Map<Object, Limit> getAllLimits() {
        return new HashMap<>(limits);
    }
}