// 文件路径: backend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/feed/MesProdFeedService.java
package cn.iocoder.yudao.module.mes.service.feed;

import java.util.Map;

public interface MesProdFeedService {
    /**
     * 创建投料记录 (Action 驱动)
     * @param subOrderId 派工单ID
     * @param actionValue 前端提交的表单数据 (含 barcode, qty 等)
     */
    void createFeedRecord(Long subOrderId, Map<String, Object> actionValue);
}
