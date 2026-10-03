// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.qms.vo.QmsTaskPageReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 质量检验任务分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsTaskPageReqVO extends PageParam {

    @Schema(description = "检验单号")
    private String taskNo;

    @Schema(description = "检验类型")
    private String checkType;

    @Schema(description = "批次号")
    private String lotNo;

    @Schema(description = "检验结果")
    private String result;

}
