package com.px.ifp.spc.constant;

public class DictConstant {

    private DictConstant() {

    }


    /**
     * 室外天气字典值
     */
    public static final String WEATHER_TYPE_CODE = "OUTDOOR_WEATHER";

    public static final String WEATHER_REDIS_HOUR = "OUTDOOR_WEATHER_HOUR";
    /**
     * 洁净间区域字典
     */
    public static final String FAB_AREA = "FAB_AREA";

    /**
     * 洁净间位置编码
     */
    public static final String FAB_ZONE = "ROOM_7b09b7";
    /**
     *厂区位置编码
     */
    public static final String FAC2_CODE = "FAC_93abf6";
    public static final String FAC1_CODE = "FAC_83abf9";

    /**
     *  洁净间位置编码
     */
    public static final String FAB_POSITION_DICT = "CR_POSITION_CODE";
    /**
     * 洁净间系统编码
     */
    public static final String FAB_CATEGORY_DICT = "CR_SYSTEM_CODE";

    /**
     * 水课运维字典值
     */
    public static final String WATER_OPERATION_TYPE_CODE = "WTTS";

    public static final String WATER_WATER_BUDGET_VENDOR_TYPE_CODE = "WATER_BUDGET_VENDOR";


    /**
     * 水课看板字典值
     */
    public static final String WATER_OPERATION_DASHBOARD_TYPE_CODE = "WTTS";

    /**
     * 水课危险废弃物字典值
     */
    public static final String WATER_OPERATION_HAZARD_WASTER_TYPE_CODE = "WTTS";

    /**
     * 水课水质检测字典值
     */
    public static final String WATER_OPERATION_TEST_QUALITY_TYPE_CODE = "WTTS";

    /**
     * 水课系统
     */
    public static final String WATER_ITEM_SYSTEM = "WATER_SYSTEM";

    /**
     * 废液代码
     */
    public static final String WATER_ITEM_WASTE_LIQUID_CODE = "WI01";

    /**
     * 废液代码
     */
    public static final String MACHINE_ITEM_WASTE_LIQUID_CODE = "WI01M";

    /**
     * 废液 运输单位
     */
    public static final String WATER_ITEM_WASTE_LIQUID_TRANSPORT_COMPANY = "TU01";

    /**
     * 废液 运输单位
     */
    public static final String MACHINE_ITEM_WASTE_LIQUID_TRANSPORT_COMPANY = "TU01M";

    /**
     * 废液 接收单位
     */
    public static final String WATER_ITEM_WASTE_LIQUID_RECEIVE_COMPANY = "RU01";

    /**
     * 废液 接收单位
     */
    public static final String MACHINE_ITEM_WASTE_LIQUID_RECEIVE_COMPANY = "RU01M";


    /**
     * 固废代码
     */
    public static final String WATER_ITEM_WASTE_SOLID_CODE = "SW01";

    /**
     * 固废代码
     */
    public static final String MACHINE_ITEM_WASTE_SOLID_CODE = "SW01M";

    /**
     * 固废 运输单位
     */
    public static final String WATER_ITEM_WASTE_SOLID_TRANSPORT_COMPANY = "TU01";

    /**
     * 固废 运输单位
     */
    public static final String MACHINE_ITEM_WASTE_SOLID_TRANSPORT_COMPANY = "TU01M";

    /**
     * 固废 接收单位
     */
    public static final String WATER_ITEM_WASTE_SOLID_RECEIVE_COMPANY = "RU01";

    /**
     * 固废 接收单位
     */
    public static final String MACHINE_ITEM_WASTE_SOLID_RECEIVE_COMPANY = "RU01M";

    /**
     * 危险固废
     */
    @Deprecated
    public static final String WATER_ITEM_HAZARD_WASTER_SOLID = "DS01";

    /**
     * 危险固废
     */
    @Deprecated
    public static final String MACHINE_ITEM_HAZARD_WASTER_SOLID = "DS01M";

    /**
     * 危险废液
     */
    @Deprecated
    public static final String WATER_ITEM_HAZARD_WASTER_LIQUID = "DW01";

    /**
     * 危险废液
     */
    @Deprecated
    public static final String MACHINE_ITEM_HAZARD_WASTER_LIQUID = "DW01M";

    /**
     * 化学品 一般
     */
    //public static final String WATER_ITEM_CHEMICAL_GENERAL = "CH03";

    /**
     * 化学品 易制毒
     */
    //public static final String WATER_ITEM_CHEMICAL_PRECURSOR = "CH04";

    /**
     * 销售单位
     */
    public static final String WATER_ITEM_SALES_COMPANY = "SU01";

    /**
     * 存储地点
     */
    public static final String WATER_ITEM_STORAGE_LOCATION = "SL01";

    /**
     * 取样点
     */
    public static final String WATER_ITEM_SAMPLING_LOCATION = "SP01";

    /**
     * 取样点 纯水
     */
    public static final String WATER_ITEM_SAMPLING_LOCATION_UPW = "SP0104";

    /**
     * 取样点 废水
     */
    public static final String WATER_ITEM_SAMPLING_LOCATION_WWT = "SP0103";

    /**
     * 检测厂商
     */
    public static final String WATER_ITEM_TEST_MANUFACTURER = "TM01";

    /**
     * 检测项目 废水
     */
    public static final String WATER_ITEM_TEST_QUALITY_WASTE_WATER = "WTTSTI01AV01";

    /**
     * 检测项目 纯水(金属阴阳离子)
     */
    public static final String WATER_ITEM_TEST_QUALITY_UPW_METAL_IMPURITY = "WTTSTI01AV02";

    /**
     * 检测项目 纯水(细菌)
     */
    public static final String WATER_ITEM_TEST_QUALITY_UPW_BACTERIA = "WTTSTI01AV03";

    public static final String QUERY_ALL = "ALL";

    public static final String QUERY_ALL_NAME = "全部";

    public static class WaterHazardWasteDictJson {
        //废物名称
        public static final String name = "name";
        //废物类别
        public static final String type = "type";
        //废物代码
        public static final String code = "code";
        //characteristics
        public static final String characteristics = "tx";

    }

    /**
     * 机械课运维字典值
     */
    public static final String MACHINE_OPERATION_TYPE_CODE = "MCTS";
    /**
     * 机械课FFU设备类型
     */
    public static final Long MACHINE_FFU_TYPE = 595L;//是否场内场外一致？（玉超：确定不变）

    /**
     * 机械课FFU化濾类型
     */
    public static final String MACHINE_FILTER_TYPE = "FFUHLLX";

    /**
     * 机械课MAU膜类型
     */
    public static final String MACHINE_MAU_TYPE = "MAUMLX";

    /**
     * 机械课FFU状态
     */
    public static final String MACHINE_FFU_STATUS = "EQP_RUN_STATUS";

    /**
     * 机械课看板字典值
     */
    public static final String MACHINE_OPERATION_DASHBOARD_TYPE_CODE = "KB03";
    public static final String MACHINE_OPERATION_DASHBOARD_WATER_TARGET = "KB03_WATER";
    public static final String MACHINE_OPERATION_DASHBOARD_GAS_TARGET = "KB03_GAS";
    /**
     * 洁净室内控指标AMC
     */
    public static final String MACHINE_ITEM_CLEAN_ROOM_AMC = "CIT01";

    /**
     * 洁净室内控指标VOC
     */
    public static final String MACHINE_ITEM_CLEAN_ROOM_VOC = "CIT02";

    /**
     * 洁净室内控指标Particle
     */
    public static final String MACHINE_ITEM_CLEAN_ROOM_PARTICLE = "CIT03";
    /**
     * 洁净室内控指标温湿度
     */
    public static final String MACHINE_ITEM_CLEAN_ROOM_TEMP = "CIT04";

    /**
     * 洁净室取样类别
     */
    public static final String MACHINE_TEST_CATEGORY_AMC = "AMC";
    public static final String MACHINE_TEST_CATEGORY_VOC = "VOC";
    public static final String MACHINE_TEST_CATEGORY_PARTICLE = "PARTICLE";
    public static final String MACHINE_TEST_CATEGORY_TEMP = "TEMP";
    /**
     * 机械课取样点字典值
     */
    public static final String MACHINE_OPERATION_LOC_CODE = "CSP01";

    /**
     * 机械课 水质检测 热水运行曲线
     */
    public static final String MACHINE_WATER_QUALITY_HW = "HWConfig";

    public static final String MACHINE_WATER_QUALITY_PCW = "PCWConfig";

    public static final String MACHINE_WATER_QUALITY_HHW = "HHWConfig";

    public static final String MACHINE_WATER_QUALITY_MCHW = "MCHWConfig";

    public static final String MACHINE_WATER_QUALITY_LCHW = "LCHWConfig";

    public static final String MACHINE_WATER_QUALITY_CW = "CWConfig";


    public static final String MACHINE_FILTER_BASE = "HLLXBASE";
    public static final String MACHINE_FILTER_ACID = "HLLXSX";
    public static final String MACHINE_FILTER_ALK = "HLLXJX";
    public static final String MACHINE_FILTER_VOC = "HLLXYJ";
    public static final String MACHINE_FILTER_HEPA = "HLLXHEPA";
    public static final String MACHINE_FILTER_RC = "HLLXRC";

    public static final String MIC_EXHAUST_OFFLINE_ACID = "MIC_EXHAUST_OFFLINE_ACID";
    public static final String MIC_EXHAUST_OFFLINE_VOC = "MIC_EXHAUST_OFFLINE_VOC";
    public static final String MIC_EXHAUST_OFFLINE_ALK = "MIC_EXHAUST_OFFLINE_ALK";
    /**
     * 机械课加药管理  加药机构
     */
    public static final String MACHINE_CHEMICAL_DOSING_ORGANIZATION = "DA01";

    /**
     * 机械课加药管理  加药配置
     */
    public static final String MACHINE_CHEMICAL_DOSING_IMPORT_CONFIG = "RC01";

    /**
     * 侦测器字段值
     */
    public static final String GAS_DETECTOR_STATUS = "GAS_DETECTOR_STATUS";

    /**
     * 气化课值班工程师
     */
    public static final String GAS_DUTY_ENGINEER = "GAS_DUTY_ENGINEER";

    /**
     * 气化课ERC工程师
     */
    public static final String GAS_ERC_ENGINEER = "GAS_ERC_ENGINEER";

    /**
     * 气化课供应提醒点位列表
     */
    public static final String GAS_SUPPLY_REMINDER_POINTS = "GAS_SUPPLY_REMINDER_POINTS";

    /**
     * 气化课供应提醒 气瓶 点位列表
     */
    public static final String GAS_SUPPLY_CYLINDER_POINTS = "GAS_SUPPLY_CYLINDER_POINTS";

    /**
     * 气化课供应提醒 酸桶 点位列表
     */
    public static final String GAS_SUPPLY_BUCKET_POINTS = "GAS_SUPPLY_BUCKET_POINTS";


    /**
     * 气化课供应提醒 槽车 点位列表
     */
    public static final String GAS_SUPPLY_TANK_POINTS = "GAS_SUPPLY_TANK_POINTS";
    /**
     * 气化课看板
     */
    public static final String GAS_DASHBOARD = "GAS_DASHBOARD";

    /**
     * 气化课看板-大宗气体流量统计
     */
    public static final String BULK_GAS_FLOW_STAT = "BULK_GAS_FLOW_STAT";

    /**
     * 电课运维字典值
     */
    public static final String ELE_OPERATION_TYPE_CODE = "ELTS";
    /**
     * 电课运维字典值
     */
    public static final String ELE_OPERATION_KB_CODE = "KB01";
    /**
     * 课室
     */
    public static final String CLASS_NAME = "className";
    /**
     * 指标级别
     */
    public static final String SPC_LEVEL = "SPCL";

    /**
     * 耗材生命周期管理
     */
    public static final String CONSUMABLE_LIFE_CYCLE_TYPE_CODE = "CONSUMABLE_LIFE_CYCLE";

    /**
     * 设备使用耗材
     */
    public static final String CONSUMABLE_ITEM_CODE_EQP_CONSUMABLE = "EQP_CONSUMABLE";


    public static final String GAS_MIXED_ACID_POINT_MAP = "GAS_MIXED_ACID_POINT_MAP";
    public static final String GAS_MIXED_ACID_POINT_MAP_V2 = "GAS_MIXED_ACID_POINT_MAP_V2";
    /**
     * 缓存数据前置key
     */
    public static final String GAS_MIXED_ACID_ENTITY = "GAS_M_A_E";
    public static final String LINE = "_";
    public static final String SIGN = "SIGN";
    public static final String ONE = "ONE";
    public static final String TWO = "TWO";
    public static final String THREE = "THREE";
    public static final String RESULT = "RESULT";
    public static final String BASE = "BASE";
    public static final String HASH = "HASH";
    public static final String VALUES = "VALUES";

    /**
     * 算法场景及映射字典
     */
    public static final String ALGORITHM = "ALGORITHM";

    /**
     * 机械科 洁净室取样区域
     */
    public static final String CLEAN_ROOM_AREA = "CLEAN_ROOM_AREA";
    /**
     * 纯水后缀
     */
    public static final String WATER_CS = "CS";
    /**
     * NaOH字典value
     */
    public static final String WATER_ITEM_VALUE_NAOH = "CH01_CS_NaOH";
    /**
     * NaOH字典value
     */
    public static final String WATER_ITEM_VALUE_HCL = "CH02_CS_HCl";
    /**
     * 水科系统运行操作--dicType
     */
    public static final String WATER_WTTD_RUN_OPERATION = "WTTD_RUN_OPERATION";
    /**
     * 水科系统运行操作 反洗
     */
    public static final String WATER_BACK_WASH = "WATER_BACK_WASH";
    /**
     * 水科系统运行操作 再生
     */
    public static final String WATER_REGENERATION = "WATER_REGENERATION";
    /**
     * 高效制冷机房
     */
    public static final String MACHINE_REFRIGERATION_DICTYPE = "MACHINE_REFRIGERATION";
    /**
     * 高效制冷机房-总览-综合能效指标
     */
    public static final String MACHINE_REFRIGERATION_ALL_CEEI_DICITEM = "MACHINE_REFRIGERATION_ALL_CEEI";
    /**
     * 高效制冷机房-总览-实时运行参数
     */
    public static final String MACHINE_REFRIGERATION_ALL_RUNTIME_PARAMS_DICITEM = "MACHINE_REFRIGERATION_ALL_RUNTIME_PARAMS";
    /**
     * 高效制冷机房-总览-节能分析
     */
    public static final String MACHINE_REFRIGERATION_ALL_ENERGY_ANALYSIS_DICITEM = "MACHINE_REFRIGERATION_ALL_ENERGY_ANALYSIS";
    /**
     * 高效制冷机房-低温系统-综合能效指标
     */
    public static final String MACHINE_REFRIGERATION_LOW_CEEI_DICITEM = "MACHINE_REFRIGERATION_LOW_CEEI";
    /**
     * 高效制冷机房-低温系统-实时运行参数
     */
    public static final String MACHINE_REFRIGERATION_LOW_RUNTIME_PARAMS_DICITEM = "MACHINE_REFRIGERATION_LOW_RUNTIME_PARAMS";
    /**
     * 高效制冷机房-低温系统-节能分析
     */
    public static final String MACHINE_REFRIGERATION_LOW_ENERGY_ANALYSIS_DICITEM = "MACHINE_REFRIGERATION_LOW_ENERGY_ANALYSIS";
    /**
     * 高效制冷机房-低温系统-实时运行参数折线图
     */
    public static final String MACHINE_REFRIGERATION_LOW_RUNTIME_PARAMS_CHART_DICITEM = "MACHINE_REFRIGERATION_LOW_RUNTIME_PARAMS_CHART";
    /**
     * 高效制冷机房-中温系统-综合能效指标
     */
    public static final String MACHINE_REFRIGERATION_MID_CEEI_DICITEM = "MACHINE_REFRIGERATION_MID_CEEI";
    /**
     * 高效制冷机房-中温系统-实时运行参数
     */
    public static final String MACHINE_REFRIGERATION_MID_RUNTIME_PARAMS_DICITEM = "MACHINE_REFRIGERATION_MID_RUNTIME_PARAMS";
    /**
     * 高效制冷机房-中温系统-节能分析
     */
    public static final String MACHINE_REFRIGERATION_MID_ENERGY_ANALYSIS_DICITEM = "MACHINE_REFRIGERATION_MID_ENERGY_ANALYSIS";
    /**
     * 高效制冷机房-中温系统-实时运行参数折线图
     */
    public static final String MACHINE_REFRIGERATION_MID_RUNTIME_PARAMS_CHART_DICITEM = "MACHINE_REFRIGERATION_MID_RUNTIME_PARAMS_CHART";
    /**
     * 高效制冷机房-数据展示-需由其它计算好的指标来计算的指标
     */
    public static final String MACHINE_REF_DATA_SHOW_CALCULATE_DICITEM = "MACHINE_REF_DATA_SHOW_CALCULATE";
    /**
     * 高效制冷机房-机房运行模式
     */
    public static final String MACHINE_ROOM_OPERATION_MODE_DICITEM = "MACHINE_ROOM_OPERATION_MODE";


    public static final String WATER_WASTE_PREDICATE_DAY = "WATER_WASTE_PREDICATE_DAY";



    public static final String FACTORY_AREA = "factoryArea";

//    /**
//     * 高效制冷机房-优化建议-巡优参数-参数组合查询-步长
//     */
//    public static final String MACHINE_REF_DOR_PARAM_STEP = "MACHINE_REF_DOR_PARAM_STEP";
    /**
     * 高效制冷机房-优化建议-巡优参数-字段
     */
    public static final String MACHINE_REF_DOR_PARAM_LIST_FIELDS = "MACHINE_REF_DOR_PARAM_LIST_FIELDS";
    /**
     * 高效制冷机房-数据统计-固定展示指标
     */
    public static final String MACHINE_REF_DATA_STATISTICS_FIXED_POINTS = "MACHINE_REF_DATA_STATISTICS_FIXED_POINTS";
    /**
     * 高效制冷机房-数据统计-湿度
     */
    public static final String MACHINE_REF_DATA_STATISTICS_HUMIDITY = "MACHINE_REF_DATA_STATISTICS_HUMIDITY";
    /**
     * 高效制冷机房-数据统计-温度
     */
    public static final String MACHINE_REF_DATA_STATISTICS_TEMPERATURE = "MACHINE_REF_DATA_STATISTICS_TEMPERATURE";

    public static final String MACHINE_REF_MODEL_EVA = "MACHINE_REF_MODEL_EVA";

}
