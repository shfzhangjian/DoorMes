package cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolCalibrationTaskDO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsMeasureToolCalibrationTaskMapper extends BaseMapperX<QmsMeasureToolCalibrationTaskDO> {

    default PageResult<QmsMeasureToolCalibrationTaskDO> selectPage(QmsMeasureToolCalibrationTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsMeasureToolCalibrationTaskDO>()
                .eqIfPresent(QmsMeasureToolCalibrationTaskDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(QmsMeasureToolCalibrationTaskDO::getToolCode, reqVO.getToolCode())
                .likeIfPresent(QmsMeasureToolCalibrationTaskDO::getToolName, reqVO.getToolName())
                .eqIfPresent(QmsMeasureToolCalibrationTaskDO::getCategoryId, reqVO.getCategoryId())
                .likeIfPresent(QmsMeasureToolCalibrationTaskDO::getUsingDepartment, reqVO.getUsingDepartment())
                .likeIfPresent(QmsMeasureToolCalibrationTaskDO::getKeeperName, reqVO.getKeeperName())
                .eqIfPresent(QmsMeasureToolCalibrationTaskDO::getWarningStatus, reqVO.getWarningStatus())
                .eqIfPresent(QmsMeasureToolCalibrationTaskDO::getTaskStatus, reqVO.getTaskStatus())
                .betweenIfPresent(QmsMeasureToolCalibrationTaskDO::getDueDate, reqVO.getDueDate())
                .orderByAsc(QmsMeasureToolCalibrationTaskDO::getDueDate)
                .orderByDesc(QmsMeasureToolCalibrationTaskDO::getId));
    }

    default QmsMeasureToolCalibrationTaskDO selectOpenTask(Long ledgerId, LocalDate dueDate) {
        return selectOne(new LambdaQueryWrapperX<QmsMeasureToolCalibrationTaskDO>()
                .eq(QmsMeasureToolCalibrationTaskDO::getLedgerId, ledgerId)
                .eq(QmsMeasureToolCalibrationTaskDO::getDueDate, dueDate)
                .notIn(QmsMeasureToolCalibrationTaskDO::getTaskStatus, List.of("COMPLETED", "CANCELLED"))
                .last("LIMIT 1"));
    }

    default QmsMeasureToolCalibrationTaskDO selectNonCancelledTaskInMonth(Long ledgerId,
                                                                          LocalDate monthStart,
                                                                          LocalDate monthEnd) {
        return selectOne(new LambdaQueryWrapperX<QmsMeasureToolCalibrationTaskDO>()
                .eq(QmsMeasureToolCalibrationTaskDO::getLedgerId, ledgerId)
                .ge(QmsMeasureToolCalibrationTaskDO::getDueDate, monthStart)
                .le(QmsMeasureToolCalibrationTaskDO::getDueDate, monthEnd)
                .ne(QmsMeasureToolCalibrationTaskDO::getTaskStatus, "CANCELLED")
                .last("LIMIT 1"));
    }

    default List<QmsMeasureToolCalibrationTaskDO> selectTasksByMonth(LocalDate startDate, LocalDate endDate,
                                                                      Long categoryId, String usingDepartment) {
        return selectList(new LambdaQueryWrapperX<QmsMeasureToolCalibrationTaskDO>()
                .eqIfPresent(QmsMeasureToolCalibrationTaskDO::getCategoryId, categoryId)
                .likeIfPresent(QmsMeasureToolCalibrationTaskDO::getUsingDepartment, usingDepartment)
                .ge(QmsMeasureToolCalibrationTaskDO::getDueDate, startDate)
                .le(QmsMeasureToolCalibrationTaskDO::getDueDate, endDate));
    }

}
