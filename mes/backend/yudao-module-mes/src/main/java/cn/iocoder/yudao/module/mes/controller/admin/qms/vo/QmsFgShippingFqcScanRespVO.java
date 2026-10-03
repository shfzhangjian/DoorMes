package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 发货成品检验扫码解析 Response VO")
@Data
public class QmsFgShippingFqcScanRespVO {

    private String scanCode;
    private String scanTargetType;
    private String matchResult;
    private String message;
    private String openTarget;
    private QmsFgShippingFqcRespVO record;
    private QmsFgShippingFqcRespVO.ShippingDetail detail;
}
