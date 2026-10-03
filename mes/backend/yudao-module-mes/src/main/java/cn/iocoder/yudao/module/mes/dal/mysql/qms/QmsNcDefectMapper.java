package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDefectDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsNcDefectMapper extends BaseMapperX<QmsNcDefectDO> {

    default List<QmsNcDefectDO> selectListByNcRecordId(Long ncRecordId) {
        return selectList(new LambdaQueryWrapperX<QmsNcDefectDO>()
                .eq(QmsNcDefectDO::getNcRecordId, ncRecordId)
                .orderByDesc(QmsNcDefectDO::getPrimaryFlag)
                .orderByAsc(QmsNcDefectDO::getSort)
                .orderByAsc(QmsNcDefectDO::getId));
    }
}
