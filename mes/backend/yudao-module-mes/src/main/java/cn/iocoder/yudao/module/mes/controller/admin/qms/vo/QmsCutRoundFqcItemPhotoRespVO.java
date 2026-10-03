package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 裁切成品检验项目照片 Response VO")
@Data
public class QmsCutRoundFqcItemPhotoRespVO {

    private Long id;
    private Long fqcId;
    private String fqcNo;
    private Long submissionDetailId;
    private Long fqcItemId;
    private Long sampleId;
    private String productionBatchNo;
    private String inspectionItem;
    private String photoUrl;
    private String photoScene;
    private Long defectCodeId;
    private String defectCode;
    private List<QmsCutRoundFqcItemPhotoDefectRespVO> defects;
    private String remark;
    private Integer sort;
    private Long capturedById;
    private String capturedByName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime capturedTime;
}
