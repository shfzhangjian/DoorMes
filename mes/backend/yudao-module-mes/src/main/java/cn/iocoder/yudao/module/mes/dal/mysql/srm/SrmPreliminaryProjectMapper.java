package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryProjectDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPreliminaryProjectMapper extends BaseMapperX<SrmPreliminaryProjectDO> {

    default PageResult<SrmPreliminaryProjectDO> selectPage(SrmPreliminaryProjectPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmPreliminaryProjectDO>()
                .likeIfPresent(SrmPreliminaryProjectDO::getProjectCode, reqVO.getProjectCode())
                .likeIfPresent(SrmPreliminaryProjectDO::getProjectName, reqVO.getProjectName())
                .eqIfPresent(SrmPreliminaryProjectDO::getStatus, reqVO.getStatus())
                .orderByDesc(SrmPreliminaryProjectDO::getUpdateTime)
                .orderByDesc(SrmPreliminaryProjectDO::getId));
    }

    default SrmPreliminaryProjectDO selectByProjectCode(String projectCode) {
        return selectOne(SrmPreliminaryProjectDO::getProjectCode, projectCode);
    }

    default List<SrmPreliminaryProjectDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<SrmPreliminaryProjectDO>()
                .eq(SrmPreliminaryProjectDO::getStatus, "ENABLED")
                .orderByAsc(SrmPreliminaryProjectDO::getProjectCode)
                .orderByDesc(SrmPreliminaryProjectDO::getId));
    }

}
