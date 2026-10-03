package cn.iocoder.yudao.module.mes.dal.mysql.process;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.mes.controller.admin.process.vo.*;

/**
 * MES标准工序 Mapper
 *
 * @author 演示管理员
 */
@Mapper
public interface ProcessMapper extends BaseMapperX<ProcessDO> {

    default PageResult<ProcessDO> selectPage(ProcessPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProcessDO>()
                .eqIfPresent(ProcessDO::getCode, reqVO.getCode())
                .likeIfPresent(ProcessDO::getName, reqVO.getName())
                .eqIfPresent(ProcessDO::getWorkshopId, reqVO.getWorkshopId())
                .eqIfPresent(ProcessDO::getWorkshopCode, reqVO.getWorkshopCode())
                .likeIfPresent(ProcessDO::getWorkshopName, reqVO.getWorkshopName())
                .eqIfPresent(ProcessDO::getProcessType, reqVO.getProcessType())
                .eqIfPresent(ProcessDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ProcessDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ProcessDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProcessDO::getId));
    }

}
