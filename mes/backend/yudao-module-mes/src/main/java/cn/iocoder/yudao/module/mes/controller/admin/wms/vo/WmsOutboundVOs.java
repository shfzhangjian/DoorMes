package cn.iocoder.yudao.module.mes.controller.admin.wms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

public class WmsOutboundVOs {
    @Schema(description = "管理后台 - WMS出库单保存 Request VO")
    @Data
    public static class SaveReqVO {
        private Long id;
        @NotBlank(message = "出库单号不能为空")
        private String outboundNo;
        private String type; // PRODUCTION(领料)/SALE(销售)
        private String workOrderNo;
        @NotNull(message = "物料ID不能为空")
        private Long materialId;
        @NotNull(message = "出库数量不能为空")
        private BigDecimal qty; // 🚨 红线：BigDecimal
        private String status;
        private String remark;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PageReqVO extends PageParam {
        private String outboundNo;
        private String type;
        private String workOrderNo;
        private String status;
    }
}
