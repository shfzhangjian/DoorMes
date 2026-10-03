package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ResourceDeviceImportRespVO {

    private Integer successCount = 0;
    private Integer failureCount = 0;
    private List<String> messages = new ArrayList<>();

    public void addSuccess() {
        successCount++;
    }

    public void addFailure(String message) {
        failureCount++;
        messages.add(message);
    }

}
