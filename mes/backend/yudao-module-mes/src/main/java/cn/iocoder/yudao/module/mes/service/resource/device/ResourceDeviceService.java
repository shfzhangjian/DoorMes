package cn.iocoder.yudao.module.mes.service.resource.device;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceCategoryListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceCategorySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceExceptionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceExceptionProcessReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceExceptionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintCandidateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintCurrentMonthAppendReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintOrderExecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanExecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanWeekSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintStandardPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintStandardSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceCategoryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceExceptionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceExceptionPartDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceLedgerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintOrderItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintOrderPartDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceParamDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDevicePartDO;
import jakarta.validation.Valid;
import java.util.List;

public interface ResourceDeviceService {

    Long createCategory(@Valid ResourceDeviceCategorySaveReqVO reqVO);

    void updateCategory(@Valid ResourceDeviceCategorySaveReqVO reqVO);

    void deleteCategory(Long id);

    ResourceDeviceCategoryDO getCategory(Long id);

    List<ResourceDeviceCategoryDO> getCategoryList(ResourceDeviceCategoryListReqVO reqVO);

    Long createLedger(@Valid ResourceDeviceLedgerSaveReqVO reqVO);

    void updateLedger(@Valid ResourceDeviceLedgerSaveReqVO reqVO);

    void deleteLedger(Long id);

    ResourceDeviceLedgerDO getLedger(Long id);

    PageResult<ResourceDeviceLedgerDO> getLedgerPage(ResourceDeviceLedgerPageReqVO reqVO);

    List<ResourceDevicePartDO> getPartList(Long deviceId);

    List<ResourceDeviceParamDO> getParamList(Long deviceId);

    Long createStandard(@Valid ResourceDeviceMaintStandardSaveReqVO reqVO);

    void updateStandard(@Valid ResourceDeviceMaintStandardSaveReqVO reqVO);

    void deleteStandard(Long id);

    ResourceDeviceMaintStandardDO getStandard(Long id);

    PageResult<ResourceDeviceMaintStandardDO> getStandardPage(ResourceDeviceMaintStandardPageReqVO reqVO);

    List<ResourceDeviceMaintStandardItemDO> getStandardItemList(Long standardId);

    Long createMaintPlan(@Valid ResourceDeviceMaintPlanSaveReqVO reqVO);

    void updateMaintPlan(@Valid ResourceDeviceMaintPlanSaveReqVO reqVO);

    void deleteMaintPlan(Long id);

    ResourceDeviceMaintPlanDO getMaintPlan(Long id);

    PageResult<ResourceDeviceMaintPlanDO> getMaintPlanPage(ResourceDeviceMaintPlanPageReqVO reqVO);

    ResourceDeviceMaintPlanRespVO.Matrix getMaintPlanMatrix(Integer year, Long deviceId);

    Long saveMaintPlanWeek(@Valid ResourceDeviceMaintPlanWeekSaveReqVO reqVO);

    int copyMaintPlanYear(@Valid ResourceDeviceMaintPlanGenerateReqVO reqVO);

    int generateMaintOrdersFromPlan(List<Long> ids);

    ResourceDeviceImportRespVO importMaintPlanExcel(List<ResourceDeviceMaintPlanImportExcelVO> rows, Integer year, Boolean overwrite);

    Long executeMaintPlan(@Valid ResourceDeviceMaintPlanExecuteReqVO reqVO);

    int confirmMaintPlans(@Valid ResourceDeviceMaintPlanConfirmReqVO reqVO);

    Long createMaintOrder(@Valid ResourceDeviceMaintOrderSaveReqVO reqVO);

    void updateMaintOrder(@Valid ResourceDeviceMaintOrderSaveReqVO reqVO);

    void deleteMaintOrder(Long id);

    ResourceDeviceMaintOrderDO getMaintOrder(Long id);

    PageResult<ResourceDeviceMaintOrderDO> getMaintOrderPage(ResourceDeviceMaintOrderPageReqVO reqVO);

    Long executeMaintOrder(@Valid ResourceDeviceMaintOrderExecuteReqVO reqVO);

    List<ResourceDeviceMaintCandidateRespVO> getMaintCurrentMonthCandidates(ResourceDeviceMaintCandidateReqVO reqVO);

    int appendCurrentMonthMaintOrders(@Valid ResourceDeviceMaintCurrentMonthAppendReqVO reqVO);

    int confirmMaintOrders(@Valid ResourceDeviceMaintConfirmReqVO reqVO);

    List<ResourceDeviceMaintOrderItemDO> getMaintOrderItemList(Long taskId);

    List<ResourceDeviceMaintOrderPartDO> getMaintOrderPartList(Long taskId);

    ResourceDeviceMaintRecordDO getMaintRecord(Long id);

    PageResult<ResourceDeviceMaintRecordDO> getMaintRecordPage(ResourceDeviceMaintRecordPageReqVO reqVO);

    List<ResourceDeviceMaintRecordRespVO.MonthlySummary> getMaintRecordMonthlySummary(ResourceDeviceMaintRecordPageReqVO reqVO);

    ResourceDeviceMaintOrderDO getMaintOrderByRecordId(Long recordId);

    Long createException(@Valid ResourceDeviceExceptionSaveReqVO reqVO);

    void updateException(@Valid ResourceDeviceExceptionSaveReqVO reqVO);

    void processException(@Valid ResourceDeviceExceptionProcessReqVO reqVO);

    void deleteException(Long id);

    ResourceDeviceExceptionDO getException(Long id);

    PageResult<ResourceDeviceExceptionDO> getExceptionPage(ResourceDeviceExceptionPageReqVO reqVO);

    List<ResourceDeviceExceptionPartDO> getExceptionPartList(Long exceptionId);

}
