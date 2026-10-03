package cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class HcWetProductionRecordInitRespVO {
    private Integer sourceCount = 0;
    private Integer createCount = 0;
    private Integer skippedCount = 0;
    private List<String> messages = new ArrayList<>();
}
