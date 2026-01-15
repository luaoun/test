package com.px.ifp.spc.util;

import com.px.ifp.common.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class OperationDateUtils {


    private static final ThreadLocal<SimpleDateFormat> HOUR_MINUTE_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("HH:mm")
    );

    private static final ThreadLocal<SimpleDateFormat> MONTH_DAY_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("MM/dd")
    );

    private static final ThreadLocal<SimpleDateFormat> M_D_HH_MM_FORMAT = ThreadLocal.withInitial(
            //() -> new SimpleDateFormat("M.d HH:mm")
            () -> new SimpleDateFormat("yyyy/MM/dd HH:mm")
    );

    private static final ThreadLocal<SimpleDateFormat> DEFAULT_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat(DateUtils.DatePatternEnum.DEFAULT_PATTERN.getPattern())
    );

    private static final ThreadLocal<SimpleDateFormat> YYYY_MM_DD_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat(DateUtils.DatePatternEnum.YYYY_MM_DD.getPattern())
    );

    private static final ThreadLocal<SimpleDateFormat> YYYY_MM_DD_HH_MM_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat(DateUtils.DatePatternEnum.YYYY_MM_DD_HH_MM.getPattern())
    );
    private static final ThreadLocal<SimpleDateFormat> YYYY_MM_DD_HH_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH")
    );

    private static final ThreadLocal<SimpleDateFormat> YYYY_M_D_SLASH_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy/M/d")
    );

    private static final ThreadLocal<SimpleDateFormat> YYYY_MM_DD_SLASH_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy/MM/dd")
    );

    private static final ThreadLocal<SimpleDateFormat> YYYY_MM_SLASH_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy/MM")
    );

    private static final ThreadLocal<SimpleDateFormat> YYYY_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy")
    );

    private static final ThreadLocal<SimpleDateFormat> YYYY_M_STR_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy年M月")
    );

    private static final ThreadLocal<SimpleDateFormat> YYYY_M_D_STR_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy年M月d日")
    );

    private static final ThreadLocal<SimpleDateFormat> YYYY_STR_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy年")
    );

    public static String formatHourMinute(Date date) {
        if (date == null){
            return null;
        }
        return HOUR_MINUTE_FORMAT.get().format(date);
    }

    public static String formatMonthDay(Date date){
        if (date == null){
            return null;
        }
        return MONTH_DAY_FORMAT.get().format(date);
    }

    public static String formatMDHHMM(Date date){
        if (date == null){
            return null;
        }
        return M_D_HH_MM_FORMAT.get().format(date);
    }

    public static String format(Date date) {
        if (date == null){
            return null;
        }
        return DEFAULT_FORMAT.get().format(date);
    }


    public static String formatYYYYMMDD(Date date) {

        if (date == null){
            return null;
        }
        return YYYY_MM_DD_FORMAT.get().format(date);
    }

    public static String formatYYYYMMDDHH(Date date) {

        if (date == null){
            return null;
        }
        return YYYY_MM_DD_HH_FORMAT.get().format(date);
    }

    public static SimpleDateFormat format() {
        return DEFAULT_FORMAT.get();
    }

    public static SimpleDateFormat formatYYYYMMDDHHMM() {
        return YYYY_MM_DD_HH_MM_FORMAT.get();
    }

    public static SimpleDateFormat formatYYYYMMDD() {
        return YYYY_MM_DD_FORMAT.get();
    }

    public static SimpleDateFormat formatYYYYMDSLASH() {
        return YYYY_M_D_SLASH_FORMAT.get();
    }

    public static SimpleDateFormat formatYYYYMMDDSLASH() {
        return YYYY_MM_DD_SLASH_FORMAT.get();
    }

    public static SimpleDateFormat formatYYYYMMSLASH() {
        return YYYY_MM_SLASH_FORMAT.get();
    }


    public static SimpleDateFormat formatYYYY() {
        return YYYY_FORMAT.get();
    }

    public static SimpleDateFormat formatYYYYMStr() {
        return YYYY_M_STR_FORMAT.get();
    }

    public static SimpleDateFormat formatYYYYMDStr() {
        return YYYY_M_D_STR_FORMAT.get();
    }

    public static SimpleDateFormat formatYYYYStr() {
        return YYYY_STR_FORMAT.get();
    }


    public static SimpleDateFormat formatMonthDay() {
        return MONTH_DAY_FORMAT.get();
    }

    public  static void remove(){
        HOUR_MINUTE_FORMAT.remove();
        DEFAULT_FORMAT.remove();
        MONTH_DAY_FORMAT.remove();
        YYYY_MM_DD_FORMAT.remove();
        YYYY_MM_DD_HH_MM_FORMAT.remove();
        YYYY_M_D_SLASH_FORMAT.remove();
        YYYY_MM_DD_SLASH_FORMAT.remove();
        YYYY_MM_SLASH_FORMAT.remove();
        YYYY_M_STR_FORMAT.remove();
        YYYY_M_D_STR_FORMAT.remove();
        M_D_HH_MM_FORMAT.remove();
        YYYY_STR_FORMAT.remove();
        YYYY_FORMAT.remove();
    }

    public static Integer getWeekOfYear(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }

}
