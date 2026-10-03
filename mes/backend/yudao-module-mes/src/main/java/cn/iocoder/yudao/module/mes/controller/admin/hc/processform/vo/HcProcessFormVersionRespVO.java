package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import java.time.LocalDate;
import lombok.Data;

@Data
public class HcProcessFormVersionRespVO {

    private Long id;
    private Long templateId;
    private String templateCode;
    private String versionNo;
    private Boolean isCurrent;
    private LocalDate effectiveDate;
    private String sourceFileName;
    private String sourceFilePath;
    private String sourceFileUrl;
    private String sourceFileHash;
    private String sheetJson;
    private String layoutJson;
    private String parseStatus;
    private String parseMessage;
    private String remark;
}
