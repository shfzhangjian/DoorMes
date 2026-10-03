package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - SRM供应商协议资质 Response VO")
@Data
public class SrmSupplierFileRespVO {

    private Long id;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String fileType;
    private String fileName;
    private String providedProduct;
    private String productModel;
    private String inspectionAgency;
    private String reportCode;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate effectDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate expiryDate;

    private String fileStatus;
    private Integer warningDays;
    private Integer daysLeft;
    private String standardCompliant;
    private Integer validityMonths;
    private String expiryRule;
    private String payloadJson;
    private String attachmentName;
    private String attachmentUrl;
    private String sourceBizType;
    private Long sourceBizId;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
