package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDefectCauseDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsDefectCauseMapper extends BaseMapperX<QmsDefectCauseDO> {

    default List<QmsDefectCauseDO> selectListByDefectCodeId(Long defectCodeId) {
        return selectList(new LambdaQueryWrapper<QmsDefectCauseDO>()
                .eq(QmsDefectCauseDO::getDefectCodeId, defectCodeId)
                .orderByAsc(QmsDefectCauseDO::getSort)
                .orderByAsc(QmsDefectCauseDO::getId));
    }

    default int deleteByDefectCodeId(Long defectCodeId) {
        return delete(new LambdaQueryWrapper<QmsDefectCauseDO>()
                .eq(QmsDefectCauseDO::getDefectCodeId, defectCodeId));
    }
}
