package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 粘胶胶板边库库存分页 Request VO")
@Data
public class HcAdhesiveGlueBoardStockPageReqVO extends PageParam {

    @Schema(description = "辅料类别")
    private String accessoryCategory;

    @Schema(description = "辅料料号")
    private String glueBoardMaterialCode;

    @Schema(description = "胶板型号")
    private String glueBoardModel;

    @Schema(description = "候选胶板型号，逗号分隔，用于看板按成品胶板对照过滤")
    private String candidateGlueBoardModels;

    @Schema(description = "边库耗材台账来源工序")
    private String sourceProcessCode;

    @Schema(description = "当前看板设备ID，用于粘胶2按机台复用已上机胶板余量")
    private Long equipmentId;

    @Schema(description = "当前看板设备编码，用于粘胶2按机台复用已上机胶板余量")
    private String equipmentCode;

    @Schema(description = "辅料批号")
    private String glueBoardBatchNo;

    @Schema(description = "库存状态")
    private String stockStatus;

    @Schema(description = "质量状态")
    private String qualityStatus;

    @Schema(description = "ERP移库同步状态")
    private String erpTransferStatus;

    @Schema(description = "领料日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate receiveDate;
}
