package cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCategoryListReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolCategoryDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsMeasureToolCategoryMapper extends BaseMapperX<QmsMeasureToolCategoryDO> {

    default List<QmsMeasureToolCategoryDO> selectList(QmsMeasureToolCategoryListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<QmsMeasureToolCategoryDO>()
                .eqIfPresent(QmsMeasureToolCategoryDO::getCategoryCode, reqVO.getCategoryCode())
                .likeIfPresent(QmsMeasureToolCategoryDO::getCategoryName, reqVO.getCategoryName())
                .eqIfPresent(QmsMeasureToolCategoryDO::getStatus, reqVO.getStatus())
                .orderByAsc(QmsMeasureToolCategoryDO::getParentId)
                .orderByAsc(QmsMeasureToolCategoryDO::getSort)
                .orderByDesc(QmsMeasureToolCategoryDO::getId));
    }

    default QmsMeasureToolCategoryDO selectByCode(String categoryCode) {
        return selectOne(QmsMeasureToolCategoryDO::getCategoryCode, categoryCode);
    }

    default QmsMeasureToolCategoryDO selectByName(String categoryName) {
        return selectOne(QmsMeasureToolCategoryDO::getCategoryName, categoryName);
    }

    default QmsMeasureToolCategoryDO selectByNameAndParentId(String categoryName, Long parentId) {
        return selectOne(new LambdaQueryWrapperX<QmsMeasureToolCategoryDO>()
                .eq(QmsMeasureToolCategoryDO::getCategoryName, categoryName)
                .eq(QmsMeasureToolCategoryDO::getParentId, parentId));
    }

    default long selectCountByParentId(Long parentId) {
        return selectCount(QmsMeasureToolCategoryDO::getParentId, parentId);
    }

}
