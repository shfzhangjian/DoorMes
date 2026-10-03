// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.trace.vo.TraceabilityRecordPageReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.trace.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 全链路批次追溯分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class TraceabilityRecordPageReqVO extends PageParam {

    @Schema(description = "关联主工单ID")
    private Long workOrderId;

    @Schema(description = "动作类型")
    private String actionType;

    @Schema(description = "投入批次号 (正向追溯入口)")
    private String inputLotNo;

    @Schema(description = "产出批次号 (反向追溯入口)")
    private String outputLotNo;

}
