// 文件路径: backend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qms/MesQmsServiceImpl.java
package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.workordersub.MesWorkOrderSubMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Validated
public class MesQmsServiceImpl implements MesQmsService {

    @Resource
    private QmsNcRecordMapper ncRecordMapper;
    @Resource
    private MesWorkOrderSubMapper subOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNcRecord(Long subOrderId, Long actionId, String reason) {
        MesWorkOrderSubDO subOrder = subOrderMapper.selectById(subOrderId);

        QmsNcRecordDO ncDO = QmsNcRecordDO.builder()
                .tenantId(subOrder.getTenantId())
                .subOrderId(subOrderId)
                .ncNo("NCR-" + UUID.randomUUID().toString().substring(0, 8)) // 简易单号生成
                .lotNo(subOrder.getWorkOrderNo()) // 暂用工单号作为批次
                .defectCode("DEF_AUTO")
                .defectQty(BigDecimal.ONE) // 默认1件
                .mrbDecision("PENDING") // 待判定
                .remark("触发动作ID: " + actionId + ", 原因: " + reason)
                .build();

        ncRecordMapper.insert(ncDO);
    }
}
