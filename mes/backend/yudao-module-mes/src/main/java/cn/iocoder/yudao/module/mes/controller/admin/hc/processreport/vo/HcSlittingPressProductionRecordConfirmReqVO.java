package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class HcSlittingPressProductionRecordConfirmReqVO {
    @NotEmpty(message = "确认记录不能为空") private List<Long> ids;
    private String confirmerName;
}
