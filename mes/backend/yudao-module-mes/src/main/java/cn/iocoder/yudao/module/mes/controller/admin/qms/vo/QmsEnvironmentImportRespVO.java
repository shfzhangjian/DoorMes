package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class QmsEnvironmentImportRespVO {

    private Integer totalRows = 0;
    private Integer skippedRows = 0;
    private Integer successCount = 0;
    private Integer failureCount = 0;
    private List<String> messages = new ArrayList<>();
    private List<String> failures = new ArrayList<>();
}
