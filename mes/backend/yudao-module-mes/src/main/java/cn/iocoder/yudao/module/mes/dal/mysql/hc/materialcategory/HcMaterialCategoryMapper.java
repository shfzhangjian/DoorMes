package cn.iocoder.yudao.module.mes.dal.mysql.hc.materialcategory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo.HcMaterialCategoryPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.materialcategory.HcMaterialCategoryDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcMaterialCategoryMapper extends BaseMapperX<HcMaterialCategoryDO> {

    default PageResult<HcMaterialCategoryDO> selectPage(HcMaterialCategoryPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcMaterialCategoryDO> selectList(HcMaterialCategoryPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcMaterialCategoryDO> buildQuery(HcMaterialCategoryPageReqVO reqVO) {
        LambdaQueryWrapperX<HcMaterialCategoryDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(HcMaterialCategoryDO::getParentId, reqVO.getParentId());
        wrapper.likeIfPresent(HcMaterialCategoryDO::getCategoryCode, reqVO.getCategoryCode());
        wrapper.likeIfPresent(HcMaterialCategoryDO::getCategoryName, reqVO.getCategoryName());
        wrapper.likeIfPresent(HcMaterialCategoryDO::getCategoryCodePath, reqVO.getCategoryPath());
        wrapper.eqIfPresent(HcMaterialCategoryDO::getStatus, reqVO.getStatus());
        wrapper.orderByAsc(HcMaterialCategoryDO::getParentId);
        wrapper.orderByAsc(HcMaterialCategoryDO::getSort);
        wrapper.orderByAsc(HcMaterialCategoryDO::getId);
        return wrapper;
    }

    default List<HcMaterialCategoryDO> selectListByParentId(Long parentId) {
        LambdaQueryWrapperX<HcMaterialCategoryDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(HcMaterialCategoryDO::getParentId, parentId);
        wrapper.orderByAsc(HcMaterialCategoryDO::getSort);
        wrapper.orderByAsc(HcMaterialCategoryDO::getId);
        return selectList(wrapper);
    }

    default Integer selectMaxSortByParentId(Long parentId) {
        List<HcMaterialCategoryDO> list = selectListByParentId(parentId);
        if (list.isEmpty()) {
            return 0;
        }
        return list.get(list.size() - 1).getSort();
    }
}