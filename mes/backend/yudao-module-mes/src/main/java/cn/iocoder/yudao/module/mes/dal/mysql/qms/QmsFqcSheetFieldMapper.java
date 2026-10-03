package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSheetFieldDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcSheetFieldMapper extends BaseMapperX<QmsFqcSheetFieldDO> {

    default List<QmsFqcSheetFieldDO> selectListBySectionIds(Collection<Long> sectionIds) {
        return selectList(new LambdaQueryWrapper<QmsFqcSheetFieldDO>()
                .in(QmsFqcSheetFieldDO::getSectionId, sectionIds)
                .orderByAsc(QmsFqcSheetFieldDO::getSort)
                .orderByAsc(QmsFqcSheetFieldDO::getId));
    }
}
