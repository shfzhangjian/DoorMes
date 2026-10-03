package cn.iocoder.yudao.module.mes.service.hc.paramrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo.HcParamRecordContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo.HcParamRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo.HcParamRecordSubmitReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.paramrecord.HcParamRecordDO;

public interface HcParamRecordService {

    HcParamRecordContextRespVO getParamRecordContext(Long reportId, String reportNo);

    void submitParamRecord(HcParamRecordSubmitReqVO reqVO);

    PageResult<HcParamRecordDO> getParamRecordPage(HcParamRecordPageReqVO pageReqVO);
}
