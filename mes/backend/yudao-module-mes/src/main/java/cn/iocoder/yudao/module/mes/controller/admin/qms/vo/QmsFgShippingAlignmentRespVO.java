package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 发货客户批号对齐详情 Response VO")
@Data
public class QmsFgShippingAlignmentRespVO {

    private Long shippingNoticeId;
    private String shippingNoticeNo;
    private String customerName;
    private String erpOrderNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String noticeStatus;
    private Integer plannedCount;
    private Integer alignedCount;
    private Integer candidateCount;
    private Boolean alignmentCompleted;
    private List<QmsFgShippingFqcRespVO.AlignmentRow> rows;
    private List<QmsFgShippingFqcRespVO.AlignmentCandidate> candidates;
}
