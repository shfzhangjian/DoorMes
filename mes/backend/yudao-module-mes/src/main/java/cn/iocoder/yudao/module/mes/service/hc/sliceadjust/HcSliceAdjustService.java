package cn.iocoder.yudao.module.mes.service.hc.sliceadjust;

import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.AuditQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.AuditRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.CandidateQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.CandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.RenameReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.SwapReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.SwapRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.TraceQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.TraceRespVO;
import java.util.List;

public interface HcSliceAdjustService {

    List<CandidateRespVO> getCandidateList(CandidateQueryReqVO reqVO);

    List<TraceRespVO> getTraceList(TraceQueryReqVO reqVO);

    List<AuditRecordRespVO> getAuditRecordList(AuditQueryReqVO reqVO);

    SwapRespVO swapSliceNo(SwapReqVO reqVO);

    SwapRespVO renameSliceNo(RenameReqVO reqVO);
}
