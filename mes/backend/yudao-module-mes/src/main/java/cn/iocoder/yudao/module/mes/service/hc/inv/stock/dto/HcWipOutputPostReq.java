package cn.iocoder.yudao.module.mes.service.hc.inv.stock.dto;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 工序产出物入中间边库请求。
 */
@Data
@Builder
public class HcWipOutputPostReq {

    private HcPlanOrderDO plan;
    private HcPlanOrderOperationDO operation;
    private String sourceType;
    private String sourceTable;
    private Long sourceId;
    private Long sourceReportId;
    private String batchNo;
    private String parentBatchNo;
    private BigDecimal outputQty;
    private String uom;
    private String segmentCode;
    private String segmentName;
    private BigDecimal thickness;
    private String materialCode;
    private String materialName;
    private Long materialId;
    private String modelNo;
    private String specSize;
    private LocalDate productionDate;
    private String qualityStatus;
    private String bizStatus;
    private String businessRemark;
    private String txnRemark;
    private LocalDateTime postTime;
    private Long operatorId;
    private String operatorName;

}
