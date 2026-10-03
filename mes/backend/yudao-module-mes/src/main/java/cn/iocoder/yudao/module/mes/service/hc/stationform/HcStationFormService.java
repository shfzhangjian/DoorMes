package cn.iocoder.yudao.module.mes.service.hc.stationform;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormConfigPackageImportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormConfigPackageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormImportConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormProcessOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormItemDO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcStationFormService {

    Long createHcStationForm(HcStationFormSaveReqVO createReqVO);

    void updateHcStationForm(HcStationFormSaveReqVO updateReqVO);

    void deleteHcStationForm(Long id);

    void deleteHcStationFormListByIds(List<Long> ids);

    HcStationFormDO getHcStationForm(Long id);

    List<HcStationFormItemDO> getHcStationFormItemList(Long formId);

    List<HcStationFormDO> getHcStationFormSimpleList(String processCode);

    /**
     * 按业务上下文解析可用于正式报工的动态表单。
     * <p>
     * DEV 配置只允许用于配置维护或联调，不能被正式报工入口解析到。
     */
    HcStationFormDO resolvePublishedHcStationForm(String processCode, String modelCode,
                                                   String formType, String grindingPass);

    List<HcStationFormProcessOptionRespVO> getReportProcessOptions();

    List<HcStationFormDO> getHcStationFormList(HcStationFormPageReqVO reqVO);

    PageResult<HcStationFormDO> getHcStationFormPage(HcStationFormPageReqVO pageReqVO);

    HcStationFormImportRespVO previewExcelImport(MultipartFile file, String formCode, String formName,
                                                 String processCode, String triggerTimingCode,
                                                 String presetTemplate) throws IOException;

    HcStationFormImportRespVO confirmExcelImport(HcStationFormImportConfirmReqVO reqVO);

    byte[] exportPreviewExcel(HcStationFormSaveReqVO form) throws IOException;

    HcStationFormImportRespVO importPreviewExcel(MultipartFile file, String formJson) throws IOException;

    byte[] exportConfigPackage(List<Long> ids);

    HcStationFormConfigPackageRespVO previewConfigPackageImport(MultipartFile file) throws IOException;

    HcStationFormConfigPackageRespVO confirmConfigPackageImport(HcStationFormConfigPackageImportReqVO reqVO);
}
