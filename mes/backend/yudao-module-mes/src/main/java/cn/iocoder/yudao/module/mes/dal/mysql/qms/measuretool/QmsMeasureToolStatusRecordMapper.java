package cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolStatusRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolStatusRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsMeasureToolStatusRecordMapper extends BaseMapperX<QmsMeasureToolStatusRecordDO> {

    default PageResult<QmsMeasureToolStatusRecordDO> selectPage(QmsMeasureToolStatusRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsMeasureToolStatusRecordDO>()
                .eq(QmsMeasureToolStatusRecordDO::getLedgerId, reqVO.getLedgerId())
                .orderByDesc(QmsMeasureToolStatusRecordDO::getHandleTime)
                .orderByDesc(QmsMeasureToolStatusRecordDO::getId));
    }

}
