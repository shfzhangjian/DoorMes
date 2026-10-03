package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceImportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatSyncReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatSyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatUpdateReqVO;

public interface QmsCmpWarpageSliceStatService {

    PageResult<QmsCmpWarpageSliceStatRespVO> getPage(QmsCmpWarpageSliceStatPageReqVO reqVO);

    QmsCmpWarpageSliceStatRespVO get(Long id);

    Long create(QmsCmpWarpageSliceStatSaveReqVO reqVO);

    void update(QmsCmpWarpageSliceStatUpdateReqVO reqVO);

    void delete(Long id);

    QmsCmpWarpageSliceStatSyncRespVO sync(QmsCmpWarpageSliceStatSyncReqVO reqVO);

    QmsCmpWarpageSliceImportRespVO syncSlices(QmsCmpWarpageSliceImportReqVO reqVO);
}
