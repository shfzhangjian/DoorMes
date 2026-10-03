package cn.iocoder.yudao.module.mes.dal.mysql.hc.batchtrace;

import lombok.Data;

@Data
public class HcBatchTraceAuxiliaryDTO {

    private Long sourceId;
    private String sourceType;
    private String materialType;
    private String materialTypeName;
    private String materialCode;
    private String materialName;
    private String batchNo;
    private String usageInfo;
    private String sourceTable;

}
