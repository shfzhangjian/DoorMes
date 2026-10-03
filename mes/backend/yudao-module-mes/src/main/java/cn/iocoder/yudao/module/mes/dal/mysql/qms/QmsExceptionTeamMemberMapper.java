package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionTeamMemberDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsExceptionTeamMemberMapper extends BaseMapperX<QmsExceptionTeamMemberDO> {

    default List<QmsExceptionTeamMemberDO> selectListByExceptionId(Long exceptionId) {
        return selectList(new LambdaQueryWrapperX<QmsExceptionTeamMemberDO>()
                .eq(QmsExceptionTeamMemberDO::getExceptionId, exceptionId)
                .orderByAsc(QmsExceptionTeamMemberDO::getId));
    }

    default List<QmsExceptionTeamMemberDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<QmsExceptionTeamMemberDO>()
                .eq(QmsExceptionTeamMemberDO::getUserId, userId));
    }
}
