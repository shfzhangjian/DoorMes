package cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 导布更换记录 Response VO")
@Data
public class HcGuideClothRecordRespVO {

    private Long id;
    private String lineName;
    private String lineCode;
    private Long equipmentId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime replaceTime;

    private String replacePlanNo;
    private String petBatchNo;
    private String guideClothBatchNo;
    private String petModel;
    private String replaceReason;
    private Integer useCount;
    private Integer currentFlag;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
