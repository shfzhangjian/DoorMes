package cn.iocoder.yudao.module.mes.service.hc.batchtrace;

import cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace.vo.HcBatchTraceQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace.vo.HcBatchTraceRespVO;

public interface HcBatchTraceService {

    HcBatchTraceRespVO getTrace(HcBatchTraceQueryReqVO reqVO);

}
