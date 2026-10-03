package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierReviewExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierReviewPlanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierReviewPlanRespVO;

public interface SrmSupplierReviewPlanService {

    Long initYearPlan(Integer planYear);

    SrmSupplierReviewPlanRespVO.YearPlan getYearPlan(Integer planYear);

    Long addSupplierLine(SrmSupplierReviewPlanReqVO.AddSupplierLine reqVO);

    void updateLineContact(SrmSupplierReviewPlanReqVO.UpdateLineContact reqVO);

    SrmSupplierReviewPlanRespVO.DeleteAnnualPlansResult deleteAnnualPlans(
            SrmSupplierReviewPlanReqVO.DeleteAnnualPlans reqVO);

    SrmSupplierReviewPlanRespVO.MonthPlan getMonthPlan(Long id);

    void updateMonthPlan(SrmSupplierReviewPlanReqVO.MonthSave reqVO);

    /**
     * 保存现场考察记录（审核类别/审定日期/审核说明/审核附件），仅更新审核相关字段。
     */
    void saveSiteInspection(SrmSupplierReviewPlanReqVO.SiteInspectionSave reqVO);

    void clearMonthPlan(Long id);

    void adjustMonthStatus(SrmSupplierReviewPlanReqVO.StatusAdjust reqVO);

    Long createReply(SrmSupplierReviewPlanReqVO.ReplyCreate reqVO);

    PageResult<SrmSupplierReviewPlanRespVO.ExecutionItem> getExecutionPage(
            SrmSupplierReviewExecutionPageReqVO reqVO);

}
