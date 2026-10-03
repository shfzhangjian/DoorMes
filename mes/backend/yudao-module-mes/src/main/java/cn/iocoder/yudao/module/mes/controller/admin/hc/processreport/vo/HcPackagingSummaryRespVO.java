package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 包装看板汇总 Response VO")
@Data
public class HcPackagingSummaryRespVO {

    private Integer sourcePieceCount;
    private Integer unpackedPieceCount;
    private Integer innerUnitCount;
    private Integer innerPieceCount;
    private Integer reviewedInnerUnitCount;
    private Integer outerBoxCount;
    private Integer outerPieceCount;
    private Integer reviewedOuterBoxCount;
}
