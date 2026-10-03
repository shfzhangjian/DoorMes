package cn.iocoder.yudao.module.mes.dal.mysql.hc.processform;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordDO;
import java.time.LocalDate;
import java.util.Collection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcProcessFormRecordMapper extends BaseMapperX<HcProcessFormRecordDO> {

    default PageResult<HcProcessFormRecordDO> selectPage(HcProcessFormRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HcProcessFormRecordDO>()
                .eqIfPresent(HcProcessFormRecordDO::getProcessCode, reqVO.getProcessCode())
                .eqIfPresent(HcProcessFormRecordDO::getTemplateCode, reqVO.getTemplateCode())
                .eqIfPresent(HcProcessFormRecordDO::getModelCode, reqVO.getModelCode())
                .eqIfPresent(HcProcessFormRecordDO::getFormType, reqVO.getFormType())
                .eqIfPresent(HcProcessFormRecordDO::getRecordStatus, reqVO.getRecordStatus())
                .eqIfPresent(HcProcessFormRecordDO::getRecordDate, reqVO.getRecordDate())
                .likeIfPresent(HcProcessFormRecordDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(HcProcessFormRecordDO::getBatchNo, reqVO.getBatchNo())
                .orderByDesc(HcProcessFormRecordDO::getRecordDate)
                .orderByDesc(HcProcessFormRecordDO::getId));
    }

    default HcProcessFormRecordDO selectOneByRecordNo(String recordNo) {
        return selectOne(new LambdaQueryWrapperX<HcProcessFormRecordDO>()
                .eq(HcProcessFormRecordDO::getRecordNo, recordNo)
                .orderByDesc(HcProcessFormRecordDO::getId)
                .last("LIMIT 1"));
    }

    default boolean existsByProcessCodeAndFormTypeAndRecordDate(String processCode,
                                                                 String formType,
                                                                 LocalDate recordDate) {
        return selectCount(new LambdaQueryWrapperX<HcProcessFormRecordDO>()
                .eq(HcProcessFormRecordDO::getProcessCode, processCode)
                .eq(HcProcessFormRecordDO::getFormType, formType)
                .eq(HcProcessFormRecordDO::getRecordDate, recordDate)
                .and(query -> query.ne(HcProcessFormRecordDO::getRecordStatus, "VOID")
                        .or()
                        .isNull(HcProcessFormRecordDO::getRecordStatus))) > 0;
    }

    default boolean existsConfirmedOkByProcessCodeAndFormTypeAndRecordDate(String processCode,
                                                                             String formType,
                                                                             LocalDate recordDate) {
        return selectCount(new LambdaQueryWrapperX<HcProcessFormRecordDO>()
                .eq(HcProcessFormRecordDO::getProcessCode, processCode)
                .eq(HcProcessFormRecordDO::getFormType, formType)
                .eq(HcProcessFormRecordDO::getRecordDate, recordDate)
                .eq(HcProcessFormRecordDO::getRecordStatus, "CONFIRMED")
                .eq(HcProcessFormRecordDO::getResultStatus, "OK")) > 0;
    }

    default boolean existsConfirmedByTemplateCodeAndRecordDate(String templateCode, LocalDate recordDate) {
        return selectCount(new LambdaQueryWrapperX<HcProcessFormRecordDO>()
                .eq(HcProcessFormRecordDO::getTemplateCode, templateCode)
                .eq(HcProcessFormRecordDO::getRecordDate, recordDate)
                .eq(HcProcessFormRecordDO::getRecordStatus, "CONFIRMED")) > 0;
    }

    default boolean existsConfirmedByProcessCodeAndRecordDateExcludingTemplateCode(String processCode,
                                                                                    LocalDate recordDate,
                                                                                    String excludedTemplateCode) {
        LambdaQueryWrapperX<HcProcessFormRecordDO> query = new LambdaQueryWrapperX<HcProcessFormRecordDO>()
                .eq(HcProcessFormRecordDO::getProcessCode, processCode)
                .eq(HcProcessFormRecordDO::getRecordDate, recordDate)
                .eq(HcProcessFormRecordDO::getRecordStatus, "CONFIRMED");
        if (excludedTemplateCode != null && !excludedTemplateCode.isBlank()) {
            query.ne(HcProcessFormRecordDO::getTemplateCode, excludedTemplateCode);
        }
        return selectCount(query) > 0;
    }

    default boolean existsConfirmedByProcessCodeAndRecordDateExcludingTemplateCodes(String processCode,
                                                                                     LocalDate recordDate,
                                                                                     Collection<String> excludedTemplateCodes) {
        LambdaQueryWrapperX<HcProcessFormRecordDO> query = new LambdaQueryWrapperX<HcProcessFormRecordDO>()
                .eq(HcProcessFormRecordDO::getProcessCode, processCode)
                .eq(HcProcessFormRecordDO::getRecordDate, recordDate)
                .eq(HcProcessFormRecordDO::getRecordStatus, "CONFIRMED");
        if (excludedTemplateCodes != null && !excludedTemplateCodes.isEmpty()) {
            query.notIn(HcProcessFormRecordDO::getTemplateCode, excludedTemplateCodes);
        }
        return selectCount(query) > 0;
    }
}
