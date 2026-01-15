package com.px.ifp.spc.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 数据倍率和精度处理工具类
 */
public class NumberUtil {

    private NumberUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 放大数据的倍率
     * @param value 原始值
     * @param ratio 倍率（如：100表示放大100倍）
     * @param scale 精度（小数点后位数）
     * @return 处理后的值
     */
    public static BigDecimal enlargeNumber(BigDecimal value, int ratio, int scale) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.multiply(new BigDecimal(ratio))
                .setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * 缩小数据的倍率
     * @param value 原始值
     * @param ratio 倍率（如：100表示缩小100倍）
     * @param scale 精度（小数点后位数）
     * @return 处理后的值
     */
    public static BigDecimal shrinkNumber(BigDecimal value, int ratio, int scale) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.divide(new BigDecimal(ratio), scale, RoundingMode.HALF_UP);
    }

    /**
     * 放大数据的倍率（String类型输入）
     * @param value 原始值（字符串）
     * @param ratio 倍率
     * @param scale 精度
     * @return 处理后的值
     */
    public static BigDecimal enlargeNumber(String value, int ratio, int scale) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return enlargeNumber(new BigDecimal(value), ratio, scale);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 缩小数据的倍率（String类型输入）
     * @param value 原始值（字符串）
     * @param ratio 倍率
     * @param scale 精度
     * @return 处理后的值
     */
    public static BigDecimal shrinkNumber(String value, int ratio, int scale) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return shrinkNumber(new BigDecimal(value), ratio, scale);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 放大数据的倍率（double类型输入）
     * @param value 原始值
     * @param ratio 倍率
     * @param scale 精度
     * @return 处理后的值
     */
    public static BigDecimal enlargeNumber(double value, int ratio, int scale) {
        return enlargeNumber(BigDecimal.valueOf(value), ratio, scale);
    }

    /**
     * 缩小数据的倍率（double类型输入）
     * @param value 原始值
     * @param ratio 倍率
     * @param scale 精度
     * @return 处理后的值
     */
    public static BigDecimal shrinkNumber(double value, int ratio, int scale) {
        return shrinkNumber(BigDecimal.valueOf(value), ratio, scale);
    }

    /**
     * 放大数据的倍率（int类型输入）
     * @param value 原始值
     * @param ratio 倍率
     * @param scale 精度
     * @return 处理后的值
     */
    public static BigDecimal enlargeNumber(int value, int ratio, int scale) {
        return enlargeNumber(new BigDecimal(value), ratio, scale);
    }

    /**
     * 缩小数据的倍率（int类型输入）
     * @param value 原始值
     * @param ratio 倍率
     * @param scale 精度
     * @return 处理后的值
     */
    public static BigDecimal shrinkNumber(int value, int ratio, int scale) {
        return shrinkNumber(new BigDecimal(value), ratio, scale);
    }


    /**
     * 根据ratio和scale参数处理数据的倍率和精度
     * @param value 原始值
     * @param ratio 倍率参数：
     *              - 正数：放大相应倍数
     *              - 负数：缩小相应倍数
     *              - 0：不调整
     * @param scale 精度参数：
     *              - 正数：保留指定小数位数
     *              - 负数：保留指定整数位数（如-2表示保留到百位）
     *              - 0：不保留小数位
     * @return 处理后的值
     */
    public static BigDecimal autoAdjustByRatioAndScale(BigDecimal value, int ratio, int scale) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        // 处理倍率
        BigDecimal result = value;
        if (ratio > 0) {
            result = result.multiply(new BigDecimal(ratio));
        } else if (ratio < 0) {
            result = result.divide(new BigDecimal(Math.abs(ratio)), Math.abs(scale), RoundingMode.HALF_UP);
        }

        // 处理精度
        if (scale > 0) {
            // 保留指定小数位数
            result = result.setScale(scale, RoundingMode.HALF_UP);
        } else if (scale < 0) {
            // 保留指定整数位数
            int integerScale = Math.abs(scale);
            result = result.setScale(0, RoundingMode.HALF_UP)
                    .movePointLeft(integerScale)
                    .setScale(0, RoundingMode.HALF_UP)
                    .movePointRight(integerScale);
        } else {
            //scale = 0 不处理精度
        }

        return result;
    }

    /**
     * 根据ratio和scale参数处理数据的倍率和精度（String类型输入）
     */
    public static BigDecimal autoAdjustByRatioAndScale(String value, int ratio, int scale) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return autoAdjustByRatioAndScale(new BigDecimal(value), ratio, scale);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 根据ratio和scale参数处理数据的倍率和精度（double类型输入）
     */
    public static BigDecimal autoAdjustByRatioAndScale(double value, int ratio, int scale) {
        return autoAdjustByRatioAndScale(BigDecimal.valueOf(value), ratio, scale);
    }

    /**
     * 根据ratio和scale参数处理数据的倍率和精度（int类型输入）
     */
    public static BigDecimal autoAdjustByRatioAndScale(int value, int ratio, int scale) {
        return autoAdjustByRatioAndScale(new BigDecimal(value), ratio, scale);
    }
}