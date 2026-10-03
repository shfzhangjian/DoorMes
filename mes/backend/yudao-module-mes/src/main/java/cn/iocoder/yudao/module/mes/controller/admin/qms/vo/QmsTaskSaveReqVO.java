// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsTaskSaveReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 质量检验任务保存 Request VO")
@Data
public class QmsTaskSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "检验单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "检验单号不能为空")
    private String taskNo;

    @Schema(description = "检验类型(IQC/IPQC/FQC)")
    private String checkType;

    @Schema(description = "来源类型(INBOUND/WORK_ORDER)")
    private String sourceType;

    @Schema(description = "来源单据ID")
    private Long sourceId;

    @Schema(description = "物料ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "物料ID不能为空")
    private Long materialId;

    @Schema(description = "批次号")
    private String lotNo;

    @Schema(description = "报检数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "报检数量不能为空")
    private BigDecimal checkQty; // 🚨 架构师红线：强约束 BigDecimal

    @Schema(description = "抽样/损耗数量")
    private BigDecimal sampleQty; // 🚨 架构师红线：强约束 BigDecimal

    @Schema(description = "检验结果 (PENDING, QUALIFIED, REJECTED 等)")
    private String result;

    @Schema(description = "检验员")
    private String inspector;

    @Schema(description = "检验时间")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime checkTime;

    @Schema(description = "备注")
    private String remark;

}
