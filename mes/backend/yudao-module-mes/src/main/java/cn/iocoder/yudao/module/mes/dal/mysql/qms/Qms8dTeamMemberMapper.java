package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dTeamMemberDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface Qms8dTeamMemberMapper extends BaseMapperX<Qms8dTeamMemberDO> {

    default List<Qms8dTeamMemberDO> selectListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapperX<Qms8dTeamMemberDO>()
                .eq(Qms8dTeamMemberDO::getReportId, reportId)
                .orderByAsc(Qms8dTeamMemberDO::getSort)
                .orderByAsc(Qms8dTeamMemberDO::getId));
    }

    default List<Qms8dTeamMemberDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<Qms8dTeamMemberDO>()
                .eq(Qms8dTeamMemberDO::getUserId, userId));
    }
}
