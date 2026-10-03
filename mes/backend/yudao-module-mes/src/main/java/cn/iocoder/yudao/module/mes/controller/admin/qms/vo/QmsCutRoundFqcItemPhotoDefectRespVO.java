package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 裁切成品检验项目照片缺陷码 Response VO")
@Data
public class QmsCutRoundFqcItemPhotoDefectRespVO {

    private Long id;
    private Long defectCodeId;
    private String defectCode;
    private String defectName;
    private String defectLevel;
    private Integer sort;
}
