package cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 成品胶板对照明细 Request VO")
@Data
public class HcFinishedGlueBoardMapItemReqVO {

    private Long id;

    @NotBlank(message = "粘胶工序不能为空")
    private String glueProcess;

    private String glueProcessName;
    private Long glueBoardMaterialId;
    private String glueBoardMaterialCode;
    private String glueBoardMaterialName;
    private String glueBoardModel;
    private String glueBoardSpec;
    private Boolean preferredFlag;
    private Integer sort;
    private String remark;

}
