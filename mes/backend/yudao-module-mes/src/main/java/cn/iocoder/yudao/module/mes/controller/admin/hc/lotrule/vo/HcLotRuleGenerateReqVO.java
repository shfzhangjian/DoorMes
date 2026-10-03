package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleSegmentDO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则生成 Request VO")
@Data
public class HcLotRuleGenerateReqVO {

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "业务日期")
    private LocalDate bizDate;

    @Schema(description = "规则模式")
    private String ruleMode;

    @Schema(description = "年份代码模式")
    private String yearCodeMode;

    @Schema(description = "月份代码模式")
    private String monthCodeMode;

    @Schema(description = "流水长度")
    private Integer seqLength;

    @Schema(description = "流水起始值")
    private Integer seqStart;

    @Schema(description = "流水步长")
    private Integer seqStep;

    @Schema(description = "重置周期")
    private String resetCycle;

    @Schema(description = "抽样段规则")
    private String sampleSegmentRule;

    @Schema(description = "是否占用流水号")
    private Boolean consumeSequence;

    @Schema(description = "规则分段列表，未传则按规则主数据读取")
    private List<HcLotRuleSegmentDO> lotRuleSegments;

    @Schema(description = "动态输入值，key=segmentCode")
    private Map<String, String> inputValues;
}
