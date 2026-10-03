package cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 量产备货库存分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcMassStockPageReqVO extends PageParam {

    @Schema(description = "关键词：型号/母卷批号/母卷段号/SEM/备注")
    private String keyword;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "母卷批号")
    private String motherBatchNo;

    @Schema(description = "母卷段号")
    private String motherSegmentBatchNo;

    @Schema(description = "仅显示已粘胶2")
    private Boolean onlyAdhesive2;

}
