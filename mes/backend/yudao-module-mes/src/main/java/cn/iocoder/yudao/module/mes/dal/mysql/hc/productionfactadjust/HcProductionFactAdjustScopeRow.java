package cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust;

import lombok.Data;

@Data
public class HcProductionFactAdjustScopeRow {
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
}
