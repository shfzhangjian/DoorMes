package cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 生产指令分页 Request VO")
@Data
public class HcProductionInstructionPageReqVO extends PageParam {

    @Schema(description = "关键词：指令号/计划号/批次号/工序/指令内容")
    private String keyword;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "工序编码")
    private String operationCode;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "状态：ISSUED/CONFIRMED/REVOKED")
    private String status;

    @Schema(description = "下达开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedTimeStart;

    @Schema(description = "下达结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedTimeEnd;

}
