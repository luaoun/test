package com.px.ifp.spc.error;


import com.px.ifp.common.exception.ErrorModule;

public class OperationError extends ErrorModule {

    public static final OperationError DATA_DELETE = new OperationError("8001", "该记录删除,无法修改");

    //非法数据
    public static final OperationError DATA_ILLEGAL = new OperationError("8002", "非法数据,无法修改");

    public static final OperationError DATA_DUPLICATE = new OperationError("8003", "%s重复");

    public static final OperationError DATA_INVALID = new OperationError("8004", "%s已失效");

    public static final OperationError OUT_OF_STOCK = new OperationError("8005", "%s库存不足");

    public static final OperationError DATA_MODIFIED = new OperationError("8006", "数据已被更改");

    public static final OperationError UNIT_NOT_MATCH = new OperationError("8007", "入库单位不一致");

    public static final OperationError DATE_FORMAT_ERROR = new OperationError("8008", "日期格式错误");

    public static final OperationError UPLOAD_ERROR = new OperationError("8009", "上传文件失败");

    public static final OperationError INVENTORY_OUT_ERROR = new OperationError("8010", "出库失败");

    public static final OperationError REMOTE_ERROR = new OperationError("8011", "远程服务调用失败");

    public static final OperationError PARAM_ERROR = new OperationError("8010", "参数错误");

    public static final OperationError DELETE_FAILED = new OperationError("8011","删除失败");

    public static final OperationError IMPORT_EXCEL_ERROR = new OperationError("8011", "导入excel数据失败");





    protected OperationError(String code, String reasonPhrase) {
        super(code, reasonPhrase);
    }

}