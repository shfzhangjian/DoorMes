package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingAddInnerItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingAddOuterUnitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingCreateInnerUnitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingCreateOuterBoxReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingInnerUnitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingOuterBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingPassWorkRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingPassWorkSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSourceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingUnitActionReqVO;
import java.util.List;

public interface HcPackagingConsoleService {

    List<HcPackagingTaskRespVO> getTaskList(HcPackagingTaskPageReqVO reqVO);

    Long start(HcPackagingStartReqVO reqVO);

    List<HcPackagingPassWorkRespVO> getPassWorkList(Long planId, Long planOperationId);

    Long savePassWork(HcPackagingPassWorkSaveReqVO reqVO);

    Long confirmPassWork(HcPackagingPassWorkSaveReqVO reqVO);

    HcPackagingSummaryRespVO getSummary(Long planId, Long planOperationId);

    List<HcPackagingSourceRespVO> getSourceList(Long planId, Long planOperationId);

    HcPackagingSourceRespVO scanSource(String sliceBatchNo);

    List<HcPackagingInnerUnitRespVO> getInnerUnitList(Long planOperationId);

    Long createInnerUnit(HcPackagingCreateInnerUnitReqVO reqVO);

    Long addInnerItem(HcPackagingAddInnerItemReqVO reqVO);

    Long confirmInnerUnit(HcPackagingUnitActionReqVO reqVO);

    Long printInnerUnit(HcPackagingUnitActionReqVO reqVO);

    Long reviewInnerUnit(HcPackagingUnitActionReqVO reqVO);

    List<HcPackagingOuterBoxRespVO> getOuterBoxList(Long planOperationId);

    Long createOuterBox(HcPackagingCreateOuterBoxReqVO reqVO);

    Long addOuterUnit(HcPackagingAddOuterUnitReqVO reqVO);

    Long confirmOuterBox(HcPackagingUnitActionReqVO reqVO);

    Long printOuterBox(HcPackagingUnitActionReqVO reqVO);

    Long reviewOuterBox(HcPackagingUnitActionReqVO reqVO);

    Long submit(HcPackagingSubmitReqVO reqVO);
}
