package com.px.ifp.spc.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Objects;

public class BigDecimalUtils {


    public static final String PERCENT_DELIMITER = "%";

    public static final BigDecimal HUNDRED = new BigDecimal("100");

    public static final BigDecimal THOUSAND = new BigDecimal("1000");

    private static final ThreadLocal<DecimalFormat> INTEGER_FORMAT = ThreadLocal.withInitial(
            () -> new DecimalFormat("#,##0")
    );

    private static final ThreadLocal<DecimalFormat> TWO_DECIMAL_FORMAT = ThreadLocal.withInitial(
            () -> new DecimalFormat("#,##0.##")
    );

    public static String formatInteger(BigDecimal value) {
        if (value == null){
            return INTEGER_FORMAT.get().format(BigDecimal.ZERO);
        }
        return INTEGER_FORMAT.get().format(value);
    }

    public static String formatTwoDecimal(BigDecimal value) {
        if (value == null){
            return TWO_DECIMAL_FORMAT.get().format(BigDecimal.ZERO);
        }
        return TWO_DECIMAL_FORMAT.get().format(value);
    }


    // 除法
    public static BigDecimal divide(BigDecimal a, BigDecimal b, int scale, RoundingMode roundingMode) {
        if (b.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return a.divide(b, scale, roundingMode);
    }

    //除以1000
    public static BigDecimal divideByThousand(BigDecimal value){
        if (value == null){
            return BigDecimal.ZERO;
        }

        return value.divide(THOUSAND,2,RoundingMode.HALF_UP);
    }

    public  static void remove(){
        INTEGER_FORMAT.remove();
        TWO_DECIMAL_FORMAT.remove();
    }


    public static BigDecimal toBigDecimalNullZero(Object object) {
        if(Objects.isNull(object)){
            return new BigDecimal(0);
        }
        if (object instanceof BigDecimal) {
            return (BigDecimal)object;
        } else if (object instanceof String) {
            return new BigDecimal((String)object);
        } else if (object instanceof Integer) {
            return new BigDecimal((Integer)object);
        } else if (object instanceof Long) {
            return new BigDecimal((Long)object);
        } else if (object instanceof Double) {
            return BigDecimal.valueOf((Double)object);
        } else if (object instanceof Float) {
            return BigDecimal.valueOf((double)(Float)object);
        } else {
            throw new IllegalArgumentException("Object cannot be converted to BigDecimal");
        }
    }

    public static void addValueMap(Map<String, BigDecimal> linkMap, String key, BigDecimal value) {
        if (Objects.isNull(value)) {
            return;
        }
        if (linkMap.containsKey(key)) {
            linkMap.put(key, linkMap.get(key).add(value));
        } else {
            linkMap.put(key, value);
        }
    }

    public static void main(String[] args) {

        DecimalFormat DF_NUMBER = new DecimalFormat("#,##0.##");
        System.out.println(DF_NUMBER.format(new BigDecimal("0.000")));
        System.out.println(DF_NUMBER.format(new BigDecimal("0.001")));
        System.out.println(DF_NUMBER.format(new BigDecimal("1000000000.000")));
        System.out.println(DF_NUMBER.format(new BigDecimal("1000000000.1")));
        System.out.println(DF_NUMBER.format(new BigDecimal("1000000000.123")));
        System.out.println(DF_NUMBER.format(new BigDecimal("1000000000.126")));
        System.out.println(DF_NUMBER.format(new BigDecimal("1000000000.00")));
        //System.out.println(BigDecimalUtils.formatInteger(null));
        //System.out.println(BigDecimalUtils.formatTwoDecimal(null));
    }
    public static boolean isInRange(BigDecimal value, BigDecimal lsl, BigDecimal usl) {
        // 如果 value、lsl 或 usl 为 null，直接返回 false
        if (value == null || lsl == null || usl == null) {
            return false;
        }

        // 检查 value 是否在 [lsl, usl] 范围内
        return value.compareTo(lsl) >= 0 && value.compareTo(usl) <= 0;
    }
}
