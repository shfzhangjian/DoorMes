package cn.iocoder.yudao.module.mes.service.hc.visualprintdesigner;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintCustomerInfoPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintCustomerInfoRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintCustomerInfoSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintDesignAttachReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintDesignRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintDesignSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintFieldOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintImportRespVO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcVisualPrintDesignerService {

    Long createCustomerInfo(HcVisualPrintCustomerInfoSaveReqVO createReqVO);

    void updateCustomerInfo(HcVisualPrintCustomerInfoSaveReqVO updateReqVO);

    void deleteCustomerInfo(Long id, Long productItemId);

    HcVisualPrintCustomerInfoRespVO getCustomerInfoDetail(Long id);

    PageResult<HcVisualPrintCustomerInfoRespVO> getCustomerInfoPage(HcVisualPrintCustomerInfoPageReqVO pageReqVO);

    List<HcVisualPrintCustomerInfoRespVO> getCustomerInfoList(HcVisualPrintCustomerInfoPageReqVO reqVO);

    HcVisualPrintImportRespVO importCustomerInfoExcel(MultipartFile file) throws IOException;

    List<HcVisualPrintFieldOptionRespVO> getFieldOptions();

    List<HcVisualPrintDesignRespVO> getDesigns(Long customerInfoId, Long productItemId);

    HcVisualPrintDesignRespVO getDesign(Long customerInfoId, Long productItemId, String labelKind);

    Long saveDesign(HcVisualPrintDesignSaveReqVO saveReqVO);

    HcVisualPrintDesignRespVO attachDesign(HcVisualPrintDesignAttachReqVO attachReqVO);

}
