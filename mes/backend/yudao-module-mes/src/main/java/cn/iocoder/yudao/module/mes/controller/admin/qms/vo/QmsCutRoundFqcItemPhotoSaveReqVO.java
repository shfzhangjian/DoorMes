package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 裁切成品检验项目照片保存 Request VO")
@Data
public class QmsCutRoundFqcItemPhotoSaveReqVO {

    @Schema(description = "FQC主单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "FQC主单ID不能为空")
    private Long fqcId;

    @Schema(description = "送检明细ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "送检明细ID不能为空")
    private Long submissionDetailId;

    @Schema(description = "检验项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "检验项目ID不能为空")
    private Long fqcItemId;

    @Schema(description = "检验样本ID")
    private Long sampleId;

    @Schema(description = "照片URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "照片URL不能为空")
    @Size(max = 2048, message = "照片URL长度不能超过2048个字符")
    private String photoUrl;

    @Schema(description = "照片场景：RESULT/DEFECT/RECHECK")
    private String photoScene;

    @Schema(description = "关联缺陷代码ID")
    private Long defectCodeId;

    @Schema(description = "关联缺陷代码")
    @Size(max = 64, message = "缺陷代码长度不能超过64个字符")
    private String defectCode;

    @Schema(description = "照片关联缺陷码，可多选")
    @Size(max = 20, message = "单张照片最多关联20个缺陷码")
    private List<@Valid QmsCutRoundFqcItemPhotoDefectReqVO> defects;

    @Schema(description = "照片备注")
    @Size(max = 500, message = "照片备注长度不能超过500个字符")
    private String remark;

    @Schema(description = "显示顺序")
    private Integer sort;
}
