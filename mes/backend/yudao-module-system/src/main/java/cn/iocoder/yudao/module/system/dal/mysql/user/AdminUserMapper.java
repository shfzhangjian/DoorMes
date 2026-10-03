package cn.iocoder.yudao.module.system.dal.mysql.user;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AdminUserMapper extends BaseMapperX<AdminUserDO> {

    default AdminUserDO selectByUsername(String username) {
        return selectOne(AdminUserDO::getUsername, username);
    }

    default AdminUserDO selectByEmail(String email) {
        return selectOne(AdminUserDO::getEmail, email);
    }

    default AdminUserDO selectByMobile(String mobile) {
        return selectOne(AdminUserDO::getMobile, mobile);
    }

    default PageResult<AdminUserDO> selectPage(UserPageReqVO reqVO, Collection<Long> deptIds, Collection<Long> userIds) {
        LambdaQueryWrapperX<AdminUserDO> query = new LambdaQueryWrapperX<AdminUserDO>()
                .likeIfPresent(AdminUserDO::getUsername, reqVO.getUsername())
                .likeIfPresent(AdminUserDO::getNickname, reqVO.getNickname());
        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            query.and(wrapper -> wrapper
                    .like(AdminUserDO::getUsername, reqVO.getKeyword())
                    .or()
                    .like(AdminUserDO::getNickname, reqVO.getKeyword()));
        }
        return selectPage(reqVO, query
                .likeIfPresent(AdminUserDO::getMobile, reqVO.getMobile())
                .eqIfPresent(AdminUserDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AdminUserDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(AdminUserDO::getDeptId, deptIds)
                .inIfPresent(AdminUserDO::getId, userIds)
                .orderByDesc(AdminUserDO::getId));
    }

    default List<AdminUserDO> selectListByNickname(String nickname) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>().like(AdminUserDO::getNickname, nickname));
    }

    default List<AdminUserDO> selectListByStatus(Integer status) {
        return selectList(AdminUserDO::getStatus, status);
    }

    default List<AdminUserDO> selectListByOaEcologyReceiveEnabled(Collection<Long> userIds) {
        LambdaQueryWrapperX<AdminUserDO> query = new LambdaQueryWrapperX<>();
        query.eq(AdminUserDO::getStatus, CommonStatusEnum.ENABLE.getStatus());
        query.eq(AdminUserDO::getOaEcologyReceiveEnabled, true);
        query.isNotNull(AdminUserDO::getOaEcologyTenantKey);
        query.and(wrapper -> wrapper
                .isNotNull(AdminUserDO::getOaEcologyUserId)
                .or()
                .isNotNull(AdminUserDO::getOaEcologyWorkCode));
        query.inIfPresent(AdminUserDO::getId, userIds);
        return selectList(query.orderByAsc(AdminUserDO::getId));
    }

    default List<AdminUserDO> selectListByDeptIds(Collection<Long> deptIds) {
        return selectList(AdminUserDO::getDeptId, deptIds);
    }

}
