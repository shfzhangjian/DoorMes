package cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 湿法生产记录分页 Request VO")
@Data
public class HcWetProductionRecordPageReqVO extends PageParam {

    @Schema(description = "日期开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDateStart;

    @Schema(description = "日期结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDateEnd;

    @Schema(description = "批号")
    private String batchNo;

    @Schema(description = "垫型：BLACK_PAD/WHITE_PAD；UNCLASSIFIED 仅用于查询未归类历史记录")
    private String padType;

    @Schema(description = "导布更换，Y/N")
    private String guideClothChanged;

    @Schema(description = "更换说明")
    private String changeDesc;

    @Schema(description = "确认状态，WAIT_CONFIRM/CONFIRMED")
    private String status;
}
