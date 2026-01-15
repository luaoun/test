package com.px.ifp.spc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * SPC 采样策略配置表
 * </p>
 *
 * @author liuxj
 * @since 2026-01-09
 */
@Getter
@Setter
@TableName("spc_sampling_strategy")
@Schema(name = "SpcSamplingStrategy", description = "SPC 采样策略配置表")
public class SpcSamplingStrategy implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "指标编码")
    @TableField("measure_code")
    private String measureCode;

    @Schema(description = "采样周期（秒）：60/300/3600")
    @TableField("period_s")
    private Integer periodS;

    @Schema(description = "周期标签：1m/5m/1h")
    @TableField("period_label")
    private String periodLabel;

    @Schema(description = "策略类型：periodic/change/threshold/hybrid")
    @TableField("strategy_type")
    private String strategyType;

    @Schema(description = "窗口类型：tumble/hop/session")
    @TableField("window_type")
    private String windowType;

    @Schema(description = "窗口大小（秒）；periodic默认=period_s；hop用作INTERVAL")
    @TableField("window_size_s")
    private Integer windowSizeS;

    @Schema(description = "窗口滑动步长（秒），仅hop生效（SLIDING）")
    @TableField("slide_step_s")
    private Integer slideStepS;

    @Schema(description = "会话窗口gap（秒），仅session生效")
    @TableField("session_gap_s")
    private Integer sessionGapS;

    @Schema(description = "绝对死区")
    @TableField("deadband_abs")
    private BigDecimal deadbandAbs;

    @Schema(description = "相对死区（%）")
    @TableField("deadband_percent")
    private BigDecimal deadbandPercent;

    @Schema(description = "最大采样间隔（秒）")
    @TableField("max_interval_s")
    private Integer maxIntervalS;

    @Schema(description = "上阈值")
    @TableField("threshold_upper")
    private BigDecimal thresholdUpper;

    @Schema(description = "下阈值")
    @TableField("threshold_lower")
    private BigDecimal thresholdLower;

    @Schema(description = "正常采样周期")
    @TableField("normal_period_s")
    private Integer normalPeriodS;

    @Schema(description = "告警后采样周期")
    @TableField("alert_period_s")
    private Integer alertPeriodS;

    @Schema(description = "告警模式持续时间")
    @TableField("alert_duration_s")
    private Integer alertDurationS;

    @Schema(description = "统计特征（逗号分隔）")
    @TableField("features")
    private String features;

    @Schema(description = "计算方式：realtime/batch/hybrid")
    @TableField("computation_mode")
    private String computationMode;

    @Schema(description = "样本值口径：avg/last/median")
    @TableField("value_mode")
    private String valueMode;

    @Schema(description = "子组窗口秒数（Xbar-R用），推荐60")
    @TableField("subgroup_window_s")
    private Integer subgroupWindowS;

    @Schema(description = "子组最小有效点数（baseline/图控），默认4")
    @TableField("min_valid_n")
    private Integer minValidN;

    @Schema(description = "有效比例阈值（baseline），默认0.80")
    @TableField("valid_ratio_min")
    private BigDecimal validRatioMin;

    @Schema(description = "子组内漂移阈值（baseline），默认0.0100")
    @TableField("drift_ratio_max")
    private BigDecimal driftRatioMax;

    @Schema(description = "基线计算窗口天数（rolling）")
    @TableField("baseline_window_days")
    private Integer baselineWindowDays;

    @Schema(description = "基线刷新周期（秒）")
    @TableField("baseline_refresh_s")
    private Integer baselineRefreshS;

    @Schema(description = "是否开启采样")
    @TableField("enabled")
    private Boolean enabled;

    @Schema(description = "优先级（数字越大优先级越高）")
    @TableField("priority")
    private Integer priority;

    @Schema(description = "记录创建时间")
    @TableField("created_at")
    private Date createdAt;

    @Schema(description = "记录更新时间")
    @TableField("updated_at")
    private Date updatedAt;

    @Schema(description = "厂区编码")
    @TableField("fac_code")
    private String facCode;

    @Schema(description = "异常数据处理:是否启用物理上下限守卫（0=关闭,1=开启）")
    @TableField("physical_limit_enabled")
    private Byte physicalLimitEnabled;

    @Schema(description = "异常数据处理:物理下限值（NULL表示不限制）")
    @TableField("physical_min_value")
    private BigDecimal physicalMinValue;

    @Schema(description = "异常数据处理:物理上限值（NULL表示不限制）")
    @TableField("physical_max_value")
    private BigDecimal physicalMaxValue;

    @Schema(description = "异常数据处理:越界值处理方式（drop=丢弃,clip=裁剪到边界,mark=标记但保留）")
    @TableField("out_of_range_action")
    private String outOfRangeAction;

    @Schema(description = "异常数据处理:离群值检测方法（仅用于基线计算）enum('none','3sigma','median','iqr','hampel')")
    @TableField("outlier_detection_method")
    private String outlierDetectionMethod;

    @Schema(description = "异常数据处理:3σ方法：异常阈值倍数（默认3.0）")
    @TableField("outlier_3sigma_threshold")
    private BigDecimal outlier3sigmaThreshold;

    @Schema(description = "异常数据处理:3σ方法：迭代次数（1-3，默认2）")
    @TableField("outlier_3sigma_iterations")
    private Integer outlier3sigmaIterations;

    @Schema(description = "异常数据处理:中位数方法：MAD倍数阈值（默认3.0）")
    @TableField("outlier_median_threshold")
    private BigDecimal outlierMedianThreshold;

    @Schema(description = "异常数据处理:中位数方法：滑动窗口大小（默认7）")
    @TableField("outlier_median_window")
    private Integer outlierMedianWindow;

    @Schema(description = "异常数据处理:IQR方法：四分位数范围倍数K（默认1.5）")
    @TableField("outlier_iqr_k")
    private BigDecimal outlierIqrK;

    @Schema(description = "异常数据处理:Hampel方法：阈值K（默认3.0）")
    @TableField("outlier_hampel_k")
    private BigDecimal outlierHampelK;

    @Schema(description = "异常数据处理:Hampel方法：滑动窗口大小（默认7）")
    @TableField("outlier_hampel_window")
    private Integer outlierHampelWindow;

    @Schema(description = "异常数据处理:检测到离群值的处理方式enum('drop','mark','keep')")
    @TableField("outlier_action")
    private String outlierAction;

    @Schema(description = "异常数据处理:是否启用质量码过滤（0=关闭,1=开启）")
    @TableField("quality_filter_enabled")
    private Boolean qualityFilterEnabled;

    @Schema(description = "异常数据处理:允许的质量码列表，逗号分隔（默认仅好值192）")
    @TableField("quality_allowed_codes")
    private String qualityAllowedCodes;

    @Schema(description = "异常数据处理:坏值处理方式（drop=丢弃,mark=标记,keep=保留）")
    @TableField("bad_value_action")
    private String badValueAction;
}
