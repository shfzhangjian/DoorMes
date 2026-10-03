// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.job.vo.JobActionHistoryPageReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.job.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "移动端 - SOP作业执行历史分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class JobActionHistoryPageReqVO extends PageParam {

    @Schema(description = "关联派工细单ID")
    private Long subOrderId;

    @Schema(description = "打卡执行人")
    private String operatorUser;

    @Schema(description = "合规标识(true:合规, false:异常)")
    private Boolean compliant; // 架构师红线：杜绝 isCompliant

}
