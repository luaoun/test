# Tags 字段功能修改摘要

## 修改概述

为 SPC 指标管理系统添加标签（tags）功能，支持为每个指标添加多个标签。

### 设计要点

- **API 层**: 接收 `List<String>` 类型的标签数组
- **存储层**: 数据库中存储为逗号分隔的字符串（varchar(500)）
- **转换逻辑**: Service 层自动处理 List 到 String 的转换

## 详细修改清单

### 1. 数据库层修改

#### 文件: `sql/mysql/init_mysql.sql`

在 `spc_point_metadata` 表的 CREATE TABLE 语句中添加字段：

```sql
`tags` varchar(500) DEFAULT NULL COMMENT '标签（多个标签用逗号分隔）'
```

位置：在 `deleted_id` 字段之后

#### 文件: `sql/mysql/migration_add_tags_field.sql` (新建)

数据库迁移脚本，用于在现有数据库中添加 tags 字段：

```sql
ALTER TABLE `spc_point_metadata`
ADD COLUMN `tags` varchar(500) DEFAULT NULL COMMENT '标签（多个标签用逗号分隔）'
AFTER `deleted_id`;
```

### 2. Entity 层修改

#### 文件: `src/main/java/com/px/ifp/spc/entity/SpcPointMetadataDO.java`

**位置**: 第 164 行

**添加字段**:
```java
@TableField("tags")
private String tags;  // 标签（多个标签用逗号分隔）
```

### 3. Mapper 层修改

#### 文件: `src/main/resources/mapper/SpcPointMetadataMapper.xml`

**修改 1 - INSERT 语句** (第 104 行):

在字段列表中添加 `tags`，在值列表中添加 `#{tags}`：

```xml
<insert id="insert" parameterType="com.px.ifp.spc.entity.SpcPointMetadataDO" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO spc_point_metadata (
        ..., tags, attributes, ...
    ) VALUES (
        ..., #{tags}, #{attributes}, ...
    )
</insert>
```

**修改 2 - UPDATE 语句** (第 152 行):

在 update 的 set 块中添加：

```xml
<if test="tags != null">tags = #{tags},</if>
```

### 4. DTO 层修改

#### 文件: `src/main/java/com/px/ifp/spc/dto/manager/request/AddSpcPointMetaReqDTO.java`

**位置**: 第 159 行

**修改**:
```java
@Schema(description = "标签列表")
private List<String> tags;
```

#### 文件: `src/main/java/com/px/ifp/spc/dto/manager/request/UpdateSpcPointMetaReqDTO.java`

**位置**: 第 159 行

**修改**:
```java
@Schema(description = "标签列表")
private List<String> tags;
```

### 5. Service 层修改

#### 文件: `src/main/java/com/px/ifp/spc/service/impl/SpcPointMetaDataServiceImpl.java`

**修改 1 - add 方法** (第 167 行):

添加 List<String> 到 String 的转换逻辑：

```java
// 将标签列表转换为逗号分隔的字符串
if (reqDTO.getTags() != null && !reqDTO.getTags().isEmpty()) {
    spcIndicatorDO.setTags(String.join(",", reqDTO.getTags()));
} else {
    spcIndicatorDO.setTags(null);
}
```

**修改 2 - update 方法** (第 236 行):

添加相同的转换逻辑：

```java
// 将标签列表转换为逗号分隔的字符串
if (reqDTO.getTags() != null && !reqDTO.getTags().isEmpty()) {
    spcIndicatorDO.setTags(String.join(",", reqDTO.getTags()));
} else {
    spcIndicatorDO.setTags(null);
}
```

### 6. Controller 层

#### 文件: `src/main/java/com/px/ifp/spc/web/legacy/SpcIndicatorController.java`

**无需修改** - Controller 使用的 DTO 已经更新，自动支持新的 tags 字段

## API 接口变更

### 新增指标接口

**接口**: `POST /api/v1/spcIndicator/add`

**新增参数**:
- `tags` (可选): `List<String>` - 标签列表

**示例**:
```json
{
  "indicatorName": "温度监控",
  "tags": ["生产", "温度", "关键指标"],
  ...其他字段
}
```

### 更新指标接口

**接口**: `POST /api/v1/spcIndicator/update`

**新增参数**:
- `tags` (可选): `List<String>` - 标签列表

**示例**:
```json
{
  "id": 1,
  "tags": ["生产", "温度", "已优化"],
  ...其他字段
}
```

### 查询接口

查询接口无需修改，会自动返回 tags 字段（数据库中的逗号分隔字符串）

## 数据转换说明

### API → 数据库

- API 接收: `["生产", "质量", "关键指标"]` (List<String>)
- 数据库存储: `"生产,质量,关键指标"` (String)

### 空值处理

- API 接收: `[]` (空数组) → 数据库存储: `NULL`
- API 接收: `null` → 数据库存储: `NULL`

## 编译验证

✅ 项目编译成功，所有修改语法正确

```bash
mvn clean compile -DskipTests
```

输出: `BUILD SUCCESS`

## 向后兼容性

- ✅ 旧的 API 调用（不包含 tags 字段）仍然正常工作
- ✅ tags 为可选字段，不影响现有功能
- ✅ 数据库字段默认值为 NULL，不影响现有数据

## 测试建议

1. **功能测试**: 参考 `TAGS_FEATURE_TEST.md` 文档
2. **集成测试**: 测试 add/update/query 接口
3. **边界测试**:
   - 空数组/null 值
   - 单个标签
   - 多个标签
   - 超长标签（接近 500 字符限制）
4. **兼容性测试**: 不带 tags 的旧接口调用

## 部署步骤

1. 执行数据库迁移脚本:
   ```bash
   mysql -u username -p database_name < sql/mysql/migration_add_tags_field.sql
   ```

2. 部署新版本代码

3. 重启应用服务

4. 验证功能是否正常

## 查询接口返回值增强

### 修改的接口

1. **POST /api/v1/spcIndicator/queryDetail** - 查询指标详情
2. **POST /api/v1/spcIndicator/commonQueryDetail** - 通用查询指标详情

### 返回值新增字段

#### 1. tags (List<String>)

- 将数据库中的逗号分隔字符串自动转换为字符串数组
- 数据库存储: `"生产,温度,关键指标"`
- API 返回: `["生产", "温度", "关键指标"]`

#### 2. samplingStrategies (List<SamplingStrategyDTO>)

- 查询该指标对应的所有采样策略配置
- 包含策略的完整配置信息（周期、类型、窗口等）

### 文件修改

#### 文件: `src/main/java/com/px/ifp/spc/dto/manager/response/QuerySpcIndicatorDetailRespDTO.java`

**添加字段**:

```java
@Schema(description = "标签列表")
private List<String> tags;

@Schema(description = "采样策略配置列表")
private List<SamplingStrategyDTO> samplingStrategies;
```

**添加导入**:

```java
import com.px.ifp.spc.dto.manager.request.SamplingStrategyDTO;
import java.util.List;
```

#### 文件: `src/main/java/com/px/ifp/spc/service/impl/SpcPointMetaDataServiceImpl.java`

**修改方法**: `queryDetail(IdReqDTO reqDTO)` - 第 354 行

**修改方法**: `querySpcIndicatorDetail(QuerySpcIndicatorDTO reqDTO)` - 第 392 行

**添加逻辑**:

```java
// 将 tags 字符串转换为 List<String>
if (StrUtil.isNotBlank(spcIndicatorDO.getTags())) {
    respDTO.setTags(Arrays.asList(spcIndicatorDO.getTags().split(",")));
}

// 查询采样策略配置
LambdaQueryWrapper<com.px.ifp.spc.entity.SpcSamplingStrategy> strategyWrapper = new LambdaQueryWrapper<>();
strategyWrapper.eq(com.px.ifp.spc.entity.SpcSamplingStrategy::getMeasureCode, spcIndicatorDO.getMeasureCode());
List<com.px.ifp.spc.entity.SpcSamplingStrategy> strategies = spcSamplingStrategyService.list(strategyWrapper);
if (CollectionUtil.isNotEmpty(strategies)) {
    List<SamplingStrategyDTO> samplingStrategies = strategies.stream()
            .map(strategy -> ObjectConvertUtil.convert(strategy, SamplingStrategyDTO.class))
            .collect(Collectors.toList());
    respDTO.setSamplingStrategies(samplingStrategies);
}
```

### 返回值示例

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
        "windowType": "tumble",
        "windowSizeS": 60,
        "features": "avg,max,min,std",
        "enabled": true
      }
    ],
    "targetValue": 100.0,
    "uclValue": 110.0,
    "lclValue": 90.0,
    ...
  }
}
```

## SpcAnalysisDTO.status 字段映射修复

### 问题

`SpcAnalysisDTO` 中的 `status` 属性需要对应数据库表 `spc_point_metadata` 的 `enable_realtime_alarm` 字段。

### 修改内容

在 `SpcPointMetaDataServiceImpl.java` 的三个方法中添加手动映射：

1. **queryPointMetaList** (第 514 行)
2. **querySpcAnalysis** (第 645 行)
3. **queryDetailByJobId** (第 1298 行)

**映射逻辑**:
```java
// 手动映射 enableRealtimeAlarm 到 status
for (int i = 0; i < spcAnalysisDTOList.size(); i++) {
    spcAnalysisDTOList.get(i).setStatus(spcIndicatorDOList.get(i).getEnableRealtimeAlarm());
}
```

详细说明请参考: `SPC_ANALYSIS_STATUS_MAPPING_FIX.md`

## SamplingStrategyDTO.features 字段修改

### 问题

`SamplingStrategyDTO` 中的 `features` 字段需要从 `String` 改为 `List<String>`，提供更好的 API 体验。

### 修改内容

#### DTO 层修改

1. `SamplingStrategyDTO.java` - features 改为 `List<String>`
2. `SaveOrUpdateSamplingStrategyDTO.java` - features 改为 `List<String>`

#### Service 层修改

**保存时转换 (List → String)**:
- `SpcSamplingStrategyServiceImpl.saveOrUpdate` 方法
- `SpcSamplingStrategyServiceImpl.batchSaveOrUpdate` 方法

**查询时转换 (String → List)**:
- `SpcPointMetaDataServiceImpl.queryDetail` 方法
- `SpcPointMetaDataServiceImpl.querySpcIndicatorDetail` 方法

**转换逻辑**:
```java
// 保存: List → String
if (dto.getFeatures() != null && !dto.getFeatures().isEmpty()) {
    entity.setFeatures(String.join(",", dto.getFeatures()));
}

// 查询: String → List
if (StrUtil.isNotBlank(entity.getFeatures())) {
    dto.setFeatures(Arrays.asList(entity.getFeatures().split(",")));
}
```

详细说明请参考: `SAMPLING_STRATEGY_FEATURES_CHANGE.md`

## 文件修改统计

- 新增文件: 4 个
  - `sql/mysql/migration_add_tags_field.sql`
  - `TAGS_FEATURE_TEST.md`
  - `SPC_ANALYSIS_STATUS_MAPPING_FIX.md`
  - `SAMPLING_STRATEGY_FEATURES_CHANGE.md` ⭐ **新增**

- 修改文件: 10 个
  - `sql/mysql/init_mysql.sql`
  - `src/main/java/com/px/ifp/spc/entity/SpcPointMetadataDO.java`
  - `src/main/resources/mapper/SpcPointMetadataMapper.xml`
  - `src/main/java/com/px/ifp/spc/dto/manager/request/AddSpcPointMetaReqDTO.java`
  - `src/main/java/com/px/ifp/spc/dto/manager/request/UpdateSpcPointMetaReqDTO.java`
  - `src/main/java/com/px/ifp/spc/dto/manager/request/SamplingStrategyDTO.java` ⭐ **新增**
  - `src/main/java/com/px/ifp/spc/dto/manager/request/SaveOrUpdateSamplingStrategyDTO.java` ⭐ **新增**
  - `src/main/java/com/px/ifp/spc/dto/manager/response/QuerySpcIndicatorDetailRespDTO.java`
  - `src/main/java/com/px/ifp/spc/service/impl/SpcPointMetaDataServiceImpl.java` (查询转换 + status映射 + features转换)
  - `src/main/java/com/px/ifp/spc/service/impl/SpcSamplingStrategyServiceImpl.java` ⭐ **新增**

---

**修改完成日期**: 2026-01-22

**修改人员**: Claude

**编译状态**: ✅ 成功
