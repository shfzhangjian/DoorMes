// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.andon.vo.AndonRecordPageReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.andon.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 安灯呼叫分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AndonRecordPageReqVO extends PageParam {

    @Schema(description = "安灯呼叫单号")
    private String andonNo;

    @Schema(description = "类别")
    private String exceptionType;

    @Schema(description = "等级")
    private String severityLevel;

    @Schema(description = "状态")
    private String status;

}
