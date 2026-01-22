package com.px.ifp.spc.dto.manager.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * SPC 采样策略 DTO
 *
 * @author liuxj
 * @since 2026-01-20
 */
@Data
@Schema(name = "SamplingStrategyDTO", description = "SPC 采样策略配置")
public class SamplingStrategyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID（更新时必填）")
    private Long id;

    @Schema(description = "采样周期（秒）：60/300/3600")
    private Integer periodS;

    @Schema(description = "周期标签：1m/5m/1h")
    private String periodLabel;

    @Schema(description = "策略类型：periodic/change/threshold/hybrid")
    private String strategyType;

    @Schema(description = "窗口类型：tumble/hop/session")
    private String windowType;

    @Schema(description = "窗口大小（秒）；periodic默认=period_s；hop用作INTERVAL")
    private Integer windowSizeS;

    @Schema(description = "窗口滑动步长（秒），仅hop生效（SLIDING）")
    private Integer slideStepS;

    @Schema(description = "会话窗口gap（秒），仅session生效")
    private Integer sessionGapS;

    @Schema(description = "绝对死区")
    private BigDecimal deadbandAbs;

    @Schema(description = "相对死区（%）")
    private BigDecimal deadbandPercent;

    @Schema(description = "最大采样间隔（秒）")
    private Integer maxIntervalS;

    @Schema(description = "上阈值")
    private BigDecimal thresholdUpper;

    @Schema(description = "下阈值")
    private BigDecimal thresholdLower;

    @Schema(description = "正常采样周期")
    private Integer normalPeriodS;

    @Schema(description = "告警后采样周期")
    private Integer alertPeriodS;

    @Schema(description = "告警模式持续时间")
    private Integer alertDurationS;

    @Schema(description = "统计特征列表")
    private List<String> features;

    @Schema(description = "计算方式：realtime/batch/hybrid")
    private String computationMode;

    @Schema(description = "样本值口径：avg/last/median")
    private String valueMode;

    @Schema(description = "子组窗口秒数（Xbar-R用），推荐60")
    private Integer subgroupWindowS;

    @Schema(description = "子组最小有效点数（baseline/图控），默认4")
    private Integer minValidN;

    @Schema(description = "有效比例阈值（baseline），默认0.80")
    private BigDecimal validRatioMin;

    @Schema(description = "子组内漂移阈值（baseline），默认0.0100")
    private BigDecimal driftRatioMax;

    @Schema(description = "基线计算窗口天数（rolling）")
    private Integer baselineWindowDays;

    @Schema(description = "基线刷新周期（秒）")
    private Integer baselineRefreshS;

    @Schema(description = "是否开启采样")
    private Boolean enabled;

    @Schema(description = "优先级（数字越大优先级越高）")
    private Integer priority;

    @Schema(description = "厂区编码")
    private String facCode;

    @Schema(description = "异常数据处理:是否启用物理上下限守卫（0=关闭,1=开启）")
    private Byte physicalLimitEnabled;

    @Schema(description = "异常数据处理:物理下限值（NULL表示不限制）")
    private BigDecimal physicalMinValue;

    @Schema(description = "异常数据处理:物理上限值（NULL表示不限制）")
    private BigDecimal physicalMaxValue;

    @Schema(description = "异常数据处理:越界值处理方式（drop=丢弃,clip=裁剪到边界,mark=标记但保留）")
    private String outOfRangeAction;

    @Schema(description = "异常数据处理:离群值检测方法（仅用于基线计算）enum('none','3sigma','median','iqr','hampel')")
    private String outlierDetectionMethod;

    @Schema(description = "异常数据处理:3σ方法：异常阈值倍数（默认3.0）")
    private BigDecimal outlier3sigmaThreshold;

    @Schema(description = "异常数据处理:3σ方法：迭代次数（1-3，默认2）")
    private Integer outlier3sigmaIterations;

    @Schema(description = "异常数据处理:中位数方法：MAD倍数阈值（默认3.0）")
    private BigDecimal outlierMedianThreshold;

    @Schema(description = "异常数据处理:中位数方法：滑动窗口大小（默认7）")
    private Integer outlierMedianWindow;

    @Schema(description = "异常数据处理:IQR方法：四分位数范围倍数K（默认1.5）")
    private BigDecimal outlierIqrK;

    @Schema(description = "异常数据处理:Hampel方法：阈值K（默认3.0）")
    private BigDecimal outlierHampelK;

    @Schema(description = "异常数据处理:Hampel方法：滑动窗口大小（默认7）")
    private Integer outlierHampelWindow;

    @Schema(description = "异常数据处理:检测到离群值的处理方式enum('drop','mark','keep')")
    private String outlierAction;

    @Schema(description = "异常数据处理:是否启用质量码过滤（0=关闭,1=开启）")
    private Boolean qualityFilterEnabled;

    @Schema(description = "异常数据处理:允许的质量码列表，逗号分隔（默认仅好值192）")
    private String qualityAllowedCodes;

    @Schema(description = "异常数据处理:坏值处理方式（drop=丢弃,mark=标记,keep=保留）")
    private String badValueAction;
}
