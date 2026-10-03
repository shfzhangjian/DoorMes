package cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 历史片量产备货库存分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcHistoryPieceMassStockPageReqVO extends PageParam {

    @Schema(description = "关键词（型号、分段批号或备注）")
    private String keyword;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "分段批号")
    private String segmentBatchNo;

}
