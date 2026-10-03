package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 发货成品检验待检发货通知 Response VO")
@Data
public class QmsFgShippingFqcPendingRespVO {

    private Long shippingNoticeId;
    private String shippingNoticeNo;
    private String customerName;
    private String erpOrderNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String productSize;
    private String noticeStatus;
    private Integer pickedQty;
    private Integer inspectedQty;
    private Long existingFqcId;
    private String existingFqcNo;
    private String existingFqcStatus;
    private String existingFqcJudgment;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime shippingTime;
}
