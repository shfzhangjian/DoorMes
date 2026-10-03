package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - SRM供方异常与整改台账 Response VO")
@Data
public class SrmScarRespVO {

    private Long id;
    private String scarNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String materialModel;
    private String batchNo;
    private BigDecimal quantity;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate issueDate;

    private String issueDesc;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate replyDate;

    private String replyDesc;
    private String status;
    private Long applicantId;
    private String applicantName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime applyTime;

    private String remark;
    private Integer version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
