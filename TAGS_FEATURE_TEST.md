# Tags 字段功能测试文档

## 功能概述

为 `spc_point_metadata` 表新增 `tags` 字段，支持为每个 SPC 指标添加标签，多个标签用逗号分隔。

## 数据库修改

### 1. 执行数据库迁移脚本

```bash
# 在数据库中执行以下 SQL 脚本
mysql -u your_username -p your_database < sql/mysql/migration_add_tags_field.sql
```

或者手动执行：

```sql
ALTER TABLE `spc_point_metadata`
ADD COLUMN `tags` varchar(500) DEFAULT NULL COMMENT '标签（多个标签用逗号分隔）'
AFTER `deleted_id`;
```

### 2. 验证字段添加

```sql
DESC spc_point_metadata;
```

应该能看到新增的 `tags` 字段。

## API 测试

### 测试 1: 新增 SPC 指标（带 tags）

**接口**: `POST /api/v1/spcIndicator/add`

**请求示例**:

```json
{
  "indicatorName": "测试指标_tags_001",
  "classCode": "CLASS001",
  "systemCode": "SYS001",
  "indicatorLevel": "A",
  "measureCode": "MEASURE_001",
  "tags": ["生产", "质量", "关键指标"],
  "configType": "USER",
  "targetValue": 100.0,
  "uclValue": 110.0,
  "lclValue": 90.0,
  "enableRealtimeAlarm": true,
  "enableOfflineChart": true,
  "attributes": "{\"custom_field\": \"value\"}"
}
```

**预期结果**:
- 返回成功 (success: true)
- 数据库中该记录的 `tags` 字段值为 "生产,质量,关键指标"（自动转换为逗号分隔的字符串）

### 测试 2: 新增 SPC 指标（不带 tags）

**接口**: `POST /api/v1/spcIndicator/add`

**请求示例**:

```json
{
  "indicatorName": "测试指标_tags_002",
  "classCode": "CLASS001",
  "systemCode": "SYS001",
  "indicatorLevel": "B",
  "measureCode": "MEASURE_002",
  "configType": "USER",
  "targetValue": 100.0
}
```

**预期结果**:
- 返回成功 (success: true)
- 数据库中该记录的 `tags` 字段值为 NULL

### 测试 3: 更新 SPC 指标（添加 tags）

**接口**: `POST /api/v1/spcIndicator/update`

**请求示例**:

```json
{
  "id": 1,
  "classCode": "CLASS001",
  "tags": ["更新后的标签", "新标签", "测试"]
}
```

**预期结果**:
- 返回成功 (success: true)
- 数据库中该记录的 `tags` 字段值更新为 "更新后的标签,新标签,测试"（自动转换为逗号分隔的字符串）

### 测试 4: 更新 SPC 指标（清空 tags）

**接口**: `POST /api/v1/spcIndicator/update`

**请求示例**:

```json
{
  "id": 1,
  "classCode": "CLASS001",
  "tags": null
}
```

或者使用空数组:

```json
{
  "id": 1,
  "classCode": "CLASS001",
  "tags": []
}
```

**预期结果**:
- 返回成功 (success: true)
- 数据库中该记录的 `tags` 字段值更新为 NULL

### 测试 5: 查询 SPC 指标详情

**接口**: `POST /api/v1/spcIndicator/queryDetail`

**请求示例**:

```json
{
  "id": 1
}
```

**预期结果**:
- 返回的数据中包含 `tags` 字段（`List<String>` 类型）
- `tags` 字段是数据库逗号分隔字符串转换后的数组
- 返回的数据中包含 `samplingStrategies` 字段（`List<SamplingStrategyDTO>` 类型）
- `samplingStrategies` 包含该指标的所有采样策略配置

**返回示例**:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "indicatorName": "温度监控",
    "measureCode": "TEMP_001",
    "tags": ["生产", "温度", "关键指标"],
    "samplingStrategies": [
      {
        "id": 1,
        "measureCode": "TEMP_001",
        "periodS": 60,
        "periodLabel": "1m",
        "strategyType": "periodic",
        ...
      }
    ],
    ...
  }
}
```

### 测试 6: 通用查询 SPC 指标详情

**接口**: `POST /api/v1/spcIndicator/commonQueryDetail`

**请求示例**:

```json
{
  "indicatorName": "温度监控"
}
```

或:

```json
{
  "jobId": "TEMP_001_1"
}
```

**预期结果**:
- 返回的数据中包含 `tags` 字段（`List<String>` 类型）
- 返回的数据中包含 `samplingStrategies` 字段（`List<SamplingStrategyDTO>` 类型）
- 数据格式与 queryDetail 接口相同

## 数据库验证

### 查询带有 tags 的记录

```sql
SELECT id, indicator_name, tags, created_at
FROM spc_point_metadata
WHERE tags IS NOT NULL AND deleted_id = 0
ORDER BY created_at DESC
LIMIT 10;
```

### 查询特定标签的记录

```sql
SELECT id, indicator_name, tags
FROM spc_point_metadata
WHERE tags LIKE '%生产%' AND deleted_id = 0;
```

## 测试用例清单

- [x] 数据库表结构修改完成
- [ ] 新增指标时可以设置 tags（数组格式）
- [ ] 新增指标时不设置 tags（tags 为 NULL）
- [ ] 更新指标时可以修改 tags（数组格式）
- [ ] 更新指标时可以清空 tags（空数组或 null）
- [ ] 查询指标详情时返回 tags 字段（List<String> 格式）
- [ ] 查询指标详情时返回 samplingStrategies 字段（List<SamplingStrategyDTO> 格式）
- [ ] tags 字段支持多个标签（API 接收数组，数据库存储逗号分隔）
- [ ] tags 字段长度不超过 500 字符（转换后的字符串长度）
- [ ] queryDetail 接口正确转换和返回 tags 和 samplingStrategies
- [ ] commonQueryDetail 接口正确转换和返回 tags 和 samplingStrategies

## 注意事项

1. **tags 字段格式**:
   - API 接口接收 `List<String>` 类型的数组，例如：`["标签1", "标签2", "标签3"]`
   - 数据库存储时自动转换为逗号分隔的字符串：`"标签1,标签2,标签3"`
2. **字段长度限制**: tags 字段最大长度为 500 字符（转换后的字符串长度）
3. **可选字段**: tags 字段为可选，可以为 `null` 或空数组 `[]`，两者都会在数据库中存储为 NULL
4. **向后兼容**: 旧的 API 调用（不包含 tags）仍然可以正常工作
5. **自动转换**: Service 层会自动将 `List<String>` 转换为逗号分隔的字符串存入数据库

## API 使用示例

### 新增指标时添加标签

```json
POST /api/v1/spcIndicator/add
{
  "indicatorName": "温度监控",
  "classCode": "PROD",
  "systemCode": "SYS001",
  "indicatorLevel": "A",
  "measureCode": "TEMP_001",
  "tags": ["生产", "温度", "关键指标"],
  "configType": "USER"
}
```

**数据库存储**: `tags` 字段值为 `"生产,温度,关键指标"`

### 更新指标标签

```json
POST /api/v1/spcIndicator/update
{
  "id": 1,
  "classCode": "PROD",
  "tags": ["生产", "温度", "已优化"]
}
```

**数据库存储**: `tags` 字段值更新为 `"生产,温度,已优化"`

### 清空标签

使用空数组:
```json
POST /api/v1/spcIndicator/update
{
  "id": 1,
  "classCode": "PROD",
  "tags": []
}
```

或使用 null:
```json
POST /api/v1/spcIndicator/update
{
  "id": 1,
  "classCode": "PROD",
  "tags": null
}
```

**数据库存储**: `tags` 字段值为 `NULL`

## 代码修改清单

1. ✅ 数据库表结构修改（`sql/mysql/init_mysql.sql`）
2. ✅ 数据库迁移脚本（`sql/mysql/migration_add_tags_field.sql`）
3. ✅ Entity 类修改（`SpcPointMetadataDO.java`）
4. ✅ Mapper XML 修改（`SpcPointMetadataMapper.xml`）
5. ✅ DTO 类修改（`AddSpcPointMetaReqDTO.java`, `UpdateSpcPointMetaReqDTO.java` - tags 改为 List<String>）
6. ✅ Service 实现类修改（`SpcPointMetaDataServiceImpl.java` - 添加 List 到 String 转换）
7. ✅ 项目编译成功

## 测试环境要求

- 数据库已执行迁移脚本
- 应用服务已重启
- 测试数据已准备

## 测试结果记录

| 测试项 | 状态 | 备注 |
|--------|------|------|
| 数据库迁移 | ⏳ 待测试 | |
| 新增接口-带tags | ⏳ 待测试 | |
| 新增接口-不带tags | ⏳ 待测试 | |
| 更新接口-修改tags | ⏳ 待测试 | |
| 更新接口-清空tags | ⏳ 待测试 | |
| 查询接口-返回tags | ⏳ 待测试 | |

---

**测试完成日期**: ___________

**测试人员**: ___________

**测试结果**: ___________
