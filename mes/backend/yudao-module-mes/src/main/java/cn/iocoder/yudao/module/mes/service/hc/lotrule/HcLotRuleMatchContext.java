package cn.iocoder.yudao.module.mes.service.hc.lotrule;

import lombok.Builder;
import lombok.Data;

/**
 * 批号规则匹配上下文。
 *
 * <p>只表达已有业务事件能够提供的事实；新增业务事件或上下文字段时，
 * 仍需要由对应业务链路显式接入规则引擎。</p>
 */
@Data
@Builder
public class HcLotRuleMatchContext {

    private String bizType;
    private String productCategoryCode;
    private String prodType;
    private String generationTrigger;
    private String generationScope;
    private String modelCode;
}
