package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeListReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDefectCodeDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

@Mapper
public interface QmsDefectCodeMapper extends BaseMapperX<QmsDefectCodeDO> {

    default List<QmsDefectCodeDO> selectList(QmsDefectCodeListReqVO reqVO) {
        LambdaQueryWrapperX<QmsDefectCodeDO> wrapper = new LambdaQueryWrapperX<QmsDefectCodeDO>()
                .eqIfPresent(QmsDefectCodeDO::getType, reqVO.getType())
                .eqIfPresent(QmsDefectCodeDO::getStatus, reqVO.getStatus());
        if (StringUtils.hasText(reqVO.getName())) {
            wrapper.and(query -> {
                query.like(QmsDefectCodeDO::getName, reqVO.getName())
                        .or()
                        .like(QmsDefectCodeDO::getCode, reqVO.getName());
            });
        }
        wrapper.orderByAsc(QmsDefectCodeDO::getParentId);
        wrapper.orderByAsc(QmsDefectCodeDO::getSort);
        wrapper.orderByAsc(QmsDefectCodeDO::getId);
        return selectList(wrapper);
    }

    default Integer selectMaxSortByParentId(Long parentId) {
        LambdaQueryWrapperX<QmsDefectCodeDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(QmsDefectCodeDO::getParentId, parentId);
        wrapper.orderByDesc(QmsDefectCodeDO::getSort);
        wrapper.last("LIMIT 1");
        QmsDefectCodeDO entity = selectOne(wrapper);
        return entity == null || entity.getSort() == null ? 0 : entity.getSort();
    }

    default List<QmsDefectCodeDO> selectListByParentId(Long parentId) {
        LambdaQueryWrapperX<QmsDefectCodeDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(QmsDefectCodeDO::getParentId, parentId);
        wrapper.orderByAsc(QmsDefectCodeDO::getSort);
        wrapper.orderByAsc(QmsDefectCodeDO::getId);
        return selectList(wrapper);
    }
}
