package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 包装点检/清洁明细 Response VO")
@Data
public class HcPackagingPassWorkItemRespVO {

    private Integer itemSeq;
    private String category;
    private String node;
    private String item;
    private String standard;
    private String valueMode;
    private String dualLabel1;
    private String dualLabel2;
    private String actualValue;
    private String actualValue2;
    private String status;
    private String remark;
}
