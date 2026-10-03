package cn.iocoder.yudao.module.mes.controller.admin.prod.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProdFeedVOs {
    @Schema(description = "移动端 - 生产投料保存 Request VO")
    @Data
    public static class SaveReqVO {
        private Long id;
        @NotNull(message = "排产ID不能为空")
        private Long scheduleId;
        @NotNull(message = "物料ID不能为空")
        private Long materialId;
        private String lotNo;
        @NotNull(message = "投料数量不能为空")
        private BigDecimal feedQty; // 🚨 强约束 BigDecimal
        @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
        private LocalDateTime feedTime;
        private String operator;
        private String remark;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PageReqVO extends PageParam {
        private Long scheduleId;
        private Long materialId;
        private String lotNo;
    }
}
