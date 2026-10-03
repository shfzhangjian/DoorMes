package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - HC 生产计划静态选项 Response VO")
@Data
public class HcPlanOrderStaticOptionsRespVO {

    @Schema(description = "生产类型选项")
    private List<Option> prodTypes;

    @Schema(description = "物料类别选项")
    private List<Option> materialCategories;

    @Schema(description = "尺寸规格选项")
    private List<Option> sizeSpecs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {

        @Schema(description = "选项 ID")
        private Long id;

        @Schema(description = "编码")
        private String code;

        @Schema(description = "文字描述")
        private String name;

    }

}
