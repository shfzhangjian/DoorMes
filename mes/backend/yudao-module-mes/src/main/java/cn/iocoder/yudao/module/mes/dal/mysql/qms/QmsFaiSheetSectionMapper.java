package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetSectionDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFaiSheetSectionMapper extends BaseMapperX<QmsFaiSheetSectionDO> {

    default List<QmsFaiSheetSectionDO> selectListByTemplateId(Long templateId) {
        return selectList(new LambdaQueryWrapper<QmsFaiSheetSectionDO>()
                .eq(QmsFaiSheetSectionDO::getTemplateId, templateId)
                .orderByAsc(QmsFaiSheetSectionDO::getSort)
                .orderByAsc(QmsFaiSheetSectionDO::getId));
    }

    default List<QmsFaiSheetSectionDO> selectListByTemplateIds(Collection<Long> templateIds) {
        return selectList(new LambdaQueryWrapper<QmsFaiSheetSectionDO>()
                .in(QmsFaiSheetSectionDO::getTemplateId, templateIds)
                .orderByAsc(QmsFaiSheetSectionDO::getSort)
                .orderByAsc(QmsFaiSheetSectionDO::getId));
    }
}
