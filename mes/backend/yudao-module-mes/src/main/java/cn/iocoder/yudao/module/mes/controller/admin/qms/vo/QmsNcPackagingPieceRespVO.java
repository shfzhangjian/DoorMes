package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import lombok.Data;

/** 包装工作台只读处置范围；未确认范围不代表最终处置结论。 */
@Data
public class QmsNcPackagingPieceRespVO {
    private String scopeLevel;
    private String pieceNo;
    private String segmentBatchNo;
    private String motherBatchNo;
    private String dispositionType;
    private String scopeRole;
    private String executionResult;
    private String remark;
    private Boolean scopeConfirmed;
    private Boolean packaged;
}
