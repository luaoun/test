# SpcAnalysisDTO Status 字段映射修复

## 问题描述

`SpcAnalysisDTO` 中的 `status` 属性需要对应数据库表 `spc_point_metadata` 的 `enable_realtime_alarm` 字段，但由于字段名不同，`ObjectConvertUtil.convertList` 无法自动映射。

## 字段映射关系

| 数据库表 | 数据库字段 | Entity 类字段 | DTO 类字段 |
|---------|-----------|--------------|-----------|
| `spc_point_metadata` | `enable_realtime_alarm` | `SpcPointMetadataDO.enableRealtimeAlarm` | `SpcAnalysisDTO.status` |

## 修改内容

### 文件: `src/main/java/com/px/ifp/spc/service/impl/SpcPointMetaDataServiceImpl.java`

在所有使用 `ObjectConvertUtil.convertList` 将 `SpcPointMetadataDO` 转换为 `SpcAnalysisDTO` 的地方，添加手动映射逻辑。

#### 修改位置 1 - queryPointMetaList 方法 (第 514-527 行)

**修改前**:
```java
List<SpcAnalysisDTO> spcAnalysisDTOList = ObjectConvertUtil.convertList(spcIndicatorDOList, SpcAnalysisDTO.class);
List<String> pointList = new ArrayList<>();

if (MapUtil.isNotEmpty(classDictMap)) {
    spcAnalysisDTOList.forEach(dto -> {
        // ...
    });
}
```

**修改后**:
```java
List<SpcAnalysisDTO> spcAnalysisDTOList = ObjectConvertUtil.convertList(spcIndicatorDOList, SpcAnalysisDTO.class);
List<String> pointList = new ArrayList<>();

// 手动映射 enableRealtimeAlarm 到 status
for (int i = 0; i < spcAnalysisDTOList.size(); i++) {
    SpcAnalysisDTO dto = spcAnalysisDTOList.get(i);
    SpcPointMetadataDO indicatorDO = spcIndicatorDOList.get(i);
    dto.setStatus(indicatorDO.getEnableRealtimeAlarm());
}

if (MapUtil.isNotEmpty(classDictMap)) {
    spcAnalysisDTOList.forEach(dto -> {
        // ...
    });
}
```

#### 修改位置 2 - querySpcAnalysis 方法 (第 645-650 行)

**修改前**:
```java
if (!CollectionUtil.isEmpty(spcIndicatorDOList)) {
    spcAnalysisDTOList = ObjectConvertUtil.convertList(spcIndicatorDOList, SpcAnalysisDTO.class);
    spcAnalysisDTOList.stream().forEach(e -> pointList.add(e.getPoint()));
}
```

**修改后**:
```java
if (!CollectionUtil.isEmpty(spcIndicatorDOList)) {
    spcAnalysisDTOList = ObjectConvertUtil.convertList(spcIndicatorDOList, SpcAnalysisDTO.class);
    // 手动映射 enableRealtimeAlarm 到 status
    for (int i = 0; i < spcAnalysisDTOList.size(); i++) {
        spcAnalysisDTOList.get(i).setStatus(spcIndicatorDOList.get(i).getEnableRealtimeAlarm());
    }
    spcAnalysisDTOList.stream().forEach(e -> pointList.add(e.getPoint()));
}
```

#### 修改位置 3 - queryDetailByJobId 方法 (第 1298-1309 行)

**修改前**:
```java
List<SpcAnalysisDTO> spcAnalysisDTOList = ObjectConvertUtil.convertList(list, SpcAnalysisDTO.class);
if (CollectionUtil.isNotEmpty(spcAnalysisDTOList))
    for (int i = 0; i < spcAnalysisDTOList.size(); i++) {
        SpcAnalysisDTO spcAnalysisDTO = spcAnalysisDTOList.get(i);
        SpcPointMetadataDO spcIndicatorDO = list.get(i);
        spcAnalysisDTO.setIndicatorId(spcIndicatorDO.getId());
    }
return spcAnalysisDTOList;
```

**修改后**:
```java
List<SpcAnalysisDTO> spcAnalysisDTOList = ObjectConvertUtil.convertList(list, SpcAnalysisDTO.class);
if (CollectionUtil.isNotEmpty(spcAnalysisDTOList))
    for (int i = 0; i < spcAnalysisDTOList.size(); i++) {
        SpcAnalysisDTO spcAnalysisDTO = spcAnalysisDTOList.get(i);
        SpcPointMetadataDO spcIndicatorDO = list.get(i);
        spcAnalysisDTO.setIndicatorId(spcIndicatorDO.getId());
        // 手动映射 enableRealtimeAlarm 到 status
        spcAnalysisDTO.setStatus(spcIndicatorDO.getEnableRealtimeAlarm());
    }
return spcAnalysisDTOList;
```

## 影响范围

修改影响以下接口返回的 `SpcAnalysisDTO` 对象中的 `status` 字段：

1. **queryPointMetaList** - 查询指标列表
2. **querySpcAnalysis** - 查询 SPC 分析数据
3. **queryDetailByJobId** - 根据作业 ID 查询详情
4. **getDetailByJobId** - 获取作业详情（调用 queryDetailByJobId）

## 字段含义

- `status` (Boolean): 是否启用实时 SPC 阈值告警
  - `true`: 启用实时告警
  - `false`: 禁用实时告警
  - 对应数据库字段: `enable_realtime_alarm`

## 已有的正确映射

在 `queryDetail` 方法中已经有正确的映射逻辑（第 382 行）：

```java
respDTO.setStatus(respDTO.getEnableRealtimeAlarm());
```

这次修改确保了所有返回 `SpcAnalysisDTO` 的方法都正确映射了 `status` 字段。

## 编译验证

✅ **BUILD SUCCESS** - 所有修改通过编译验证

## 测试建议

1. 测试查询指标列表接口，验证返回的 `status` 字段值正确
2. 测试 SPC 分析接口，验证 `status` 字段反映实时告警开关状态
3. 测试根据作业 ID 查询详情，验证 `status` 字段准确性
4. 对比数据库中的 `enable_realtime_alarm` 值与 API 返回的 `status` 值是否一致

---

**修改完成日期**: 2026-01-22

**修改人员**: Claude

**编译状态**: ✅ 成功
