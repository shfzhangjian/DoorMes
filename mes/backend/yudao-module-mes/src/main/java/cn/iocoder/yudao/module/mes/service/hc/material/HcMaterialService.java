package cn.iocoder.yudao.module.mes.service.hc.material;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialBindingPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialExtAttrDO;
import java.util.List;

public interface HcMaterialService {
    Long createHcMaterial(HcMaterialSaveReqVO createReqVO);
    void updateHcMaterial(HcMaterialSaveReqVO updateReqVO);
    void deleteHcMaterial(Long id);
    void deleteHcMaterialListByIds(List<Long> ids);
    HcMaterialDO getHcMaterial(Long id);
    String generateMaterialCode(HcMaterialSaveReqVO reqVO);
    HcMaterialBindingPreviewRespVO getHcMaterialBindingPreview(Long id);
    HcMaterialBindingPreviewRespVO previewHcMaterialBinding(HcMaterialSaveReqVO reqVO);
    List<HcMaterialDO> getHcMaterialSimpleList();
    List<HcMaterialDO> getHcMaterialList(HcMaterialPageReqVO reqVO);
    PageResult<HcMaterialDO> getHcMaterialPage(HcMaterialPageReqVO pageReqVO);
    List<HcMaterialExtAttrDO> getHcMaterialExtAttrListByParentId(Long parentId);
}
