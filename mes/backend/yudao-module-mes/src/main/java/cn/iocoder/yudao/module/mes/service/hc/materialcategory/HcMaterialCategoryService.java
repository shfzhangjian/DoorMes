package cn.iocoder.yudao.module.mes.service.hc.materialcategory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo.HcMaterialCategoryPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo.HcMaterialCategorySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.materialcategory.HcMaterialCategoryDO;
import java.util.List;

public interface HcMaterialCategoryService {
    Long createHcMaterialCategory(HcMaterialCategorySaveReqVO createReqVO);

    void updateHcMaterialCategory(HcMaterialCategorySaveReqVO updateReqVO);

    void deleteHcMaterialCategory(Long id);

    void deleteHcMaterialCategoryListByIds(List<Long> ids);

    void moveUp(Long id);

    void moveDown(Long id);

    HcMaterialCategoryDO getHcMaterialCategory(Long id);

    List<HcMaterialCategoryDO> getHcMaterialCategorySimpleList();

    List<HcMaterialCategoryDO> getHcMaterialCategoryList(HcMaterialCategoryPageReqVO reqVO);

    PageResult<HcMaterialCategoryDO> getHcMaterialCategoryPage(HcMaterialCategoryPageReqVO pageReqVO);
}