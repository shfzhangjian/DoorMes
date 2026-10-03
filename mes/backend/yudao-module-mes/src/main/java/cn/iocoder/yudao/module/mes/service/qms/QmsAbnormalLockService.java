package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsAbnormalLockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsAbnormalLockRespVO;

public interface QmsAbnormalLockService {

    void syncFromFai(Long faiId);

    void syncFromIpqc(Long ipqcId);

    void syncFromFqc(Long fqcId);

    PageResult<QmsAbnormalLockRespVO> getPage(QmsAbnormalLockPageReqVO reqVO);
}
