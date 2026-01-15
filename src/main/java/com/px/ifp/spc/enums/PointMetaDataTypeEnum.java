package com.px.ifp.spc.enums;

public enum PointMetaDataTypeEnum {

    ANALOG("ANALOG","模拟量"),
    DIGITAL("DIGITAL","数字量");

    private String code;

    private String fullName;

    PointMetaDataTypeEnum(String code, String fullName){
        this.code = code;
        this.fullName = fullName;
    }

    public static String translateCode(String status) {
        for (PointMetaDataTypeEnum ps : values()) {
            if (ps.name().equalsIgnoreCase(status)) {
                return ps.getFullName();
            }
        }
        return "未知状态";
    }

    private String getFullName() {
        return fullName;
    }

    private String getCode(){
        return code;
    }

    public static String translateFullName(String code){
        switch (code){
            case "ANALOG": return ANALOG.fullName;
            case "DIGITAL": return DIGITAL.fullName;
            default: return "";
        }
    }

    public boolean equals(String compareCode){
        return this.code.equalsIgnoreCase(compareCode);
    }

}
