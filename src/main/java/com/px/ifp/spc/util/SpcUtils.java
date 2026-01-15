package com.px.ifp.spc.util;

public class SpcUtils {

    /**
     * 计算Cpk值
     * @param samples 样本数据
     * @param usl 上规格限
     * @param lsl 下规格限
     * @return Cpk值
     */
    public static double calculateCpk(double[] samples, double usl, double lsl) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("样本数据不能为空");
        }

        // 计算平均值 (μ)
        double mean = calculateMean(samples);

        // 计算标准差 (σ)
        double standardDeviation = calculateStandardDeviation(samples, mean);

        // 计算Cpk值
        double cpkUpper = (usl - mean) / (3 * standardDeviation);
        double cpkLower = (mean - lsl) / (3 * standardDeviation);

        // 返回较小的Cpk值
        return Math.min(cpkUpper, cpkLower);
    }

    /**
     * 计算Cpk值
     * @param standardDeviation 方差值
     * @param mean  平均值
     * @param usl   上规格限
     * @param lsl   下规格限
     * @return
     */
    public static double calculateCpk(double standardDeviation,double mean, double usl, double lsl) {
        // 计算Cpk值
        double cpkUpper = (usl - mean) / (3 * standardDeviation);
        double cpkLower = (mean - lsl) / (3 * standardDeviation);
        // 返回较小的Cpk值
        return Math.min(cpkUpper, cpkLower);
    }

    // 计算平均值
    private static double calculateMean(double[] samples) {
        double sum = 0;
        for (double sample : samples) {
            sum += sample;
        }
        return sum / samples.length;
    }

    // 计算标准差
    private static double calculateStandardDeviation(double[] samples, double mean) {
        double sumOfSquares = 0;
        for (double sample : samples) {
            sumOfSquares += Math.pow(sample - mean, 2);
        }
        return Math.sqrt(sumOfSquares / samples.length);
    }

    public static double calculateCP(double standardDeviation,double upperLimit, double lowerLimit) {
        return (upperLimit - lowerLimit) / (6.0 * standardDeviation);
    }


}
