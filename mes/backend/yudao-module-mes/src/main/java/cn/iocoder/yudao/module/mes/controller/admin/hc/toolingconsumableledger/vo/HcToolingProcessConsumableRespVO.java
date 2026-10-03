package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 工序耗材字典 Response VO")
@Data
public class HcToolingProcessConsumableRespVO {

    private Long id;
    private String processCode;
    private String processName;
    private String consumableType;
    private String consumableTypeName;
    private String defaultErpMaterialCode;
    private String defaultBatchNo;
    private Long defaultUomId;
    private String defaultUomCode;
    private String defaultUomName;
    private Integer status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
