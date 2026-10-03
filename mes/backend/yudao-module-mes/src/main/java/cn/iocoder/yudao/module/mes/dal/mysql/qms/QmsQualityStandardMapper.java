package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

@Mapper
public interface QmsQualityStandardMapper extends BaseMapperX<QmsQualityStandardDO> {

    default PageResult<QmsQualityStandardDO> selectPage(QmsQualityStandardPageReqVO reqVO) {
        LambdaQueryWrapperX<QmsQualityStandardDO> wrapper = new LambdaQueryWrapperX<QmsQualityStandardDO>()
                .eqIfPresent(QmsQualityStandardDO::getApplyType, reqVO.getApplyType())
                .eqIfPresent(QmsQualityStandardDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QmsQualityStandardDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(QmsQualityStandardDO::getProcessId, reqVO.getProcessId())
                .eqIfPresent(QmsQualityStandardDO::getGlueBoardModel, reqVO.getGlueBoardModel())
                .likeIfPresent(QmsQualityStandardDO::getProductModelCode, reqVO.getProductModelCode())
                .likeIfPresent(QmsQualityStandardDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(QmsQualityStandardDO::getMaterialName, reqVO.getMaterialName());
        if (StringUtils.hasText(reqVO.getStandardName())) {
            wrapper.and(query -> query.like(QmsQualityStandardDO::getStandardName, reqVO.getStandardName())
                    .or()
                    .like(QmsQualityStandardDO::getStandardNo, reqVO.getStandardName()));
        }
        if (StringUtils.hasText(reqVO.getProcessKeyword())) {
            wrapper.and(query -> query.like(QmsQualityStandardDO::getProcessCode, reqVO.getProcessKeyword())
                    .or()
                    .like(QmsQualityStandardDO::getProcessName, reqVO.getProcessKeyword()));
        }
        if (StringUtils.hasText(reqVO.getProductModelKeyword())) {
            wrapper.and(query -> query.like(QmsQualityStandardDO::getProductModelCode, reqVO.getProductModelKeyword())
                    .or()
                    .like(QmsQualityStandardDO::getProductModelName, reqVO.getProductModelKeyword()));
        }
        wrapper.orderByDesc(QmsQualityStandardDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default QmsQualityStandardDO selectByBusinessKey(Long materialId, String standardName, String version,
                                                     String applyType, Long processId, Long productModelId,
                                                     Long excludeId) {
        LambdaQueryWrapperX<QmsQualityStandardDO> wrapper = new LambdaQueryWrapperX<QmsQualityStandardDO>()
                .eq(QmsQualityStandardDO::getStandardName, standardName)
                .eq(QmsQualityStandardDO::getVersion, version)
                .eq(QmsQualityStandardDO::getApplyType, applyType)
                .neIfPresent(QmsQualityStandardDO::getId, excludeId);
        if (materialId == null) {
            wrapper.isNull(QmsQualityStandardDO::getMaterialId);
        } else {
            wrapper.eq(QmsQualityStandardDO::getMaterialId, materialId);
        }
        if (processId == null) {
            wrapper.isNull(QmsQualityStandardDO::getProcessId);
        } else {
            wrapper.eq(QmsQualityStandardDO::getProcessId, processId);
        }
        if (productModelId == null) {
            wrapper.isNull(QmsQualityStandardDO::getProductModelId);
        } else {
            wrapper.eq(QmsQualityStandardDO::getProductModelId, productModelId);
        }
        return selectOne(wrapper);
    }

    default QmsQualityStandardDO selectByGlueBoardBusinessKey(String glueBoardModel, String standardName,
                                                             String version, String applyType, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<QmsQualityStandardDO>()
                .eq(QmsQualityStandardDO::getStandardName, standardName)
                .eq(QmsQualityStandardDO::getVersion, version)
                .eq(QmsQualityStandardDO::getApplyType, applyType)
                .eq(QmsQualityStandardDO::getGlueBoardModel, glueBoardModel)
                .neIfPresent(QmsQualityStandardDO::getId, excludeId));
    }

    default QmsQualityStandardDO selectByStandardNo(String standardNo, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<QmsQualityStandardDO>()
                .eq(QmsQualityStandardDO::getStandardNo, standardNo)
                .neIfPresent(QmsQualityStandardDO::getId, excludeId));
    }

    default Long selectCountByStandardNo(String standardNo, Long excludeId) {
        return selectCount(new LambdaQueryWrapperX<QmsQualityStandardDO>()
                .eq(QmsQualityStandardDO::getStandardNo, standardNo)
                .neIfPresent(QmsQualityStandardDO::getId, excludeId));
    }

    @Select("""
            SELECT
              (SELECT COUNT(1) FROM mes_qms_iqc_order WHERE deleted = b'0' AND standard_id = #{standardId})
            + (SELECT COUNT(1) FROM mes_qms_fai_order WHERE deleted = b'0' AND standard_id = #{standardId})
            + (SELECT COUNT(1) FROM mes_qms_ipqc_order WHERE deleted = b'0' AND standard_id = #{standardId})
            + (SELECT COUNT(1) FROM mes_qms_fqc_order WHERE deleted = b'0' AND standard_id = #{standardId})
            """)
    Long selectExecutionReferenceCount(@Param("standardId") Long standardId);

    default void updateAuditInfo(Long id, Integer auditStatus, Long auditorId, String auditorName, LocalDateTime auditTime) {
        update(null, new LambdaUpdateWrapper<QmsQualityStandardDO>()
                .eq(QmsQualityStandardDO::getId, id)
                .set(QmsQualityStandardDO::getAuditStatus, auditStatus)
                .set(QmsQualityStandardDO::getAuditorId, auditorId)
                .set(QmsQualityStandardDO::getAuditorName, auditorName)
                .set(QmsQualityStandardDO::getAuditTime, auditTime));
    }

    default void updateAuditNotifyTime(Long id, LocalDateTime auditNotifyTime) {
        update(null, new LambdaUpdateWrapper<QmsQualityStandardDO>()
                .eq(QmsQualityStandardDO::getId, id)
                .set(QmsQualityStandardDO::getAuditNotifyTime, auditNotifyTime));
    }

    default void clearAuditNotifyTime(Long id) {
        updateAuditNotifyTime(id, null);
    }

    default void restoreMainFromSnapshot(QmsQualityStandardDO standard) {
        update(null, new LambdaUpdateWrapper<QmsQualityStandardDO>()
                .eq(QmsQualityStandardDO::getId, standard.getId())
                .set(QmsQualityStandardDO::getStandardNo, standard.getStandardNo())
                .set(QmsQualityStandardDO::getStandardName, standard.getStandardName())
                .set(QmsQualityStandardDO::getGlueBoardModel, standard.getGlueBoardModel())
                .set(QmsQualityStandardDO::getMaterialId, standard.getMaterialId())
                .set(QmsQualityStandardDO::getMaterialCode, standard.getMaterialCode())
                .set(QmsQualityStandardDO::getMaterialName, standard.getMaterialName())
                .set(QmsQualityStandardDO::getSpecification, standard.getSpecification())
                .set(QmsQualityStandardDO::getProductModelId, standard.getProductModelId())
                .set(QmsQualityStandardDO::getProductModelCode, standard.getProductModelCode())
                .set(QmsQualityStandardDO::getProductModelName, standard.getProductModelName())
                .set(QmsQualityStandardDO::getProdType, standard.getProdType())
                .set(QmsQualityStandardDO::getProdTypeName, standard.getProdTypeName())
                .set(QmsQualityStandardDO::getProcessId, standard.getProcessId())
                .set(QmsQualityStandardDO::getProcessCode, standard.getProcessCode())
                .set(QmsQualityStandardDO::getProcessName, standard.getProcessName())
                .set(QmsQualityStandardDO::getVersion, standard.getVersion())
                .set(QmsQualityStandardDO::getApplyType, standard.getApplyType())
                .set(QmsQualityStandardDO::getStatus, standard.getStatus())
                .set(QmsQualityStandardDO::getAuditStatus, standard.getAuditStatus())
                .set(QmsQualityStandardDO::getAuditorId, standard.getAuditorId())
                .set(QmsQualityStandardDO::getAuditorName, standard.getAuditorName())
                .set(QmsQualityStandardDO::getAuditTime, standard.getAuditTime())
                .set(QmsQualityStandardDO::getAuditNotifyTime, standard.getAuditNotifyTime())
                .set(QmsQualityStandardDO::getRemark, standard.getRemark())
                .set(QmsQualityStandardDO::getTenantId, standard.getTenantId()));
    }
}
