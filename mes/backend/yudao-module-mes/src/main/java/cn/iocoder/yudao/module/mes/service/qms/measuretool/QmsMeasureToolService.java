package cn.iocoder.yudao.module.mes.service.qms.measuretool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolApplyActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolApplyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolApplySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationDueHintRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationMonthlySummaryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationMonthlySummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskBatchConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskCancelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskCandidateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCategoryListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCategorySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerStatusUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolMaintainCalibrationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolMaintainMsaReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolMsaRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolStatusRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolApplyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolCalibrationRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolCalibrationTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolCategoryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolLedgerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolMsaRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolStatusRecordDO;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface QmsMeasureToolService {

    Long createCategory(@Valid QmsMeasureToolCategorySaveReqVO reqVO);

    void updateCategory(@Valid QmsMeasureToolCategorySaveReqVO reqVO);

    void deleteCategory(Long id);

    QmsMeasureToolCategoryDO getCategory(Long id);

    List<QmsMeasureToolCategoryDO> getCategoryList(QmsMeasureToolCategoryListReqVO reqVO);

    Long createLedger(@Valid QmsMeasureToolLedgerSaveReqVO reqVO);

    void updateLedger(@Valid QmsMeasureToolLedgerSaveReqVO reqVO);

    void updateLedgerStatus(@Valid QmsMeasureToolLedgerStatusUpdateReqVO reqVO);

    PageResult<QmsMeasureToolStatusRecordDO> getStatusRecordPage(QmsMeasureToolStatusRecordPageReqVO reqVO);

    void deleteLedger(Long id);

    QmsMeasureToolLedgerDO getLedger(Long id);

    PageResult<QmsMeasureToolLedgerDO> getLedgerPage(QmsMeasureToolLedgerPageReqVO reqVO);

    /** 获取人员、使用部门和校准机构的历史手工候选值。 */
    QmsMeasureToolLedgerSelectOptionRespVO getLedgerSelectOptions();

    int importLedgerExcel(MultipartFile file) throws IOException;

    /** 维护当前校准快照，并将被替换的快照归入校准历史。 */
    void maintainCalibration(@Valid QmsMeasureToolMaintainCalibrationReqVO reqVO);

    /** 维护当前 MSA 快照，并将被替换的快照归入 MSA 历史。 */
    void maintainMsa(@Valid QmsMeasureToolMaintainMsaReqVO reqVO);

    PageResult<QmsMeasureToolMsaRecordDO> getMsaRecordPage(QmsMeasureToolMsaRecordPageReqVO reqVO);

    Long createApply(@Valid QmsMeasureToolApplySaveReqVO reqVO);

    void updateApply(@Valid QmsMeasureToolApplySaveReqVO reqVO);

    void deleteApply(Long id);

    void submitApply(@Valid QmsMeasureToolApplyActionReqVO reqVO);

    Long approveApply(@Valid QmsMeasureToolApplyActionReqVO reqVO);

    void rejectApply(@Valid QmsMeasureToolApplyActionReqVO reqVO);

    QmsMeasureToolApplyDO getApply(Long id);

    PageResult<QmsMeasureToolApplyDO> getApplyPage(QmsMeasureToolApplyPageReqVO reqVO);

    int generateCalibrationTasks(@Valid QmsMeasureToolCalibrationTaskGenerateReqVO reqVO);

    List<QmsMeasureToolCalibrationTaskCandidateRespVO> getCalibrationTaskCandidates(
            QmsMeasureToolCalibrationTaskCandidateReqVO reqVO);

    QmsMeasureToolCalibrationDueHintRespVO getCalibrationDueHint(QmsMeasureToolCalibrationTaskCandidateReqVO reqVO);

    void cancelCalibrationTask(@Valid QmsMeasureToolCalibrationTaskCancelReqVO reqVO);

    int batchConfirmCalibrationTasks(@Valid QmsMeasureToolCalibrationTaskBatchConfirmReqVO reqVO);

    QmsMeasureToolCalibrationTaskDO getCalibrationTask(Long id);

    PageResult<QmsMeasureToolCalibrationTaskDO> getCalibrationTaskPage(QmsMeasureToolCalibrationTaskPageReqVO reqVO);

    Long createCalibrationRecord(@Valid QmsMeasureToolCalibrationRecordSaveReqVO reqVO);

    void updateCalibrationRecord(@Valid QmsMeasureToolCalibrationRecordSaveReqVO reqVO);

    void deleteCalibrationRecord(Long id);

    QmsMeasureToolCalibrationRecordDO getCalibrationRecord(Long id);

    PageResult<QmsMeasureToolCalibrationRecordDO> getCalibrationRecordPage(QmsMeasureToolCalibrationRecordPageReqVO reqVO);

    List<QmsMeasureToolCalibrationMonthlySummaryRespVO> getCalibrationMonthlySummary(QmsMeasureToolCalibrationMonthlySummaryReqVO reqVO);

}
