package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 缺陷代码 Response VO")
@Data
public class QmsDefectCodeRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "缺陷代码")
    private String code;

    @Schema(description = "缺陷名称")
    private String name;

    @Schema(description = "节点类型")
    private String type;

    @Schema(description = "严重等级")
    private String level;

    @Schema(description = "参考缺陷图片")
    private List<String> referencePicUrls;

    @Schema(description = "发生原因明细")
    private List<Cause> causes;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "管理后台 - 缺陷发生原因")
    @Data
    public static class Cause {

        @Schema(description = "主键")
        private Long id;

        @Schema(description = "缺陷项ID")
        private Long defectCodeId;

        @Schema(description = "缺陷代码")
        private String defectCode;

        @Schema(description = "缺陷名称")
        private String defectName;

        @Schema(description = "原因代码")
        private String reasonCode;

        @Schema(description = "发生原因名称")
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
