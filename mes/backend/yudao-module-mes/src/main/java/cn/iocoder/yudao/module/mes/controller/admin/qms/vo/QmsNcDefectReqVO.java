package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - NCR缺陷明细 Request VO")
@Data
public class QmsNcDefectReqVO {

    private Long id;
    private Long defectCodeId;
    private String defectCode;
    private String defectName;
    private String defectPath;
    private String sourceSectionName;
    private String sourceInspectionItem;
    private String sourceResult;
    private Boolean primaryFlag;
    private Integer sort;
}
