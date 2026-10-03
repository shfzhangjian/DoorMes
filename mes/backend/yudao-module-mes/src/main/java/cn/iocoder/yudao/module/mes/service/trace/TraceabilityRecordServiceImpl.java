// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.trace.TraceabilityRecordServiceImpl.java
package cn.iocoder.yudao.module.mes.service.trace;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.trace.vo.TraceabilityRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.trace.vo.TraceabilityRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.trace.TraceabilityRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.trace.TraceabilityRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;


@Service
@Validated
class TraceabilityRecordServiceImpl implements TraceabilityRecordService {

    @Resource
    private TraceabilityRecordMapper traceabilityRecordMapper;

    @Override
    public Long createTraceRecord(TraceabilityRecordSaveReqVO createReqVO) {
        // 追溯记录一经生成，通常不允许修改与删除，以保证谱系数据的防篡改特性
        TraceabilityRecordDO recordDO = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(createReqVO, TraceabilityRecordDO.class);
        traceabilityRecordMapper.insert(recordDO);
        return recordDO.getId();
    }

    @Override
    public PageResult<TraceabilityRecordDO> getTraceRecordPage(TraceabilityRecordPageReqVO pageReqVO) {
        return traceabilityRecordMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<TraceabilityRecordDO>()
                .eqIfPresent(TraceabilityRecordDO::getWorkOrderId, pageReqVO.getWorkOrderId())
                .eqIfPresent(TraceabilityRecordDO::getActionType, pageReqVO.getActionType())
                .eqIfPresent(TraceabilityRecordDO::getInputLotNo, pageReqVO.getInputLotNo())
                .eqIfPresent(TraceabilityRecordDO::getOutputLotNo, pageReqVO.getOutputLotNo())
                .orderByDesc(TraceabilityRecordDO::getId));
    }
}
