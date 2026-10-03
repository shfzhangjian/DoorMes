package cn.iocoder.yudao.module.mes.service.hc.guideclothrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRuntimeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRndConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.guideclothrecord.HcGuideClothRecordDO;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcGuideClothRecordService {

    Long create(HcGuideClothRecordSaveReqVO reqVO);

    void update(HcGuideClothRecordSaveReqVO reqVO);

    void delete(Long id);

    void deleteByIds(List<Long> ids);

    HcGuideClothRecordDO get(Long id);

    List<HcGuideClothRecordDO> getList(HcGuideClothRecordPageReqVO reqVO);

    PageResult<HcGuideClothRecordDO> getPage(HcGuideClothRecordPageReqVO reqVO);

    List<HcGuideClothRecordImportExcelVO> buildExportList(HcGuideClothRecordPageReqVO reqVO);

    HcGuideClothRecordImportRespVO importExcel(MultipartFile file, Boolean confirmClear) throws IOException;

    HcGuideClothRuntimeRespVO getRuntimeByMotherModelCode(String motherModelCode);

    HcGuideClothRuntimeRespVO getRuntimeByMotherModelCode(String motherModelCode, Long equipmentId);

    HcGuideClothRuntimeRespVO getRndRuntime(Long guideClothRecordId);

    String saveRndConsume(HcGuideClothRndConsumeSaveReqVO reqVO);

    Integer handleWetReport(String motherModelCode, String planNo, String petBatchNo, String guideClothBatchNo,
                            String petModel, String replaceReason, Integer submittedUseCount, boolean changed,
                            String remark, Long tenantId, LocalDateTime replaceTime);

    Integer handleWetReport(String motherModelCode, String planNo, String petBatchNo, String guideClothBatchNo,
                            String petModel, String replaceReason, Integer submittedUseCount, boolean changed,
                            String remark, Long tenantId, LocalDateTime replaceTime, Long equipmentId,
                            String equipmentCode, String equipmentName, Long workCenterId, String workCenterCode,
                            String workCenterName, Long planId, Long planOperationId, String operationCode,
                            String operationName, BigDecimal receiveLength, Long reportId, Long operatorId,
                            String operatorName);
}
