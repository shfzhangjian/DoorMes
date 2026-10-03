package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDefectCodeDO;
import jakarta.validation.Valid;
import java.util.List;

public interface QmsDefectCodeService {

    Long createDefectCode(@Valid QmsDefectCodeSaveReqVO createReqVO);

    void updateDefectCode(@Valid QmsDefectCodeSaveReqVO updateReqVO);

    void deleteDefectCode(Long id);

    QmsDefectCodeDO getDefectCode(Long id);

    QmsDefectCodeRespVO getDefectCodeResp(Long id);

    List<QmsDefectCodeDO> getDefectCodeList(QmsDefectCodeListReqVO reqVO);
}
