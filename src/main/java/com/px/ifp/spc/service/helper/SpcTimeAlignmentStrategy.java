package com.px.ifp.spc.service.helper;

import java.time.*;
import java.util.Date;

/**
 * 时间对齐策略枚举
 * 支持多种时间粒度的对齐: 秒、分钟、小时、天、周、月、年
 */
public enum SpcTimeAlignmentStrategy {

    /**
     * 秒级对齐 (去掉毫秒)
     * 示例: 10:05:03.234 → 10:05:03.000
     */
    SECOND {
        @Override
        public long align(long timestamp) {
            return timestamp / 1000 * 1000;
        }

        @Override
        public String getDescription() {
            return "秒级对齐";
        }
    },

    /**
     * 分钟级对齐 (去掉秒和毫秒)
     * 示例: 10:05:03.234 → 10:05:00.000
     */
    MINUTE {
        @Override
        public long align(long timestamp) {
            return timestamp / 60000 * 60000;
        }

        @Override
        public String getDescription() {
            return "分钟级对齐";
        }
    },

    /**
     * 小时级对齐 (对齐到整点)
     * 示例: 10:05:03.234 → 10:00:00.000
     */
    HOUR {
        @Override
        public long align(long timestamp) {
            return timestamp / 3600000 * 3600000;
        }

        @Override
        public String getDescription() {
            return "小时级对齐";
        }
    },

    /**
     * 天级对齐 (对齐到当天0点,基于系统时区)
     * 示例: 2024-10-19 10:05:03 → 2024-10-19 00:00:00
     */
    DAY {
        @Override
        public long align(long timestamp) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );

            LocalDateTime aligned = dateTime.toLocalDate().atStartOfDay();

            return aligned.atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        }

        @Override
        public String getDescription() {
            return "天级对齐(0点)";
        }
    },

    /**
     * 周级对齐 (对齐到本周一0点,基于系统时区)
     * 示例: 2024-10-19 (周六) 10:05:03 → 2024-10-14 (周一) 00:00:00
     */
    WEEK {
        @Override
        public long align(long timestamp) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );

            // 对齐到本周一
            LocalDate mondayOfWeek = dateTime.toLocalDate()
                    .with(DayOfWeek.MONDAY);

            LocalDateTime aligned = mondayOfWeek.atStartOfDay();

            return aligned.atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        }

        @Override
        public String getDescription() {
            return "周级对齐(周一0点)";
        }
    },

    /**
     * 月级对齐 (对齐到本月1号0点,基于系统时区)
     * 示例: 2024-10-19 10:05:03 → 2024-10-01 00:00:00
     */
    MONTH {
        @Override
        public long align(long timestamp) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );

            // 对齐到本月1号
            LocalDateTime aligned = dateTime.toLocalDate()
                    .withDayOfMonth(1)
                    .atStartOfDay();

            return aligned.atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        }

        @Override
        public String getDescription() {
            return "月级对齐(1号0点)";
        }
    },

    /**
     * 年级对齐 (对齐到本年1月1号0点,基于系统时区)
     * 示例: 2024-10-19 10:05:03 → 2024-01-01 00:00:00
     */
    YEAR {
        @Override
        public long align(long timestamp) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );

            // 对齐到本年1月1日
            LocalDateTime aligned = dateTime.toLocalDate()
                    .withDayOfYear(1)
                    .atStartOfDay();

            return aligned.atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        }

        @Override
        public String getDescription() {
            return "年级对齐(1月1日0点)";
        }
    };

    /**
     * 对齐时间戳
     *
     * @param timestamp 原始时间戳(毫秒)
     * @return 对齐后的时间戳(毫秒)
     */
    public abstract long align(long timestamp);

    /**
     * 获取对齐策略描述
     */
    public abstract String getDescription();

    /**
     * 对齐Date对象
     *
     * @param date 原始日期
     * @return 对齐后的日期
     */
    public Date align(Date date) {
        if (date == null) {
            return null;
        }
        return new Date(align(date.getTime()));
    }

    /**
     * 根据采样间隔自动选择对齐策略
     *
     * @param samplingIntervalMs 采样间隔(毫秒)
     * @return 推荐的对齐策略
     */
    public static SpcTimeAlignmentStrategy autoSelect(long samplingIntervalMs) {
        if (samplingIntervalMs < 10000) {
            // < 10秒: 秒级对齐
            return SECOND;
        } else if (samplingIntervalMs < 600000) {
            // < 10分钟: 分钟级对齐
            return MINUTE;
        } else if (samplingIntervalMs < 3600000 * 6) {
            // < 6小时: 小时级对齐
            return HOUR;
        } else if (samplingIntervalMs < 86400000 * 7) {
            // < 7天: 天级对齐
            return DAY;
        } else if (samplingIntervalMs < 86400000 * 30) {
            // < 30天: 周级对齐
            return WEEK;
        } else if (samplingIntervalMs < 86400000 * 365) {
            // < 365天: 月级对齐
            return MONTH;
        } else {
            // >= 365天: 年级对齐
            return YEAR;
        }
    }

    /**
     * 检测数据的采样间隔(取中位数)
     *
     * @param timestamps 时间戳列表(至少2个)
     * @return 采样间隔(毫秒), 如果数据不足返回-1
     */
    public static long detectSamplingInterval(long[] timestamps) {
        if (timestamps == null || timestamps.length < 2) {
            return -1;
        }

        // 计算相邻时间戳的间隔
        int sampleSize = Math.min(100, timestamps.length - 1);
        long[] intervals = new long[sampleSize];

        for (int i = 0; i < sampleSize; i++) {
            intervals[i] = timestamps[i + 1] - timestamps[i];
        }

        // 排序取中位数
        java.util.Arrays.sort(intervals);
        return intervals[sampleSize / 2];
    }
}
