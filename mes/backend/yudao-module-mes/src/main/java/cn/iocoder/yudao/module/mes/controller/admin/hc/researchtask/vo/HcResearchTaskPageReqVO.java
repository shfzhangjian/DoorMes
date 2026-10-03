package cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - HC 研发管理分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcResearchTaskPageReqVO extends PageParam {

    @Schema(description = "研发任务号")
    private String taskNo;

    @Schema(description = "研发型号编码")
    private String rdModelCode;

    @Schema(description = "关键词，匹配任务号、型号、配方、路线、研发目的")
    private String keyword;

    @Schema(description = "状态")
    private String taskStatus;

    @Schema(description = "状态列表")
    private List<String> taskStatuses;

    @Schema(description = "型号类型编码")
    private String productClassCode;

    @Schema(description = "基准配方编码")
    private String baseFormulaCode;

    @Schema(description = "湿法工艺编码")
    private String wetProcessCode;

    @Schema(description = "磨皮工艺编码")
    private String grindingProcessCode;

    @Schema(description = "后工艺编码")
    private String postProcessCode;

    @Schema(description = "路线编码")
    private String routeCode;

    @Schema(description = "研发日期-开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate researchDateStart;

    @Schema(description = "研发日期-结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate researchDateEnd;

}
