package cn.iocoder.yudao.module.mes.controller.admin.hc.inv.txn.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 库存流水分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcInvTxnLogPageReqVO extends PageParam {

    @Schema(description = "仓库编码")
    private String warehouseCode;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "型号")
    private String modelNo;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "交易类型（多选）")
    private List<String> txnTypes;

    @Schema(description = "单据号")
    private String txnNo;

    @Schema(description = "交易时间-起")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime txnTimeStart;

    @Schema(description = "交易时间-止")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime txnTimeEnd;

}
