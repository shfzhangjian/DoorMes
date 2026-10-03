package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcProcessReportDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionService;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionServiceImpl;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HcFormulaProductionRecordServiceImpl implements HcFormulaProductionRecordService {

    private static final String REPORT_TYPE_END = "END";
    private static final String SOURCE_MENU_CODE_FORMULA_REPORT = "FORMULA_REPORT";

    @Resource
    private HcProcessReportMapper hcProcessReportMapper;
    @Resource
    private HcProductionRecordRevisionService productionRecordRevisionService;
    @Resource
    private HcProductionRecordPadTypeResolver padTypeResolver;

    @Override
    public PageResult<HcFormulaProductionRecordRespVO> getPage(HcFormulaProductionRecordPageReqVO reqVO) {
        List<HcFormulaProductionRecordRespVO> rows = getList(reqVO);
        int pageSize = reqVO.getPageSize() == null ? 20 : reqVO.getPageSize();
        if (pageSize <= 0) {
            return new PageResult<>(rows, (long) rows.size());
        }
        int pageNo = reqVO.getPageNo() == null ? 1 : Math.max(reqVO.getPageNo(), 1);
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    @Override
    public List<HcFormulaProductionRecordRespVO> getList(HcFormulaProductionRecordPageReqVO reqVO) {
        List<HcFormulaProductionRecordRespVO> rows = hcProcessReportMapper.selectList(buildQuery(reqVO)).stream()
                .map(this::toRespVO)
                .toList();
        fillPadTypes(rows);
        List<HcFormulaProductionRecordRespVO> revisedRows = productionRecordRevisionService.applyRevisions(
                HcProductionRecordRevisionServiceImpl.MODULE_FORMULA, rows);
        return revisedRows.stream()
                .filter(row -> padTypeResolver.matchesFilter(reqVO.getPadType(), row.getPadType()))
                .toList();
    }

    private LambdaQueryWrapperX<HcProcessReportDO> buildQuery(HcFormulaProductionRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcProcessReportDO> wrapper = new LambdaQueryWrapperX<HcProcessReportDO>()
                .eq(HcProcessReportDO::getSourceMenuCode, SOURCE_MENU_CODE_FORMULA_REPORT)
                .eq(HcProcessReportDO::getReportType, REPORT_TYPE_END)
                .eq(HcProcessReportDO::getDeleted, false)
                .geIfPresent(HcProcessReportDO::getReportDate, reqVO.getReportDateStart())
                .leIfPresent(HcProcessReportDO::getReportDate, reqVO.getReportDateEnd())
                .likeIfPresent(HcProcessReportDO::getFilterBatchNo, trimToNull(reqVO.getFilterBatchNo()))
                .likeIfPresent(HcProcessReportDO::getMixerEquipmentCode, trimToNull(reqVO.getMixerEquipmentCode()))
                .likeIfPresent(HcProcessReportDO::getBatchingTankNo, trimToNull(reqVO.getBatchingTankNo()))
                .likeIfPresent(HcProcessReportDO::getFoamingEquipmentCode, trimToNull(reqVO.getFoamingEquipmentCode()))
                .likeIfPresent(HcProcessReportDO::getDefoamingTankNo, trimToNull(reqVO.getDefoamingTankNo()))
                .likeIfPresent(HcProcessReportDO::getRecorderName, trimToNull(reqVO.getRecorderName()))
                .orderByDesc(HcProcessReportDO::getEndTime)
                .orderByDesc(HcProcessReportDO::getReportDate)
                .orderByDesc(HcProcessReportDO::getId);
        String modelCode = trimToNull(reqVO.getModelCode());
        if (modelCode != null) {
            wrapper.and(item -> item.like(HcProcessReportDO::getMotherModelCode, modelCode)
                    .or().like(HcProcessReportDO::getMotherModelName, modelCode));
        }
        String materialCode = trimToNull(reqVO.getMaterialCode());
        if (materialCode != null) {
            wrapper.and(item -> item.like(HcProcessReportDO::getMotherMaterialCode, materialCode)
                    .or().like(HcProcessReportDO::getMaterialCode, materialCode)
                    .or().like(HcProcessReportDO::getMotherMaterialName, materialCode)
                    .or().like(HcProcessReportDO::getMaterialName, materialCode));
        }
        String batchNo = trimToNull(reqVO.getBatchNo());
        if (batchNo != null) {
            wrapper.and(item -> item.like(HcProcessReportDO::getProductionBatchNo, batchNo)
                    .or().like(HcProcessReportDO::getBatchNo, batchNo)
                    .or().like(HcProcessReportDO::getParentProductionBatchNo, batchNo));
        }
        return wrapper;
    }

    private HcFormulaProductionRecordRespVO toRespVO(HcProcessReportDO report) {
        HcFormulaProductionRecordRespVO respVO = new HcFormulaProductionRecordRespVO();
        respVO.setId(report.getId());
        respVO.setReportDate(report.getReportDate());
        respVO.setModelCode(firstNotBlank(report.getMotherModelCode(), report.getMotherModelName()));
        respVO.setMaterialCode(firstNotBlank(report.getMotherMaterialCode(), report.getMaterialCode(),
                report.getMotherMaterialName(), report.getMaterialName()));
        respVO.setBatchNo(firstNotBlank(report.getProductionBatchNo(), report.getBatchNo(),
                report.getParentProductionBatchNo(), report.getParentBatchNo()));
        respVO.setFilterBatchNo(report.getFilterBatchNo());
        respVO.setInputWeight(report.getInputWeight());
        respVO.setOutputWeight(report.getGoodQty());
        respVO.setMixerEquipmentCode(report.getMixerEquipmentCode());
        respVO.setBatchingTankNo(report.getBatchingTankNo());
        respVO.setFoamingEquipmentCode(report.getFoamingEquipmentCode());
        respVO.setDefoamingTankNo(report.getDefoamingTankNo());
        respVO.setRecorderName(report.getRecorderName());
        respVO.setRecordTime(report.getEndTime());
        respVO.setRemark(report.getRemark());
        return respVO;
    }

    private void fillPadTypes(List<HcFormulaProductionRecordRespVO> rows) {
        Map<String, String> padTypes = padTypeResolver.resolveByModelCodes(
                rows.stream().map(HcFormulaProductionRecordRespVO::getModelCode).toList());
        rows.forEach(row -> row.setPadType(padTypes.get(row.getModelCode())));
    }

    private static String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

}
