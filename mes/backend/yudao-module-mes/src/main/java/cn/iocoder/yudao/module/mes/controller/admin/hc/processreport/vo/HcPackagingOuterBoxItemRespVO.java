package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 外包装内包装单元明细 Response VO")
@Data
public class HcPackagingOuterBoxItemRespVO {

    private Long id;
    private Long outerBoxId;
    private String outerBoxNo;
    private Long innerUnitId;
    private String innerUnitNo;
    private Integer innerPackageSpec;
    private Integer pieceQty;
    private String sliceBatchListJson;
    private String scanUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime scanTime;
}
