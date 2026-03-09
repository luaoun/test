# SamplingStrategyDTO Features 字段修改

## 修改概述

将 `SamplingStrategyDTO` 和 `SaveOrUpdateSamplingStrategyDTO` 中的 `features` 字段从 `String` 类型改为 `List<String>` 类型，以提供更好的 API 体验。

## 字段映射关系

| 数据库表 | 数据库字段 | Entity 类字段 | DTO 类字段 |
|---------|-----------|--------------|-----------|
| `spc_sampling_strategy` | `features` (varchar) | `SpcSamplingStrategy.features` | `SamplingStrategyDTO.features` (List<String>) |

**数据格式转换**:
- API 接收/返回: `["avg", "max", "min", "std"]` (List<String>)
- 数据库存储: `"avg,max,min,std"` (逗号分隔的字符串)

## 修改的文件

### 1. DTO 层修改

#### 文件 1: `src/main/java/com/px/ifp/spc/dto/manager/request/SamplingStrategyDTO.java`

**修改内容**:

添加导入:
```java
import java.util.List;
```

修改字段:
```java
// 修改前
@Schema(description = "统计特征（逗号分隔）")
private String features;

// 修改后
@Schema(description = "统计特征列表")
private List<String> features;
```

#### 文件 2: `src/main/java/com/px/ifp/spc/dto/manager/request/SaveOrUpdateSamplingStrategyDTO.java`

**修改内容**:

添加导入:
```java
import java.util.List;
```

修改字段:
```java
// 修改前
@Schema(description = "统计特征（逗号分隔）")
private String features;

// 修改后
@Schema(description = "统计特征列表")
private List<String> features;
```

### 2. Service 层修改 - 保存时转换

#### 文件: `src/main/java/com/px/ifp/spc/service/impl/SpcSamplingStrategyServiceImpl.java`

**添加导入**:
```java
import cn.hutool.core.util.StrUtil;
```

**修改位置 1 - saveOrUpdate 方法 (第 39 行)**:

```java
SpcSamplingStrategy entity = ObjectConvertUtil.convert(reqDTO, SpcSamplingStrategy.class);

// 将 features List<String> 转换为逗号分隔的字符串
if (reqDTO.getFeatures() != null && !reqDTO.getFeatures().isEmpty()) {
    entity.setFeatures(String.join(",", reqDTO.getFeatures()));
} else {
    entity.setFeatures(null);
}
```

**修改位置 2 - batchSaveOrUpdate 方法 (第 78 行)**:

```java
// 转换为实体对象
SpcSamplingStrategy entity = ObjectConvertUtil.convert(strategyDTO, SpcSamplingStrategy.class);
// 设置指标编码
entity.setMeasureCode(measureCode);

// 将 features List<String> 转换为逗号分隔的字符串
if (strategyDTO.getFeatures() != null && !strategyDTO.getFeatures().isEmpty()) {
    entity.setFeatures(String.join(",", strategyDTO.getFeatures()));
} else {
    entity.setFeatures(null);
}
```

### 3. Service 层修改 - 查询时转换

#### 文件: `src/main/java/com/px/ifp/spc/service/impl/SpcPointMetaDataServiceImpl.java`

**修改位置**: queryDetail 和 querySpcIndicatorDetail 方法中的查询采样策略逻辑

```java
// 查询采样策略配置
LambdaQueryWrapper<com.px.ifp.spc.entity.SpcSamplingStrategy> strategyWrapper = new LambdaQueryWrapper<>();
strategyWrapper.eq(com.px.ifp.spc.entity.SpcSamplingStrategy::getMeasureCode, spcIndicatorDO.getMeasureCode());
List<com.px.ifp.spc.entity.SpcSamplingStrategy> strategies = spcSamplingStrategyService.list(strategyWrapper);
if (CollectionUtil.isNotEmpty(strategies)) {
    List<SamplingStrategyDTO> samplingStrategies = strategies.stream()
            .map(strategy -> {
                SamplingStrategyDTO dto = ObjectConvertUtil.convert(strategy, SamplingStrategyDTO.class);
                // 将 features 字符串转换为 List<String>
                if (StrUtil.isNotBlank(strategy.getFeatures())) {
                    dto.setFeatures(Arrays.asList(strategy.getFeatures().split(",")));
                }
                return dto;
            })
            .collect(Collectors.toList());
    respDTO.setSamplingStrategies(samplingStrategies);
}
```

## API 使用示例

### 新增/更新采样策略

**请求示例**:
```json
POST /api/v1/spcIndicator/add
{
  "indicatorName": "温度监控",
  "measureCode": "TEMP_001",
  "samplingStrategies": [
    {
      "periodS": 60,
      "periodLabel": "1m",
      "strategyType": "periodic",
      "features": ["avg", "max", "min", "std"],
      "enabled": true
    }
  ]
}
```

**数据库存储**:
- `features` 字段值: `"avg,max,min,std"`

### 查询指标详情

**接口**: `POST /api/v1/spcIndicator/queryDetail`

**返回示例**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "indicatorName": "温度监控",
    "samplingStrategies": [
      {
        "id": 1,
        "periodS": 60,
        "periodLabel": "1m",
        "strategyType": "periodic",
        "features": ["avg", "max", "min", "std"],
        "enabled": true
      }
    ]
  }
}
```

## 数据转换逻辑

### 保存时: List<String> → String

```java
// API 接收
features: ["avg", "max", "min", "std"]

// 转换逻辑
if (dto.getFeatures() != null && !dto.getFeatures().isEmpty()) {
    entity.setFeatures(String.join(",", dto.getFeatures()));
}

// 数据库存储
features: "avg,max,min,std"
```

### 查询时: String → List<String>

```java
// 数据库查询
features: "avg,max,min,std"

// 转换逻辑
if (StrUtil.isNotBlank(entity.getFeatures())) {
    dto.setFeatures(Arrays.asList(entity.getFeatures().split(",")));
}

// API 返回
features: ["avg", "max", "min", "std"]
```

## 空值处理

- **API 接收 `[]` (空数组)** → 数据库存储 `NULL`
- **API 接收 `null`** → 数据库存储 `NULL`
- **数据库 `NULL`** → API 返回 `null` (不设置)
- **数据库空字符串 `""`** → API 返回 `null` (不设置)

## 影响的接口

修改后，以下接口支持 `features` 字段的 List 格式：

### 新增/更新接口
1. `POST /api/v1/spcIndicator/add` - 新增指标及采样策略
2. `POST /api/v1/spcIndicator/update` - 更新指标及采样策略
3. `POST /api/v1/sampling-strategy/saveOrUpdate` - 保存或更新采样策略

### 查询接口
1. `POST /api/v1/spcIndicator/queryDetail` - 查询指标详情
2. `POST /api/v1/spcIndicator/commonQueryDetail` - 通用查询指标详情

## 常用特征值

统计特征常用值（仅供参考）：
- `avg` - 平均值
- `max` - 最大值
- `min` - 最小值
- `std` - 标准差
- `median` - 中位数
- `sum` - 总和
- `count` - 计数

## 编译验证

✅ **BUILD SUCCESS** - 所有代码修改通过编译

## 向后兼容性

- ✅ 字段为可选，不影响现有功能
- ✅ 数据库字段类型不变，仅 API 层改为 List 格式
- ✅ 自动转换逻辑确保数据一致性

## 测试建议

1. **新增测试**: 测试 features 数组正确保存为逗号分隔字符串
2. **更新测试**: 测试修改 features 数组
3. **查询测试**: 验证返回的 features 为数组格式
4. **空值测试**: 测试空数组和 null 的处理
5. **边界测试**: 测试单个特征值、多个特征值

---

**修改完成日期**: 2026-01-22

**修改人员**: Claude

**编译状态**: ✅ 成功
