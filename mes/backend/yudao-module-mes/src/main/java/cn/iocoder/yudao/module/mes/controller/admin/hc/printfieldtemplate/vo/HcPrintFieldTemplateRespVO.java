package cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 打印字段模板 Response VO")
@Data
public class HcPrintFieldTemplateRespVO {

    private Long id;
    private String templateCode;
    private String templateName;
    private String processCode;
    private String processName;
    private String documentType;
    private String documentName;
    private String usageScene;
    private Integer status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    private List<HcPrintFieldTemplateItemRespVO> items;

}
