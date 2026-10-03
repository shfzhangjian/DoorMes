package cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust;

import lombok.Data;

@Data
public class HcProductionFactAdjustProductRow {
    private Long productModelId;
    private String modelCode;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String specification;
    private Integer variantCount;
}
