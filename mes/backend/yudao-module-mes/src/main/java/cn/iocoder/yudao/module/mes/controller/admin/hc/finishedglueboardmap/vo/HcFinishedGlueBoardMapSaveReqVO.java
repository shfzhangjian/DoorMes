package cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 成品胶板对照表新增/修改 Request VO")
@Data
public class HcFinishedGlueBoardMapSaveReqVO {

    private Long id;

    private Long productModelId;

    @NotBlank(message = "产品型号不能为空")
    private String productModelCode;

    private String productModelName;

    @NotBlank(message = "成品规格不能为空")
    private String productSpec;

    private String sizeSpec;
    private String sizeName;
    private String adhesive1Summary;
    private String adhesive2Summary;
    private String status;
    private String remark;

    @Valid
    private List<HcFinishedGlueBoardMapItemReqVO> items;

}
