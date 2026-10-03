package cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;

@Schema(description = "管理后台 - 湿法生产记录报工初始化 Request VO")
@Data
public class HcWetProductionRecordInitReqVO {

    @Schema(description = "日期开始")
    private LocalDate recordDateStart;

    @Schema(description = "日期结束")
    private LocalDate recordDateEnd;

    @Schema(description = "批号")
    private String batchNo;

    @Schema(description = "导布更换，Y/N")
    private String guideClothChanged;

    @Schema(description = "更换说明")
    private String changeDesc;
}
