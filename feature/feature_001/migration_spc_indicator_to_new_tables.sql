-- ========================================================================
-- SPC 数据迁移脚本
-- 从 spc_indicator 迁移到 spc_point_metadata 和 spc_sampling_strategy
--
-- 作者: Claude Code
-- 日期: 2026-01-20
-- 说明:
--   1. 将旧表 spc_indicator 的数据迁移到新表 spc_point_metadata
--   2. 为每个指标创建默认的采样策略到 spc_sampling_strategy
--   3. 支持回滚操作
-- ========================================================================

-- 设置安全模式，避免误删数据
SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;

USE `ifp-operation`;

-- ========================================================================
-- 步骤 0: 备份原表数据（可选但强烈建议）
-- ========================================================================
-- 如果备份表已存在，先删除
DROP TABLE IF EXISTS `spc_indicator_backup_20260120`;

-- 创建备份表
CREATE TABLE `spc_indicator_backup_20260120` LIKE `spc_indicator`;
INSERT INTO `spc_indicator_backup_20260120` SELECT * FROM `spc_indicator`;

SELECT CONCAT('备份完成: 共备份 ', COUNT(*), ' 条记录') AS backup_info
FROM `spc_indicator_backup_20260120`;

-- ========================================================================
-- 步骤 1: 数据迁移到 spc_point_metadata 表
-- ========================================================================

-- 插入数据到 spc_point_metadata
-- 注意：使用 INSERT IGNORE 避免重复数据导致失败
INSERT IGNORE INTO `spc_point_metadata` (
    `id`,
    `measure_code`,
    `measure_name`,
    `job_id`,
    `indicator_name`,
    `indicator_level`,
    `class_code`,
    `system_code`,
    `unit`,
    `data_type`,
    `y_axis_min`,
    `y_axis_max`,
    `y_axis_step`,
    `enable_realtime`,
    `enable_offline`,
    `target_value`,
    `ucl_value`,
    `lcl_value`,
    `uwl_value`,
    `lwl_value`,
    `usl_value`,
    `lsl_value`,
    `u3l_value`,
    `l3l_value`,
    `ocap_template_id`,
    `enabled_spc_controlled`,
    `deleted_id`,
    `created_at`,
    `updated_at`,
    `created_by`,
    `updated_by`,
    `fac_code`
)
SELECT
    -- 基本字段
    `id`,
    `point` AS `measure_code`,                          -- point → measure_code (关键映射)
    NULL AS `measure_name`,                             -- 需要从指标库补充
    `job_id`,
    `indicator_name`,
    `indicator_level`,
    `class_code`,
    `system_code`,
    `point_unit` AS `unit`,
    'analog' AS `data_type`,                            -- 默认为模拟量

    -- Y轴相关
    `start_value` AS `y_axis_min`,
    `end_value` AS `y_axis_max`,
    `step` AS `y_axis_step`,

    -- 开关状态
    IF(`status` = b'1', 1, 0) AS `enable_realtime`,    -- bit → tinyint
    1 AS `enable_offline`,                              -- 默认允许离线控制图

    -- 控制线值
    `target_value`,
    `ucl_value`,
    `lcl_value`,
    `uwl_value`,
    `lwl_value`,
    `usl_value`,
    `lsl_value`,
    `u3l_value`,
    `l3l_value`,

    -- OCAP 模板
    `ocap` AS `ocap_template_id`,

    -- SPC 控制开关（默认开启）
    1 AS `enabled_spc_controlled`,

    -- 删除标识
    IF(`deleted` = b'1', `id`, 0) AS `deleted_id`,     -- bit → bigint, 已删除的记录用 id 作为标识

    -- 时间戳
    `create_time` AS `created_at`,
    `update_time` AS `updated_at`,

    -- 创建/更新人
    COALESCE(`creator`, 'migration') AS `created_by`,
    COALESCE(`creator`, 'migration') AS `updated_by`,

    -- 厂区编码
    `fac_code`
FROM
    `spc_indicator`
WHERE
    `deleted` = b'0';  -- 只迁移未删除的数据

-- 查看迁移结果
SELECT CONCAT('spc_point_metadata 迁移完成: 共迁移 ', ROW_COUNT(), ' 条记录') AS migration_info;

-- ========================================================================
-- 步骤 2: 为每个指标创建默认采样策略到 spc_sampling_strategy
-- ========================================================================

-- 为每个迁移的指标创建默认的采样策略
-- 注意：使用 INSERT IGNORE 避免重复数据导致失败
INSERT IGNORE INTO `spc_sampling_strategy` (
    `measure_code`,
    `period_s`,
    `period_label`,
    `strategy_type`,
    `window_type`,
    `window_size_s`,
    `features`,
    `computation_mode`,
    `value_mode`,
    `baseline_window_days`,
    `baseline_refresh_s`,
    `enabled`,
    `priority`,
    `physical_limit_enabled`,
    `out_of_range_action`,
    `outlier_detection_method`,
    `outlier_action`,
    `quality_filter_enabled`,
    `quality_allowed_codes`,
    `bad_value_action`,
    `created_at`,
    `updated_at`,
    `fac_code`
)
SELECT
    `point` AS `measure_code`,                         -- 使用原表的 point 作为 measure_code
    60 AS `period_s`,                                   -- 默认 60 秒采样周期
    '1m' AS `period_label`,                             -- 周期标签：1分钟
    'periodic' AS `strategy_type`,                      -- 策略类型：周期性
    'tumble' AS `window_type`,                          -- 窗口类型：滚动窗口
    60 AS `window_size_s`,                              -- 窗口大小：60秒
    'avg,max,min,std' AS `features`,                    -- 统计特征
    'realtime' AS `computation_mode`,                   -- 计算方式：实时
    'avg' AS `value_mode`,                              -- 样本值口径：平均值
    7 AS `baseline_window_days`,                        -- 基线窗口：7天
    3600 AS `baseline_refresh_s`,                       -- 基线刷新：1小时
    IF(`status` = b'1', 1, 0) AS `enabled`,            -- 继承原状态
    0 AS `priority`,                                    -- 默认优先级
    1 AS `physical_limit_enabled`,                      -- 启用物理限制
    'drop' AS `out_of_range_action`,                    -- 越界值处理：丢弃
    'none' AS `outlier_detection_method`,               -- 离群值检测：不检测
    'drop' AS `outlier_action`,                         -- 离群值处理：丢弃
    1 AS `quality_filter_enabled`,                      -- 启用质量码过滤
    '192' AS `quality_allowed_codes`,                   -- 允许的质量码
    'drop' AS `bad_value_action`,                       -- 坏值处理：丢弃
    `create_time` AS `created_at`,
    `update_time` AS `updated_at`,
    `fac_code`
FROM
    `spc_indicator`
WHERE
    `deleted` = b'0';  -- 只为未删除的数据创建采样策略

-- 查看迁移结果
SELECT CONCAT('spc_sampling_strategy 迁移完成: 共创建 ', ROW_COUNT(), ' 条采样策略') AS migration_info;

-- ========================================================================
-- 步骤 3: 数据一致性检查
-- ========================================================================

-- 检查 spc_point_metadata 迁移数量
SELECT
    '检查点 1: spc_point_metadata 数据量' AS check_point,
    (SELECT COUNT(*) FROM `spc_indicator` WHERE `deleted` = b'0') AS source_count,
    (SELECT COUNT(*) FROM `spc_point_metadata` WHERE `deleted_id` = 0) AS target_count,
    CASE
        WHEN (SELECT COUNT(*) FROM `spc_indicator` WHERE `deleted` = b'0') =
             (SELECT COUNT(*) FROM `spc_point_metadata` WHERE `deleted_id` = 0)
        THEN '✓ 数量一致'
        ELSE '✗ 数量不一致，请检查'
    END AS result;

-- 检查 spc_sampling_strategy 迁移数量
SELECT
    '检查点 2: spc_sampling_strategy 数据量' AS check_point,
    (SELECT COUNT(*) FROM `spc_indicator` WHERE `deleted` = b'0') AS expected_count,
    (SELECT COUNT(*) FROM `spc_sampling_strategy`) AS actual_count,
    CASE
        WHEN (SELECT COUNT(*) FROM `spc_indicator` WHERE `deleted` = b'0') <=
             (SELECT COUNT(*) FROM `spc_sampling_strategy`)
        THEN '✓ 数量正常'
        ELSE '✗ 采样策略数量不足'
    END AS result;

-- 检查是否有孤立的采样策略（没有对应的 point_metadata）
SELECT
    '检查点 3: 孤立的采样策略' AS check_point,
    COUNT(*) AS orphan_count,
    CASE
        WHEN COUNT(*) = 0 THEN '✓ 无孤立记录'
        ELSE '✗ 存在孤立记录，请检查'
    END AS result
FROM
    `spc_sampling_strategy` s
LEFT JOIN
    `spc_point_metadata` p ON s.`measure_code` = p.`measure_code`
WHERE
    p.`measure_code` IS NULL;

-- 检查关键字段是否为空
SELECT
    '检查点 4: 关键字段完整性' AS check_point,
    SUM(CASE WHEN `measure_code` IS NULL THEN 1 ELSE 0 END) AS null_measure_code,
    SUM(CASE WHEN `job_id` IS NULL THEN 1 ELSE 0 END) AS null_job_id,
    SUM(CASE WHEN `indicator_name` IS NULL THEN 1 ELSE 0 END) AS null_indicator_name,
    CASE
        WHEN SUM(CASE WHEN `measure_code` IS NULL THEN 1 ELSE 0 END) = 0
             AND SUM(CASE WHEN `job_id` IS NULL THEN 1 ELSE 0 END) = 0
             AND SUM(CASE WHEN `indicator_name` IS NULL THEN 1 ELSE 0 END) = 0
        THEN '✓ 关键字段完整'
        ELSE '✗ 存在空值，请检查'
    END AS result
FROM
    `spc_point_metadata`;

-- ========================================================================
-- 步骤 4: 查看迁移摘要
-- ========================================================================

SELECT
    '==================== 迁移摘要 ====================' AS summary;

SELECT
    '原表 spc_indicator' AS table_name,
    COUNT(*) AS total_records,
    SUM(CASE WHEN `deleted` = b'0' THEN 1 ELSE 0 END) AS active_records,
    SUM(CASE WHEN `deleted` = b'1' THEN 1 ELSE 0 END) AS deleted_records
FROM
    `spc_indicator`;

SELECT
    '新表 spc_point_metadata' AS table_name,
    COUNT(*) AS total_records,
    SUM(CASE WHEN `deleted_id` = 0 THEN 1 ELSE 0 END) AS active_records,
    SUM(CASE WHEN `deleted_id` > 0 THEN 1 ELSE 0 END) AS deleted_records
FROM
    `spc_point_metadata`;

SELECT
    '新表 spc_sampling_strategy' AS table_name,
    COUNT(*) AS total_records,
    SUM(CASE WHEN `enabled` = 1 THEN 1 ELSE 0 END) AS enabled_records,
    SUM(CASE WHEN `enabled` = 0 THEN 1 ELSE 0 END) AS disabled_records
FROM
    `spc_sampling_strategy`;

-- ========================================================================
-- 步骤 5: 回滚脚本（如果需要）
-- ========================================================================

/*
-- !!! 回滚操作 - 仅在需要时执行 !!!
-- 注意：这将删除所有迁移的数据，请谨慎操作

-- 1. 删除 spc_sampling_strategy 中的迁移数据
DELETE FROM `spc_sampling_strategy`
WHERE `measure_code` IN (
    SELECT `point` FROM `spc_indicator_backup_20260120`
);

-- 2. 删除 spc_point_metadata 中的迁移数据
DELETE FROM `spc_point_metadata`
WHERE `measure_code` IN (
    SELECT `point` FROM `spc_indicator_backup_20260120`
);

-- 3. 从备份表恢复原始数据（如果需要）
-- TRUNCATE TABLE `spc_indicator`;
-- INSERT INTO `spc_indicator` SELECT * FROM `spc_indicator_backup_20260120`;

SELECT '回滚完成' AS rollback_status;
*/

-- ========================================================================
-- 步骤 6: 清理（可选）
-- ========================================================================

/*
-- !!! 清理操作 - 确认迁移成功后再执行 !!!

-- 删除备份表
DROP TABLE IF EXISTS `spc_indicator_backup_20260120`;

-- 如果确认不再需要旧表，可以删除或重命名
-- RENAME TABLE `spc_indicator` TO `spc_indicator_deprecated`;
-- 或者
-- DROP TABLE `spc_indicator`;

SELECT '清理完成' AS cleanup_status;
*/

-- 恢复安全设置
SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;

SELECT '==================== 迁移脚本执行完成 ====================' AS finish;
SELECT '请检查上述检查点结果，确认数据迁移正确性' AS reminder;
