package cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - HC 排程工作台 Request VO")
@Data
public class HcScheduleWorkbenchReqVO {

    @Schema(description = "日期范围-开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "日期范围-结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Schema(description = "关键词，匹配计划号、型号、批号、执行要求")
    private String keyword;

    @Schema(description = "计划状态列表")
    private List<String> planStatuses;

    @Schema(description = "产品型号")
    private String modelCode;

    @Schema(description = "尺寸规格")
    private String sizeSpec;

    @Schema(description = "工序名称")
    private String operationName;

}
