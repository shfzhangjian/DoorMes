package cn.iocoder.yudao.module.mes.controller.admin.hc.scanpreview.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - HC 扫码预览分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcScanPreviewPageReqVO extends PageParam {

    @Schema(description = "关键词，匹配计划号、批次、料号、型号、送检单号")
    private String keyword;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "仅显示有送检单的记录")
    private Boolean onlyWithInspection;

    @Schema(description = "报工日期-开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDateStart;

    @Schema(description = "报工日期-结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDateEnd;

}
