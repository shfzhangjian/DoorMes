package cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust;

import lombok.Data;

@Data
public class HcProductionFactAdjustImpactRow {
    private Integer adhesive2ReportCount;
    private Integer cutRoundReportCount;
    private Integer outputStockCount;
    private Integer cutInspectionDetailCount;
    private Integer fqcOrderCount;
    private Integer fqcSubmissionDetailCount;
    private Integer faiOrderCount;
    private Integer processFormCount;
    private Integer packagingCount;
    private Integer finishedStockCount;
    private Integer unsafeFqcOrderCount;
    private Integer unsafeFaiOrderCount;
}
