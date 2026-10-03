package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionScopeDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsNcDispositionScopeMapper extends BaseMapperX<QmsNcDispositionScopeDO> {

    default List<QmsNcDispositionScopeDO> selectListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<QmsNcDispositionScopeDO>()
                .eq(QmsNcDispositionScopeDO::getExecutionId, executionId)
                .eq(QmsNcDispositionScopeDO::getDeleted, false)
                .orderByAsc(QmsNcDispositionScopeDO::getId));
    }

    default List<QmsNcDispositionScopeDO> selectPackagingRelevantListByPieceNos(Collection<String> pieceNos) {
        if (pieceNos == null || pieceNos.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsNcDispositionScopeDO>()
                .and(wrapper -> wrapper.in(QmsNcDispositionScopeDO::getPieceNo, pieceNos)
                        .or().in(QmsNcDispositionScopeDO::getSourceObjectNo, pieceNos)
                        .or().in(QmsNcDispositionScopeDO::getObjectKey, pieceNos))
                .eq(QmsNcDispositionScopeDO::getScopeLevel, "PIECE")
                .and(wrapper -> wrapper.ne(QmsNcDispositionScopeDO::getScopeRole, "PICK_OUTSIDE_SCRAP")
                        .or().isNull(QmsNcDispositionScopeDO::getScopeRole))
                .eq(QmsNcDispositionScopeDO::getDeleted, false)
                .orderByAsc(QmsNcDispositionScopeDO::getNcRecordId)
                .orderByAsc(QmsNcDispositionScopeDO::getId));
    }
}
