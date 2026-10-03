package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSheetSectionDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcSheetSectionMapper extends BaseMapperX<QmsFqcSheetSectionDO> {

    default List<QmsFqcSheetSectionDO> selectListByTemplateId(Long templateId) {
        return selectList(new LambdaQueryWrapper<QmsFqcSheetSectionDO>()
                .eq(QmsFqcSheetSectionDO::getTemplateId, templateId)
                .orderByAsc(QmsFqcSheetSectionDO::getSort)
                .orderByAsc(QmsFqcSheetSectionDO::getId));
    }

    default List<QmsFqcSheetSectionDO> selectListByTemplateIds(Collection<Long> templateIds) {
        return selectList(new LambdaQueryWrapper<QmsFqcSheetSectionDO>()
                .in(QmsFqcSheetSectionDO::getTemplateId, templateIds)
                .orderByAsc(QmsFqcSheetSectionDO::getSort)
                .orderByAsc(QmsFqcSheetSectionDO::getId));
    }
}
