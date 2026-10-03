package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 粘胶生产记录表分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcAdhesiveProductionRecordPageReqVO extends PageParam {

    @Schema(description = "报工日期开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDateStart;

    @Schema(description = "报工日期结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDateEnd;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "垫型：BLACK_PAD/WHITE_PAD；UNCLASSIFIED 仅用于查询未归类历史记录")
    private String padType;

    @Schema(description = "料号")
    private String materialCode;

    @Schema(description = "批号")
    private String batchNo;

    @Schema(description = "记录人")
    private String recorderName;

}
