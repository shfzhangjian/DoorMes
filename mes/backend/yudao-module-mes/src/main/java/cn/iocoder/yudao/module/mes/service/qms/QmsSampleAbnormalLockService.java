package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsSampleAbnormalLockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsSampleAbnormalLockRespVO;

public interface QmsSampleAbnormalLockService {

    void syncFromFai(Long faiId);

    PageResult<QmsSampleAbnormalLockRespVO> getPage(QmsSampleAbnormalLockPageReqVO pageReqVO);

    default QmsSampleAbnormalLockRespVO getActiveLock(String sourceProcessCode, String objectType, String objectNo) {
        return getActiveLock(sourceProcessCode, objectType, objectNo, objectNo);
    }

    QmsSampleAbnormalLockRespVO getActiveLock(String sourceProcessCode, String objectType, String objectNo,
                                              String qualificationObjectNo);

    QmsSampleAbnormalLockRespVO createRecheck(Long id);
}
