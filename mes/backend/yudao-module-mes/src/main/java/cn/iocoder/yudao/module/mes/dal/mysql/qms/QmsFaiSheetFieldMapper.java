package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetFieldDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFaiSheetFieldMapper extends BaseMapperX<QmsFaiSheetFieldDO> {

    default List<QmsFaiSheetFieldDO> selectListBySectionIds(Collection<Long> sectionIds) {
        return selectList(new LambdaQueryWrapper<QmsFaiSheetFieldDO>()
                .in(QmsFaiSheetFieldDO::getSectionId, sectionIds)
                .orderByAsc(QmsFaiSheetFieldDO::getSort)
                .orderByAsc(QmsFaiSheetFieldDO::getId));
    }
}
