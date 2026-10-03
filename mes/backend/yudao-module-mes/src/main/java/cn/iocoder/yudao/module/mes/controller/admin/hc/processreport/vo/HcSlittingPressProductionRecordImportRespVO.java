package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class HcSlittingPressProductionRecordImportRespVO {
    private Integer totalRows = 0;
    private Integer skippedRows = 0;
    private Integer createCount = 0;
    private Integer updateCount = 0;
    private Integer failureCount = 0;
    private List<String> messages = new ArrayList<>();
    private List<String> failures = new ArrayList<>();
}
