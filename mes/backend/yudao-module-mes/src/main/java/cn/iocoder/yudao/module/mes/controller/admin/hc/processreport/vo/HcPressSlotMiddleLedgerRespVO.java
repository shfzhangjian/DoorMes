package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;

@Schema(description = "管理后台 - 压槽中间品台账 Response VO")
@Data
public class HcPressSlotMiddleLedgerRespVO {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    private String sliceBatchNo;
    private String reportType;
    private String reportTypeName;
    private String widthMm;
    private String thickness1;
    private String thickness2;
    private String thickness3;
    private String thickness4;
    private String thickness5;
    private String thickness6;
    private String thickness7;
    private String thickness8;
    private String thickness9;
    private String thickness10;
    private String remark;
}
