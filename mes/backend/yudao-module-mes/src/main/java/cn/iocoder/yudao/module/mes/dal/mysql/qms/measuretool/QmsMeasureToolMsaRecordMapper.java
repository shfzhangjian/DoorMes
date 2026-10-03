package cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolMsaRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolMsaRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsMeasureToolMsaRecordMapper extends BaseMapperX<QmsMeasureToolMsaRecordDO> {

    default PageResult<QmsMeasureToolMsaRecordDO> selectPage(QmsMeasureToolMsaRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsMeasureToolMsaRecordDO>()
                .eqIfPresent(QmsMeasureToolMsaRecordDO::getLedgerId, reqVO.getLedgerId())
                .eqIfPresent(QmsMeasureToolMsaRecordDO::getToolCode, reqVO.getToolCode())
                .likeIfPresent(QmsMeasureToolMsaRecordDO::getToolName, reqVO.getToolName())
                .eqIfPresent(QmsMeasureToolMsaRecordDO::getMsaResult, reqVO.getMsaResult())
                .betweenIfPresent(QmsMeasureToolMsaRecordDO::getMsaDate, reqVO.getMsaDate())
                .orderByDesc(QmsMeasureToolMsaRecordDO::getMsaDate)
                .orderByDesc(QmsMeasureToolMsaRecordDO::getId));
    }
}
