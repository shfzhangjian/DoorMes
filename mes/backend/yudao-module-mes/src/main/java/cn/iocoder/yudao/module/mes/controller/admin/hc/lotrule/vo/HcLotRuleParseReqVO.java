package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleSegmentDO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则解析 Request VO")
@Data
public class HcLotRuleParseReqVO {

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "流水长度")
    private Integer seqLength;

    @Schema(description = "年份代码模式")
    private String yearCodeMode;

    @Schema(description = "月份代码模式")
    private String monthCodeMode;

    @Schema(description = "规则分段列表")
    private List<HcLotRuleSegmentDO> lotRuleSegments;

    @Schema(description = "待解析批号")
    @NotBlank(message = "待解析批号不能为空")
    private String lotNo;
}
