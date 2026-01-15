package com.px.ifp.spc.util;

import cn.hutool.core.util.StrUtil;
import com.px.ifp.common.utils.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @Author: zhangzhiqiang
 * @Date: 2024-07-30
 */
public class DateExtUtils {
    public static Date parseDate(String strDate){
        if (StrUtil.isBlank(strDate)){
            return null;
        }
        Date date = DateUtils.parse(strDate, DateUtils.DatePatternEnum.YYYY_MM_DD);
        if (date != null){
            return date;
        }
        date = DateUtils.parse(strDate, DateUtils.DatePatternEnum.YYYY_MM_DD_02);
        if (date != null){
            return date;
        }
        return DateUtils.parse(strDate, DateUtils.DatePatternEnum.YYYYMMDD);
    }

    public static Date getStartTimeOfMonth(Date day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(day);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date getEndTimeOfMonth(Date day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(day);
        calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    public static Date getStartTimeOfYear(Date day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(day);
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date getEndTimeOfYear(Date day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(day);
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DATE, 31);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    public static Date getStartTimeOfMinute(Date day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(day);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static int getDaysInMonth(Date day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(day);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static Date getStartTimeOfHour(Date day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(day);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date parseDate(String strDate, DateUtils.DatePatternEnum format){
        return DateUtils.parse(strDate, format);
    }

    public static String parseString(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    public static Date startOfToday(Date date) {
        String nowDate = parseString(date, DateUtils.DatePatternEnum.YYYY_MM_DD.getPattern());
        return parseDate(nowDate + " 00:00:00");
    }

    public static Date hour24Before(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
        return calendar.getTime();
    }


    public static Date hour24After(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
        return calendar.getTime();
    }

    public static Date getStartTimeOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date getStartTimeOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date getEndTimeOfDay(Date day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(day);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    public static Long getTimestampOfDay(Date day) {
        String timestamp = parseString(day, DateUtils.DatePatternEnum.YYYY_MM_DD.getPattern());
        return parseDate(timestamp).getTime();
    }

    /**
     * 根据周期类型计算同步起始时间
     */
    public static Date getStartTime(Date date,String periodType, int interval) {
        if (periodType == null || periodType.isEmpty()) {
            throw new IllegalArgumentException("Period type cannot be null or empty");
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        switch (periodType.toLowerCase()) {
            case "day":
                calendar.add(Calendar.DAY_OF_YEAR, -interval);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case "hour":
                calendar.add(Calendar.HOUR_OF_DAY, -interval);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case "week":
                calendar.add(Calendar.WEEK_OF_YEAR, -interval);
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case "month":
                calendar.add(Calendar.MONTH, -interval);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case "minute":
                calendar.add(Calendar.MINUTE, -interval);
                break;
            default:
                throw new IllegalArgumentException("Unsupported period type: " + periodType);
        }

        return calendar.getTime();
    }

    /**
     * 根据周期类型计算同步结束时间
     */
    public static Date getEndTime(Date date,String periodType, int interval) {
        if (periodType == null || periodType.isEmpty()) {
            throw new IllegalArgumentException("Period type cannot be null or empty");
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        switch (periodType.toLowerCase()) {
            case "day":
                calendar.add(Calendar.DAY_OF_YEAR, interval);
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                break;
            case "hour":
                calendar.add(Calendar.HOUR_OF_DAY, interval);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                break;
            case "week":
                calendar.add(Calendar.WEEK_OF_YEAR, interval);
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                break;
            case "month":
                calendar.add(Calendar.MONTH, interval);
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                break;
            case "minute":
                calendar.add(Calendar.MINUTE, interval);
                break;
            default:
                throw new IllegalArgumentException("Unsupported period type: " + periodType);
        }

        return calendar.getTime();
    }

    /**
     * 比较两个日期的大小，按天或按小时
     *
     * @param now 当前时间
     * @param compareDate 要比较的时间
     * @param periodType 比较类型 ("day" 或 "hour")
     * @return 正数表示now大于compareDate，负数表示now小于compareDate，0表示相等
     */
    public static int compareDates(Date now, Date compareDate, String periodType) {
        if (periodType == null || periodType.isEmpty()) {
            throw new IllegalArgumentException("Period type cannot be null or empty");
        }

        Calendar calendarNow = Calendar.getInstance();
        calendarNow.setTime(now);

        Calendar calendarCompare = Calendar.getInstance();
        calendarCompare.setTime(compareDate);

        switch (periodType.toLowerCase()) {
            case "day":
                // 只比较年月日
                calendarNow.set(Calendar.HOUR_OF_DAY, 0);
                calendarNow.set(Calendar.MINUTE, 0);
                calendarNow.set(Calendar.SECOND, 0);
                calendarNow.set(Calendar.MILLISECOND, 0);

                calendarCompare.set(Calendar.HOUR_OF_DAY, 0);
                calendarCompare.set(Calendar.MINUTE, 0);
                calendarCompare.set(Calendar.SECOND, 0);
                calendarCompare.set(Calendar.MILLISECOND, 0);
                break;

            case "hour":
                // 只比较年月日和小时
                calendarNow.set(Calendar.MINUTE, 0);
                calendarNow.set(Calendar.SECOND, 0);
                calendarNow.set(Calendar.MILLISECOND, 0);

                calendarCompare.set(Calendar.MINUTE, 0);
                calendarCompare.set(Calendar.SECOND, 0);
                calendarCompare.set(Calendar.MILLISECOND, 0);
                break;

            default:
                throw new IllegalArgumentException("Unsupported period type: " + periodType);
        }

        return calendarNow.compareTo(calendarCompare);
    }


    /**
     * 计算按天或按小时间隔n个整数后的时间
     * @param startDate 起始时间
     * @param interval 数量，正数表示往后计算，负数表示往前计算
     * @param unit 单位，可以是 Calendar.DATE 或 Calendar.HOUR
     * @return 计算后的时间
     */
    public static Date calculateDate(Date startDate, int interval, int unit) {
        // 创建一个 Calendar 实例并设置起始时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        // 根据单位和间隔计算新的时间
        calendar.add(unit, interval);

        // 返回计算后的时间
        return calendar.getTime();
    }

    /**
     * 将秒数转换为中文格式（xx天xx小时xx分钟xx秒）
     * @param seconds 秒数
     * @return 格式化后的字符串
     */
    public static String secondsToChineseFormat(long seconds) {
        if (seconds <= 0) {
            return "0秒";
        }

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        List<String> parts = new ArrayList<>();

        if (days > 0) {
            parts.add(days + "天");
        }
        if (hours > 0) {
            parts.add(hours + "小时");
        }
        if (minutes > 0) {
            parts.add(minutes + "分钟");
        }
        if (secs > 0 || parts.isEmpty()) { // 如果所有单位都是0，至少显示0秒
            parts.add(secs + "秒");
        }

        return String.join("", parts);
    }


    // 时间对齐到指定粒度（分钟、小时、天等）
    public static Date alignTime(Date time, int calendarField) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);

        switch (calendarField) {
            case Calendar.MINUTE:
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case Calendar.HOUR:
            case Calendar.HOUR_OF_DAY:
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case Calendar.DAY_OF_MONTH:
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case Calendar.WEEK_OF_YEAR:
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case Calendar.MONTH:
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            default:
                throw new IllegalArgumentException("Unsupported calendar field");
        }

        return calendar.getTime();
    }

    public static void main(String[] args) {
//        2024-12-22 07:55:22
        String dateStr = "2024-12-22 07:55:22";
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date =sf.parse(dateStr);
            Date alignTime = DateExtUtils.alignTime(date, Calendar.MINUTE);
            System.out.println(alignTime);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }
}
