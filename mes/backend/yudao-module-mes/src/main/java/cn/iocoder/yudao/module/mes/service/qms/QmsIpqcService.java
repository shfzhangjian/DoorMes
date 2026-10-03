package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcStandardRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcOrderDO;
import java.util.List;

public interface QmsIpqcService {

    Long createIpqc(QmsIpqcSaveReqVO createReqVO);

    void updateIpqc(QmsIpqcSaveReqVO updateReqVO);

    void submitIpqc(QmsIpqcSaveReqVO submitReqVO);

    void suspendIpqc(Long id);

    void deleteIpqc(Long id);

    void deleteIpqcList(List<Long> ids);

    QmsIpqcRespVO getIpqcResp(Long id);

    PageResult<QmsIpqcOrderDO> getIpqcPage(QmsIpqcPageReqVO pageReqVO);

    List<QmsIpqcRespVO> getPendingIpqcList();

    QmsIpqcStandardRespVO getIpqcStandard(String materialCode, String operationCode, String operationName);
}
