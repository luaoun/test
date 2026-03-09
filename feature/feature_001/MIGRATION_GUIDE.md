# SPC 数据迁移指南

## 概述

本文档说明如何将数据从旧表 `spc_indicator` 迁移到新的表结构：
- `spc_point_metadata` - SPC 指标元数据表
- `spc_sampling_strategy` - SPC 采样策略表

## 迁移文件说明

1. **migration_spc_indicator_to_new_tables.sql** - 主迁移脚本
2. **migration_update_measure_name.sql** - 补充脚本（更新 measure_name）
3. **MIGRATION_GUIDE.md** - 本文档

## 字段映射关系

### spc_indicator → spc_point_metadata

| 旧表字段 (spc_indicator) | 新表字段 (spc_point_metadata) | 说明 |
|-------------------------|-------------------------------|------|
| id | id | 主键，保持不变 |
| job_id | job_id | 作业ID |
| indicator_name | indicator_name | 指标名称 |
| class_code | class_code | 课室编码 |
| system_code | system_code | 系统编码 |
| indicator_level | indicator_level | 指标级别 |
| **point** | **measure_code** | **关键映射：点位编码 → 指标编码** |
| point_unit | unit | 单位 |
| start_value | y_axis_min | Y轴最小值 |
| end_value | y_axis_max | Y轴最大值 |
| step | y_axis_step | Y轴步长 |
| target_value | target_value | 目标值 |
| ucl_value | ucl_value | 控制线上限 |
| lcl_value | lcl_value | 控制线下限 |
| uwl_value | uwl_value | 警告线上限 |
| lwl_value | lwl_value | 警告线下限 |
| usl_value | usl_value | 规格线上限 |
| lsl_value | lsl_value | 规格线下限 |
| u3l_value | u3l_value | 3σ上限 |
| l3l_value | l3l_value | 3σ下限 |
| ocap | ocap_template_id | OCAP模板ID |
| status (bit) | enable_realtime (tinyint) | 是否启用实时告警 |
| deleted (bit) | deleted_id (bigint) | 删除标识 (0=未删除, id=已删除) |
| creator | created_by | 创建人 |
| create_time | created_at | 创建时间 |
| update_time | updated_at | 更新时间 |
| fac_code | fac_code | 厂区编码 |
| - | measure_name | 需要从指标库补充 |
| - | data_type | 默认值: 'analog' |
| - | enable_offline | 默认值: 1 |
| - | enabled_spc_controlled | 默认值: 1 |

### spc_indicator → spc_sampling_strategy

为每个指标创建一个默认的采样策略：

| 字段 | 默认值 | 说明 |
|-----|--------|------|
| measure_code | spc_indicator.point | 从旧表的 point 字段获取 |
| period_s | 60 | 60秒采样周期 |
| period_label | '1m' | 1分钟标签 |
| strategy_type | 'periodic' | 周期性策略 |
| window_type | 'tumble' | 滚动窗口 |
| window_size_s | 60 | 60秒窗口 |
| features | 'avg,max,min,std' | 统计特征 |
| computation_mode | 'realtime' | 实时计算 |
| value_mode | 'avg' | 平均值模式 |
| enabled | spc_indicator.status | 继承原状态 |

## 迁移步骤

### 前置条件

1. 确保有数据库的完整权限
2. 建议在非生产环境先进行测试
3. 通知相关人员，计划好迁移时间窗口

### 步骤 1: 备份数据

```sql
-- 执行迁移脚本会自动创建备份表
-- 备份表名: spc_indicator_backup_20260120
```

### 步骤 2: 执行主迁移脚本

```bash
# 方式1: 使用 MySQL 客户端
mysql -h localhost -P 3307 -u root -p ifp-operation < migration_spc_indicator_to_new_tables.sql

# 方式2: 使用 Docker 容器
docker run --rm -i mysql:8.0 mysql -h host.docker.internal -P 3307 -uroot -proot ifp-operation < migration_spc_indicator_to_new_tables.sql
```

### 步骤 3: 检查迁移结果

脚本会自动执行以下检查点：

- ✓ 检查点 1: spc_point_metadata 数据量是否一致
- ✓ 检查点 2: spc_sampling_strategy 数据量是否正常
- ✓ 检查点 3: 是否存在孤立的采样策略
- ✓ 检查点 4: 关键字段完整性检查

查看输出结果，确认所有检查点都显示 ✓

### 步骤 4: 补充 measure_name（可选但建议）

```bash
# 如果 px-ifp-base 库在同一实例，直接执行方案1
mysql -h localhost -P 3307 -u root -p ifp-operation < migration_update_measure_name.sql
```

**如果没有跨库权限：**

1. 从 px-ifp-base 库导出指标数据：
```sql
SELECT measure_code, measure_name, measure_unit
FROM `px-ifp-base`.mea_point
WHERE measure_code IN (SELECT DISTINCT measure_code FROM `ifp-operation`.spc_point_metadata);
```

2. 将数据导入临时表并更新（参考 migration_update_measure_name.sql 方案2）

### 步骤 5: 验证应用程序

1. 重启 Spring Boot 应用
2. 测试以下功能：
   - 查询 SPC 指标列表 `/api/v1/spcIndicator/queryList`
   - 查询指标详情 `/api/v1/spcIndicator/queryDetail`
   - 新增指标 `/api/v1/spcIndicator/add`
   - 更新指标 `/api/v1/spcIndicator/update`
   - 删除指标 `/api/v1/spcIndicator/delete`

### 步骤 6: 清理旧数据（确认无误后）

```sql
-- 仅在确认迁移成功、应用运行正常后执行

-- 删除备份表
DROP TABLE IF EXISTS `spc_indicator_backup_20260120`;

-- 重命名或删除旧表
RENAME TABLE `spc_indicator` TO `spc_indicator_deprecated`;
-- 或者
-- DROP TABLE `spc_indicator`;
```

## 回滚方案

如果迁移出现问题，可以执行回滚：

```sql
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

-- 3. 从备份恢复（如果需要）
TRUNCATE TABLE `spc_indicator`;
INSERT INTO `spc_indicator` SELECT * FROM `spc_indicator_backup_20260120`;
```

## 常见问题

### Q1: 迁移后发现部分记录的 measure_name 为空？

**A:** 这是正常的，需要执行补充脚本 `migration_update_measure_name.sql` 从指标库获取。

### Q2: 迁移后应用启动报错？

**A:** 检查以下几点：
1. Redis 缓存是否需要清理：`FLUSHDB`
2. 应用配置文件是否正确
3. 数据库连接是否正常

### Q3: 如何确认迁移是否成功？

**A:** 执行以下查询验证：

```sql
-- 检查数据量
SELECT
    (SELECT COUNT(*) FROM spc_indicator WHERE deleted = 0) AS old_count,
    (SELECT COUNT(*) FROM spc_point_metadata WHERE deleted_id = 0) AS new_count,
    (SELECT COUNT(*) FROM spc_sampling_strategy) AS strategy_count;

-- 抽查几条记录对比
SELECT * FROM spc_indicator WHERE id = 1;
SELECT * FROM spc_point_metadata WHERE id = 1;
SELECT * FROM spc_sampling_strategy WHERE measure_code = (SELECT point FROM spc_indicator WHERE id = 1);
```

### Q4: 旧表中的 uacl_value 和 lacl_value 字段去哪了？

**A:** 新表 `spc_point_metadata` 没有这两个字段，如果业务需要可以存储在 `attributes` JSON 字段中。

## 注意事项

1. **字段类型变化**：
   - `status` (bit) → `enable_realtime` (tinyint)
   - `deleted` (bit) → `deleted_id` (bigint)

2. **关键字段映射**：
   - 旧表的 `point` → 新表的 `measure_code`（这是最重要的映射关系）

3. **新增字段**：
   - `measure_name` 需要从指标库补充
   - `enabled_spc_controlled` 默认为 1（启用）
   - `enabled_sampling_status` 默认为 0（未启用采样，需手动配置）

4. **外键约束**：
   - `spc_sampling_strategy.measure_code` 外键引用 `spc_point_metadata.measure_code`
   - 删除 point_metadata 会级联删除对应的 sampling_strategy

5. **唯一索引**：
   - `spc_point_metadata.indicator_name` 需要唯一
   - `spc_point_metadata.measure_code` 需要唯一
   - `spc_sampling_strategy (measure_code, period_s)` 组合唯一

## 迁移时间估算

- 1000 条记录：< 1 分钟
- 10000 条记录：< 5 分钟
- 100000 条记录：< 30 分钟

实际时间取决于服务器性能和数据量。

## 联系支持

如有问题，请联系开发团队或提交 Issue。

---

**最后更新**: 2026-01-20
**版本**: 1.0.0
