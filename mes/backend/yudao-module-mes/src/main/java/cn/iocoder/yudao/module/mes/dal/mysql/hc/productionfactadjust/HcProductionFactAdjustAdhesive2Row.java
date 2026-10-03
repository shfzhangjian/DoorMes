package cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HcProductionFactAdjustAdhesive2Row {
    private Long id;
    private String productionBatchNo;
    private String modelCode;
    private String materialCode;
    private LocalDateTime confirmerTime;
    private Long tenantId;
}
