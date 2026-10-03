// 文件路径: backend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/feed/MesProdFeedServiceImpl.java
package cn.iocoder.yudao.module.mes.service.feed;

import cn.iocoder.yudao.module.mes.dal.dataobject.prod.ProdFeedDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import cn.iocoder.yudao.module.mes.dal.mysql.prod.ProdFeedMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.workordersub.MesWorkOrderSubMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Validated
public class MesProdFeedServiceImpl implements MesProdFeedService {

    @Resource
    private ProdFeedMapper prodFeedMapper;
    @Resource
    private MesWorkOrderSubMapper subOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFeedRecord(Long subOrderId, Map<String, Object> actionValue) {
        // 1. 查询上下文
        MesWorkOrderSubDO subOrder = subOrderMapper.selectById(subOrderId);
        if (subOrder == null) throw new RuntimeException("工单不存在");

        // 2. 解析 JSON 数据 (假设前端提交了 barcode 和 qty)
        // 实际场景可能需要调用 WMS 接口根据 Barcode 查询物料信息
        String barcode = (String) actionValue.getOrDefault("barcode", "UNKNOWN");
        Object qtyObj = actionValue.getOrDefault("qty", 1);
        BigDecimal qty = new BigDecimal(qtyObj.toString());

        // 3. 构建投料记录
        ProdFeedDO feedDO = ProdFeedDO.builder()
                .scheduleId(subOrderId)
                .tenantId(subOrder.getTenantId())
                .materialId(0L) // TODO: 根据 barcode 反查 materialId
                .materialCode("M_Parsed_From_" + barcode) // 暂用占位符
                .materialName("物料-" + barcode)
                .lotNo(barcode) // 假设扫的是批次码
                .feedQty(qty)
                .feedTime(LocalDateTime.now())
                .operator(subOrder.getOperatorUser())
                .build();

        // 4. 落库
        prodFeedMapper.insert(feedDO);

        // 5. TODO: 调用 WMS 扣减线边库库存 (WmsStockService)
    }
}
