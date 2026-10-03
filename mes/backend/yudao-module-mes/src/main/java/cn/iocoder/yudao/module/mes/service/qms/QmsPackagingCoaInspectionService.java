package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsPackagingCoaInspectionVO.FaiRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsPackagingCoaInspectionVO.SubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsPackagingCoaInspectionVO.SubmitRespVO;
import jakarta.validation.Valid;

/** 包装段 COA 送检服务。 */
public interface QmsPackagingCoaInspectionService {

    SubmitRespVO submit(@Valid SubmitReqVO reqVO);

    PageResult<FaiRecordRespVO> getFaiPage(@Valid QmsFaiPageReqVO pageReqVO);
}
