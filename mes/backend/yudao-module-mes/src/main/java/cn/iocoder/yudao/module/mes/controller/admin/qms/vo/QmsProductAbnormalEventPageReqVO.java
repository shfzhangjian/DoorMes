package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 产品异常事件分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsProductAbnormalEventPageReqVO extends PageParam {

    @Schema(description = "检验来源类型：FAI、GLUE_BOARD_FAI、CUT_ROUND_FQC、FG_SHIPPING_FQC、OQC")
    private String sourceType;

    @Schema(description = "NCR生成状态：PENDING 待生成，GENERATED 已生成，ALL 全部")
    private String ncrStatus;

    @Schema(description = "驳回复检状态：ALL 全部，NONE 未驳回，RECHECKING 复检中，RECHECK_OK 复检OK，RECHECK_NG 复检NG")
    private String recheckStatus;

    @Schema(description = "检验单号")
    private String inspectionNo;

    @Schema(description = "工序")
    private String operationName;

    @Schema(description = "工序分类")
    private String processCategory;

    @Schema(description = "产品型号")
    private String productModel;

    @Schema(description = "产品批次")
    private String productBatchNo;

    @Schema(description = "检验时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] inspectionTime;
}
