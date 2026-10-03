// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.trace.TraceabilityRecordServiceImpl.java
package cn.iocoder.yudao.module.mes.service.trace;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.trace.vo.TraceabilityRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.trace.vo.TraceabilityRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.trace.TraceabilityRecordDO;

import jakarta.validation.Valid;

public interface TraceabilityRecordService {
    Long createTraceRecord(@Valid TraceabilityRecordSaveReqVO createReqVO);
    PageResult<TraceabilityRecordDO> getTraceRecordPage(TraceabilityRecordPageReqVO pageReqVO);
}
