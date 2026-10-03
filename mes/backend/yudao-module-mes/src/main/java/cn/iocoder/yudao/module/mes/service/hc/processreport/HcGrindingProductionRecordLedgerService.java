package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConsumableDefaultRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConsumableSyncReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConsumableSyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordLifeUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingProductionRecordDO;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcGrindingProductionRecordLedgerService {
    Long create(HcGrindingProductionRecordSaveReqVO reqVO);
    void update(HcGrindingProductionRecordSaveReqVO reqVO);
    void updateLife(HcGrindingProductionRecordLifeUpdateReqVO reqVO);
    void delete(Long id);
    void deleteByIds(List<Long> ids);
    Integer confirm(HcGrindingProductionRecordConfirmReqVO reqVO);
    HcGrindingProductionRecordDO get(Long id);
    HcGrindingProductionRecordConsumableDefaultRespVO getConsumableDefault(Long equipmentId,
                                                                             LocalDateTime completionTime);
    HcGrindingProductionRecordConsumableSyncRespVO getLatestConfirmedConsumableSync(Long equipmentId);
    HcGrindingProductionRecordConsumableSyncRespVO syncLatestConfirmedConsumableState(
            HcGrindingProductionRecordConsumableSyncReqVO reqVO);
    PageResult<HcGrindingProductionRecordDO> getPage(HcGrindingProductionRecordPageReqVO reqVO);
    List<HcGrindingProductionRecordExcelVO> buildExportList(HcGrindingProductionRecordPageReqVO reqVO);
    HcGrindingProductionRecordImportRespVO importExcel(MultipartFile file) throws IOException;
    void syncAutoRecord(HcGrindingProductionRecordDO sourceRecord);
    void syncAutoRecords(List<HcGrindingProductionRecordDO> sourceRecords);
    void syncAutoRecordTime(String passType, Long sourceDetailId, LocalDateTime recordTime);
    void syncAutoRecordTime(String sourceBizType, String passType, Long sourceDetailId, LocalDateTime recordTime);
    void deleteRecordBySource(String passType, Long sourceDetailId);
    void deleteRecordBySource(String sourceBizType, String passType, Long sourceDetailId);
}
