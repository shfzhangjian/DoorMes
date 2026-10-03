package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 检验记录候选标准 Response VO")
@Data
public class QmsInspectionStandardCandidateRespVO {

    @Schema(description = "标准ID")
    private Long id;

    @Schema(description = "标准编号")
    private String standardNo;

    @Schema(description = "标准名称")
    private String standardName;

    @Schema(description = "标准版本")
    private String version;

    @Schema(description = "适用环节")
    private String applyType;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "产品型号ID")
    private Long productModelId;

    @Schema(description = "产品型号编码")
    private String productModelCode;

    @Schema(description = "产品型号名称")
    private String productModelName;

    @Schema(description = "胶板型号")
    private String glueBoardModel;

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "检验项目数量")
    private Integer itemCount;

    @Schema(description = "匹配类型：EXACT_MODEL/FAMILY_MODEL/MATERIAL/PROCESS/UNIVERSAL")
    private String matchType;

    @Schema(description = "匹配分数；数值越高优先级越高")
    private Integer matchScore;

    @Schema(description = "匹配原因")
    private String matchReason;

    @Schema(description = "是否为当前推荐标准")
    private Boolean recommended;
}
