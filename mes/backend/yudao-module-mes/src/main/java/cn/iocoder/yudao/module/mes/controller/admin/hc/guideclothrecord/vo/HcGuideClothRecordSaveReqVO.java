package cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 导布更换记录新增/修改 Request VO")
@Data
public class HcGuideClothRecordSaveReqVO {

    private Long id;

    @NotBlank(message = "产线名不能为空")
    private String lineName;

    @NotBlank(message = "产线编号不能为空")
    private String lineCode;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    @NotNull(message = "上次更换时间不能为空或无效")
    private LocalDateTime replaceTime;

    private String replacePlanNo;

    private String petBatchNo;

    private String guideClothBatchNo;

    private String petModel;

    private String replaceReason;

    @NotNull(message = "累计使用次数不能为空")
    private Integer useCount;

    @NotNull(message = "当前标记不能为空")
    private Integer currentFlag;

    private String remark;
}
