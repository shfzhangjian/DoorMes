package cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolApplyPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolApplyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsMeasureToolApplyMapper extends BaseMapperX<QmsMeasureToolApplyDO> {

    default PageResult<QmsMeasureToolApplyDO> selectPage(QmsMeasureToolApplyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsMeasureToolApplyDO>()
                .eqIfPresent(QmsMeasureToolApplyDO::getApplyNo, reqVO.getApplyNo())
                .likeIfPresent(QmsMeasureToolApplyDO::getToolName, reqVO.getToolName())
                .eqIfPresent(QmsMeasureToolApplyDO::getCategoryId, reqVO.getCategoryId())
                .likeIfPresent(QmsMeasureToolApplyDO::getApplyDepartment, reqVO.getApplyDepartment())
                .likeIfPresent(QmsMeasureToolApplyDO::getApplicantName, reqVO.getApplicantName())
                .eqIfPresent(QmsMeasureToolApplyDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(QmsMeasureToolApplyDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(QmsMeasureToolApplyDO::getId));
    }

}
