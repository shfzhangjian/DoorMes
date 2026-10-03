package cn.iocoder.yudao.module.mes.service.hc.bom;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomProductImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomItemDO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface HcBomService {
    Long createHcBom(HcBomSaveReqVO createReqVO);
    void updateHcBom(HcBomSaveReqVO updateReqVO);
    void deleteHcBom(Long id);
    void deleteHcBomListByIds(List<Long> ids);
    HcBomDO getHcBom(Long id);
    List<HcBomDO> getHcBomSimpleList();
    List<HcBomDO> getHcBomSimpleListByMaterialId(Long materialId);
    List<HcBomDO> getHcBomProductModelOptionList(String keyword, Long productMaterialId);
    List<HcBomDO> getHcBomList(HcBomPageReqVO reqVO);
    PageResult<HcBomDO> getHcBomPage(HcBomPageReqVO pageReqVO);
    List<HcBomItemDO> getHcBomItemListByParentId(Long parentId);
    HcBomProductImportRespVO importProductBom(MultipartFile file, Boolean overwrite) throws IOException;
}
