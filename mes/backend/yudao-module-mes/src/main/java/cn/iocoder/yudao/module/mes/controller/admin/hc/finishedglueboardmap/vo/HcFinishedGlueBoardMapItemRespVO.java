package cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 成品胶板对照明细 Response VO")
@Data
public class HcFinishedGlueBoardMapItemRespVO {

    private Long id;
    private Long mapId;
    private String productModelCode;
    private String glueProcess;
    private String glueProcessName;
    private Long glueBoardMaterialId;
    private String glueBoardMaterialCode;
    private String glueBoardMaterialName;
    private String glueBoardModel;
    private String glueBoardSpec;
    private Boolean preferredFlag;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;

}
