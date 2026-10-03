package cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - 计划排程统计分析字段目录 Response VO")
@Data
@Builder
public class HcProcessAnalysisFieldRespVO {

    @Schema(description = "字段编码")
    private String code;

    @Schema(description = "字段名称")
    private String label;

    @Schema(description = "字段分类")
    private String category;

    @Schema(description = "数据类型：TEXT、NUMBER、DATE、DATETIME、BOOLEAN")
    private String dataType;

    @Schema(description = "字段角色：DIMENSION、METRIC、TIME、DETAIL")
    private String role;

    @Schema(description = "生产进度来源路径")
    private String sourcePath;

    @Schema(description = "默认聚合方式")
    private String defaultAggregation;

    @Schema(description = "按业务粒度去重时使用的字段")
    private String distinctKeyField;

    @Schema(description = "可用过滤操作符")
    private List<String> filterOperators;

    @Schema(description = "是否可作为钻取维度")
    private Boolean drillable;

    @Schema(description = "字段说明")
    private String description;

}
