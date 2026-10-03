package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 裁切报工初始化 Response VO")
@Data
public class HcCutRoundProductionRecordInitRespVO {

    private Integer sourceCount = 0;
    private Integer createCount = 0;
    private Integer skippedCount = 0;
    private List<String> messages = new ArrayList<>();
}
