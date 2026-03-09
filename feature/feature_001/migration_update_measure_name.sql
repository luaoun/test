-- ========================================================================
-- SPC 数据迁移后补充脚本 - 更新 measure_name
--
-- 作者: Claude Code
-- 日期: 2026-01-20
-- 说明:
--   迁移完成后，需要从指标库（px-ifp-base 库的 mea_point 表）
--   补充 measure_name 字段
--
-- 前置条件:
--   1. 已执行 migration_spc_indicator_to_new_tables.sql
--   2. 需要有访问 px-ifp-base 数据库的权限
-- ========================================================================

USE `ifp-operation`;

-- 方案 1: 如果 px-ifp-base 库和 ifp-operation 库在同一个 MySQL 实例
-- 直接使用跨库 JOIN 更新

/*
UPDATE `ifp-operation`.`spc_point_metadata` pm
INNER JOIN `px-ifp-base`.`mea_point` mp
    ON pm.`measure_code` = mp.`measure_code`
SET
    pm.`measure_name` = mp.`measure_name`,
    pm.`unit` = COALESCE(pm.`unit`, mp.`measure_unit`),  -- 如果单位为空，从指标库补充
    pm.`updated_at` = NOW(),
    pm.`updated_by` = 'migration_update'
WHERE
    pm.`measure_name` IS NULL;

SELECT CONCAT('已更新 ', ROW_COUNT(), ' 条记录的 measure_name') AS update_result;
*/

-- 方案 2: 如果没有跨库权限，需要先导出 mea_point 数据，然后导入临时表
--
-- 步骤说明：
-- 1. 在 px-ifp-base 库执行以下查询导出数据：
--    SELECT measure_code, measure_name, measure_unit
--    FROM mea_point
--    WHERE measure_code IN (SELECT DISTINCT measure_code FROM `ifp-operation`.spc_point_metadata);
--
-- 2. 将结果导入到临时表（见下方脚本）

-- 创建临时表
DROP TEMPORARY TABLE IF EXISTS `temp_mea_point`;
CREATE TEMPORARY TABLE `temp_mea_point` (
    `measure_code` VARCHAR(64) PRIMARY KEY,
    `measure_name` VARCHAR(128),
    `measure_unit` VARCHAR(20)
);

-- 示例：手动插入数据（实际使用时应该用 LOAD DATA 或其他批量导入方式）
/*
INSERT INTO `temp_mea_point` (`measure_code`, `measure_name`, `measure_unit`) VALUES
('TEMP001', '温度传感器1', '℃'),
('PRESS001', '压力传感器1', 'MPa');
-- ... 添加更多数据
*/

-- 使用临时表更新 spc_point_metadata
/*
UPDATE `spc_point_metadata` pm
INNER JOIN `temp_mea_point` tmp
    ON pm.`measure_code` = tmp.`measure_code`
SET
    pm.`measure_name` = tmp.`measure_name`,
    pm.`unit` = COALESCE(pm.`unit`, tmp.`measure_unit`),
    pm.`updated_at` = NOW(),
    pm.`updated_by` = 'migration_update'
WHERE
    pm.`measure_name` IS NULL;

SELECT CONCAT('已更新 ', ROW_COUNT(), ' 条记录的 measure_name') AS update_result;
*/

-- 方案 3: 通过应用程序批量更新
-- 如果以上两种方案都不适用，建议在 Java 代码中通过 Feign 调用指标库服务
-- 批量获取 measure_name 并更新到数据库

-- 检查未更新的记录
SELECT
    '检查 measure_name 为空的记录' AS check_point,
    COUNT(*) AS null_count,
    CASE
        WHEN COUNT(*) = 0 THEN '✓ 所有记录都已补充 measure_name'
        ELSE CONCAT('✗ 还有 ', COUNT(*), ' 条记录的 measure_name 为空')
    END AS result
FROM
    `spc_point_metadata`
WHERE
    `measure_name` IS NULL
    AND `deleted_id` = 0;

-- 查看需要补充 measure_name 的记录列表
SELECT
    `id`,
    `measure_code`,
    `indicator_name`,
    `job_id`
FROM
    `spc_point_metadata`
WHERE
    `measure_name` IS NULL
    AND `deleted_id` = 0
ORDER BY
    `id`
LIMIT 20;

SELECT '==================== 补充脚本完成 ====================' AS finish;
