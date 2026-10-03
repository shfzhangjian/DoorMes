package cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

@Data
public class HcProductModelGenerateReqVO {

    @NotNull(message = "型号规则不能为空")
    private Long modelRuleId;

    private Map<String, String> segmentValues;

}
