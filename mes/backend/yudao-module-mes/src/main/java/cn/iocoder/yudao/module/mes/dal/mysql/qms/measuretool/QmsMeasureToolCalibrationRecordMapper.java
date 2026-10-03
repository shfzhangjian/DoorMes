package cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationMonthlySummaryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolCalibrationRecordDO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsMeasureToolCalibrationRecordMapper extends BaseMapperX<QmsMeasureToolCalibrationRecordDO> {

    default PageResult<QmsMeasureToolCalibrationRecordDO> selectPage(QmsMeasureToolCalibrationRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsMeasureToolCalibrationRecordDO>()
                .eqIfPresent(QmsMeasureToolCalibrationRecordDO::getLedgerId, reqVO.getLedgerId())
                .eqIfPresent(QmsMeasureToolCalibrationRecordDO::getRecordNo, reqVO.getRecordNo())
                .eqIfPresent(QmsMeasureToolCalibrationRecordDO::getToolCode, reqVO.getToolCode())
                .likeIfPresent(QmsMeasureToolCalibrationRecordDO::getToolName, reqVO.getToolName())
                .eqIfPresent(QmsMeasureToolCalibrationRecordDO::getCategoryId, reqVO.getCategoryId())
                .likeIfPresent(QmsMeasureToolCalibrationRecordDO::getUsingDepartment, reqVO.getUsingDepartment())
                .likeIfPresent(QmsMeasureToolCalibrationRecordDO::getKeeperName, reqVO.getKeeperName())
                .eqIfPresent(QmsMeasureToolCalibrationRecordDO::getCalibrationResult, reqVO.getCalibrationResult())
                .eqIfPresent(QmsMeasureToolCalibrationRecordDO::getSourceType, reqVO.getSourceType())
                .betweenIfPresent(QmsMeasureToolCalibrationRecordDO::getCalibrationDate, reqVO.getCalibrationDate())
                .orderByDesc(QmsMeasureToolCalibrationRecordDO::getCalibrationDate)
                .orderByDesc(QmsMeasureToolCalibrationRecordDO::getId));
    }

    default List<QmsMeasureToolCalibrationRecordDO> selectSummaryList(QmsMeasureToolCalibrationMonthlySummaryReqVO reqVO,
                                                                       LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<QmsMeasureToolCalibrationRecordDO>()
                .eqIfPresent(QmsMeasureToolCalibrationRecordDO::getCategoryId, reqVO.getCategoryId())
                .likeIfPresent(QmsMeasureToolCalibrationRecordDO::getUsingDepartment, reqVO.getUsingDepartment())
                .ge(QmsMeasureToolCalibrationRecordDO::getCalibrationDate, startDate)
                .le(QmsMeasureToolCalibrationRecordDO::getCalibrationDate, endDate));
    }

    default QmsMeasureToolCalibrationRecordDO selectLatestByLedgerId(Long ledgerId) {
        return selectOne(new LambdaQueryWrapperX<QmsMeasureToolCalibrationRecordDO>()
                .eq(QmsMeasureToolCalibrationRecordDO::getLedgerId, ledgerId)
                .orderByDesc(QmsMeasureToolCalibrationRecordDO::getCalibrationDate)
                .orderByDesc(QmsMeasureToolCalibrationRecordDO::getId)
                .last("LIMIT 1"));
    }

}
