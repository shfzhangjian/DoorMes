// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.app.prod.vo.ProdCheckPageReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.prod.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "移动端 - 生产质检记录分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProdCheckPageReqVO extends PageParam {

    @Schema(description = "关联排产ID")
    private Long scheduleId;

    @Schema(description = "检查类型")
    private String checkType;

    @Schema(description = "是否合格")
    private Boolean pass; // 🚨 架构师红线坚守

}
