package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 最近已确认磨皮生产记录耗材同步 Response VO")
@Data
public class HcGrindingProductionRecordConsumableSyncRespVO {

    private Long sourceRecordId;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;

    /** 按全局日期时间契约以 yyyy-MM-dd HH:mm:ss 字符串返回。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sourceRecordTime;

    private BigDecimal sandpaperLife;
    private Integer sandpaperLifeDays;
    private Integer guideClothLife;
    private Boolean synced;
    private String message;
}
