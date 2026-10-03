package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 压槽工位 ERP 相似物料 Response VO")
@Data
public class HcPressSlotConsumableMaterialRespVO {

    private Long id;

    private String materialCode;

    private String materialName;

    private String materialShortName;

    private String materialCategoryName;

    private String specModel;

    private String baseUom;

    private String stockUom;

}
