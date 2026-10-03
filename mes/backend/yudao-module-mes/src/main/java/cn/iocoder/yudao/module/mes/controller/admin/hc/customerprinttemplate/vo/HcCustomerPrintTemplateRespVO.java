package cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 客户打印模板 Response VO")
@Data
public class HcCustomerPrintTemplateRespVO {

    private Long id;
    private String templateCode;
    private String templateName;
    private String customerCode;
    private String customerName;
    private String templateType;
    private String templateFormat;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String templateContent;
    private String variableJson;
    private Integer status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    private List<HcCustomerPrintTemplateVarRespVO> vars;

}
