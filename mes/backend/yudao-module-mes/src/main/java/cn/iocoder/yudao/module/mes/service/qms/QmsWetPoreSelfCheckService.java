package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsWetPoreSelfCheckPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsWetPoreSelfCheckPhotoReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsWetPoreSelfCheckRespVO;
import jakarta.validation.Valid;

public interface QmsWetPoreSelfCheckService {

    PageResult<QmsWetPoreSelfCheckRespVO> getPage(QmsWetPoreSelfCheckPageReqVO reqVO);

    QmsWetPoreSelfCheckRespVO updatePhoto(@Valid QmsWetPoreSelfCheckPhotoReqVO reqVO);

}
