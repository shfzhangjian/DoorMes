package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - NCR MRB会签 Request VO")
@Data
public class QmsNcMrbReviewReqVO {

    private Long id;
    private Long deptId;
    private String deptName;
    private Long handlerUserId;
    private String handlerUserName;
    private String suggestedDisposition;
    private String dispositionDetail;
    private String rootCauseCategory;
    private String causeAnalysis;
    private String reviewOpinion;
    private String reviewStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime handleTime;
    private Integer sort;
}
