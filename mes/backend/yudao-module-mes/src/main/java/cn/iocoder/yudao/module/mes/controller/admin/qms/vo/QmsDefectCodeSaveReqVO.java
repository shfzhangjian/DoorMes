package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 缺陷代码新增/修改 Request VO")
@Data
public class QmsDefectCodeSaveReqVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "父级ID")
    @NotNull(message = "父级分类不能为空")
    private Long parentId;

    @Schema(description = "缺陷代码")
    @NotBlank(message = "缺陷代码不能为空")
    private String code;

    @Schema(description = "缺陷名称")
    @NotBlank(message = "缺陷名称不能为空")
    private String name;

    @Schema(description = "节点类型")
    @NotBlank(message = "节点类型不能为空")
    private String type;

    @Schema(description = "严重等级")
    private String level;

    @Schema(description = "参考缺陷图片")
    private List<String> referencePicUrls;

    @Schema(description = "发生原因明细")
    private List<@Valid Cause> causes;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "管理后台 - 缺陷发生原因")
    @Data
    public static class Cause {

        @Schema(description = "主键")
        private Long id;

        @Schema(description = "原因代码")
        private String reasonCode;

        @Schema(description = "发生原因名称")
        @NotBlank(message = "发生原因名称不能为空")
        private String reasonName;

        @Schema(description = "原因分类")
        private String reasonType;

        @Schema(description = "原因说明")
        private String reasonDesc;

        @Schema(description = "排序号")
        private Integer sort;

        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "备注")
        private String remark;
    }
}
