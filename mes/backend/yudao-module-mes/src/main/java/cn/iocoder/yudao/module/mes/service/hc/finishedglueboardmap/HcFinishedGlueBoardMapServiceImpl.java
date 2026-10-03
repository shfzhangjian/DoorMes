package cn.iocoder.yudao.module.mes.service.hc.finishedglueboardmap;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo.HcFinishedGlueBoardMapItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo.HcFinishedGlueBoardMapPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo.HcFinishedGlueBoardMapSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap.HcFinishedGlueBoardMapDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap.HcFinishedGlueBoardMapItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.finishedglueboardmap.HcFinishedGlueBoardMapItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.finishedglueboardmap.HcFinishedGlueBoardMapMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.material.HcMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFINISHEDGLUEBOARDMAP_ITEMS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFINISHEDGLUEBOARDMAP_MODEL_SPEC_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFINISHEDGLUEBOARDMAP_NOT_EXISTS;

@Service
@Validated
public class HcFinishedGlueBoardMapServiceImpl implements HcFinishedGlueBoardMapService {

    private static final String PROCESS_ADHESIVE1 = "ADHESIVE1";
    private static final String PROCESS_ADHESIVE2 = "ADHESIVE2";

    @Resource
    private HcFinishedGlueBoardMapMapper finishedGlueBoardMapMapper;
    @Resource
    private HcFinishedGlueBoardMapItemMapper finishedGlueBoardMapItemMapper;
    @Resource
    private HcProductModelMapper productModelMapper;
    @Resource
    private HcMaterialMapper materialMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcFinishedGlueBoardMap(HcFinishedGlueBoardMapSaveReqVO createReqVO) {
        validateModelSpecUnique(null, createReqVO.getProductModelCode(), createReqVO.getProductSpec());
        HcFinishedGlueBoardMapDO entity = BeanUtils.toBean(createReqVO, HcFinishedGlueBoardMapDO.class);
        entity.setStatus(StrUtil.blankToDefault(entity.getStatus(), "ENABLE"));
        fillProductSnapshot(entity);
        List<HcFinishedGlueBoardMapItemDO> itemRows = buildItemRows(entity, createReqVO.getItems());
        if (CollUtil.isEmpty(itemRows)) {
            throw exception(HCFINISHEDGLUEBOARDMAP_ITEMS_EMPTY);
        }
        fillSummaries(entity, itemRows);
        finishedGlueBoardMapMapper.insert(entity);
        itemRows.forEach(item -> item.setMapId(entity.getId()));
        finishedGlueBoardMapItemMapper.insertBatch(itemRows);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcFinishedGlueBoardMap(HcFinishedGlueBoardMapSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateModelSpecUnique(updateReqVO.getId(), updateReqVO.getProductModelCode(), updateReqVO.getProductSpec());
        HcFinishedGlueBoardMapDO updateObj = BeanUtils.toBean(updateReqVO, HcFinishedGlueBoardMapDO.class);
        updateObj.setStatus(StrUtil.blankToDefault(updateObj.getStatus(), "ENABLE"));
        fillProductSnapshot(updateObj);
        List<HcFinishedGlueBoardMapItemDO> itemRows = buildItemRows(updateObj, updateReqVO.getItems());
        if (CollUtil.isEmpty(itemRows)) {
            throw exception(HCFINISHEDGLUEBOARDMAP_ITEMS_EMPTY);
        }
        fillSummaries(updateObj, itemRows);
        finishedGlueBoardMapMapper.updateById(updateObj);
        finishedGlueBoardMapItemMapper.deleteByMapId(updateReqVO.getId());
        itemRows.forEach(item -> item.setMapId(updateReqVO.getId()));
        finishedGlueBoardMapItemMapper.insertBatch(itemRows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcFinishedGlueBoardMap(Long id) {
        validateExists(id);
        finishedGlueBoardMapMapper.deleteById(id);
        finishedGlueBoardMapItemMapper.deleteByMapId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcFinishedGlueBoardMapListByIds(List<Long> ids) {
        for (Long id : ids) {
            deleteHcFinishedGlueBoardMap(id);
        }
    }

    @Override
    public HcFinishedGlueBoardMapDO getHcFinishedGlueBoardMap(Long id) {
        return finishedGlueBoardMapMapper.selectById(id);
    }

    @Override
    public PageResult<HcFinishedGlueBoardMapDO> getHcFinishedGlueBoardMapPage(HcFinishedGlueBoardMapPageReqVO pageReqVO) {
        return finishedGlueBoardMapMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcFinishedGlueBoardMapDO> getHcFinishedGlueBoardMapList(HcFinishedGlueBoardMapPageReqVO reqVO) {
        return finishedGlueBoardMapMapper.selectList(reqVO);
    }

    @Override
    public List<HcFinishedGlueBoardMapItemDO> getItemListByMapId(Long mapId) {
        return finishedGlueBoardMapItemMapper.selectListByMapId(mapId);
    }

    @Override
    public List<HcFinishedGlueBoardMapItemDO> getMatchedItems(String productModelCode, String glueProcess) {
        String modelCode = StrUtil.trim(productModelCode);
        String process = normalizeGlueProcess(glueProcess);
        if (StrUtil.isBlank(modelCode) || StrUtil.isBlank(process)) {
            return List.of();
        }
        List<HcFinishedGlueBoardMapDO> maps = finishedGlueBoardMapMapper.selectEnabledListByProductModelCode(modelCode);
        if (CollUtil.isEmpty(maps)) {
            return List.of();
        }
        Map<String, HcFinishedGlueBoardMapItemDO> dedup = new LinkedHashMap<>();
        for (HcFinishedGlueBoardMapDO map : maps) {
            for (HcFinishedGlueBoardMapItemDO item : finishedGlueBoardMapItemMapper.selectListByMapId(map.getId())) {
                if (!process.equalsIgnoreCase(StrUtil.trim(item.getGlueProcess()))) {
                    continue;
                }
                String key = StrUtil.trimToEmpty(item.getGlueBoardMaterialCode()).toUpperCase()
                        + "|" + StrUtil.trimToEmpty(item.getGlueBoardModel()).toUpperCase();
                dedup.putIfAbsent(key, item);
            }
        }
        return dedup.values().stream()
                .sorted(Comparator
                        .comparing((HcFinishedGlueBoardMapItemDO item) -> !Boolean.TRUE.equals(item.getPreferredFlag()))
                        .thenComparing(item -> item.getSort() == null ? 0 : item.getSort())
                        .thenComparing(item -> Objects.toString(item.getGlueBoardMaterialCode(), ""))
                        .thenComparing(item -> Objects.toString(item.getGlueBoardModel(), "")))
                .toList();
    }

    @Override
    public List<HcFinishedGlueBoardMapItemDO> getGlueBoardModelItems(String glueProcess, String glueBoardMaterialCode) {
        String process = normalizeGlueProcess(glueProcess);
        String materialCode = StrUtil.trim(glueBoardMaterialCode);
        HcFinishedGlueBoardMapPageReqVO reqVO = new HcFinishedGlueBoardMapPageReqVO();
        reqVO.setStatus("ENABLE");
        List<HcFinishedGlueBoardMapDO> maps = finishedGlueBoardMapMapper.selectList(reqVO);
        if (CollUtil.isEmpty(maps)) {
            return List.of();
        }
        List<HcFinishedGlueBoardMapItemDO> candidates = new ArrayList<>();
        for (HcFinishedGlueBoardMapDO map : maps) {
            for (HcFinishedGlueBoardMapItemDO item : finishedGlueBoardMapItemMapper.selectListByMapId(map.getId())) {
                if (StrUtil.isBlank(item.getGlueBoardModel())) {
                    continue;
                }
                if (StrUtil.isNotBlank(process) && !process.equalsIgnoreCase(StrUtil.trim(item.getGlueProcess()))) {
                    continue;
                }
                if (StrUtil.isNotBlank(materialCode) && !materialCode.equalsIgnoreCase(StrUtil.trim(item.getGlueBoardMaterialCode()))) {
                    continue;
                }
                candidates.add(item);
            }
        }
        Map<String, HcFinishedGlueBoardMapItemDO> dedup = new LinkedHashMap<>();
        candidates.stream()
                .sorted(Comparator
                        .comparing((HcFinishedGlueBoardMapItemDO item) -> !Boolean.TRUE.equals(item.getPreferredFlag()))
                        .thenComparing(item -> item.getSort() == null ? 0 : item.getSort())
                        .thenComparing(item -> Objects.toString(item.getGlueBoardModel(), ""))
                        .thenComparing(item -> Objects.toString(item.getGlueBoardMaterialCode(), "")))
                .forEach(item -> dedup.putIfAbsent(
                        StrUtil.trimToEmpty(item.getGlueBoardModel()).toUpperCase()
                                + "|" + StrUtil.trimToEmpty(item.getGlueBoardMaterialCode()).toUpperCase(),
                        item));
        return new ArrayList<>(dedup.values());
    }

    private HcFinishedGlueBoardMapDO validateExists(Long id) {
        HcFinishedGlueBoardMapDO entity = finishedGlueBoardMapMapper.selectById(id);
        if (entity == null) {
            throw exception(HCFINISHEDGLUEBOARDMAP_NOT_EXISTS);
        }
        return entity;
    }

    private void validateModelSpecUnique(Long id, String modelCode, String productSpec) {
        HcFinishedGlueBoardMapDO entity = finishedGlueBoardMapMapper.selectOne(new LambdaQueryWrapperX<HcFinishedGlueBoardMapDO>()
                .eq(HcFinishedGlueBoardMapDO::getProductModelCode, modelCode)
                .eq(HcFinishedGlueBoardMapDO::getProductSpec, productSpec)
                .neIfPresent(HcFinishedGlueBoardMapDO::getId, id));
        if (entity != null) {
            throw exception(HCFINISHEDGLUEBOARDMAP_MODEL_SPEC_EXISTS);
        }
    }

    private void fillProductSnapshot(HcFinishedGlueBoardMapDO entity) {
        HcProductModelDO productModel = null;
        if (entity.getProductModelId() != null) {
            productModel = productModelMapper.selectById(entity.getProductModelId());
        }
        if (productModel == null && StrUtil.isNotBlank(entity.getProductModelCode())) {
            productModel = productModelMapper.selectByModelCode(entity.getProductModelCode());
        }
        if (productModel == null) {
            entity.setProductModelName(StrUtil.blankToDefault(entity.getProductModelName(), entity.getProductModelCode()));
            entity.setSizeSpec(StrUtil.blankToDefault(entity.getSizeSpec(), entity.getProductSpec()));
            entity.setSizeName(StrUtil.blankToDefault(entity.getSizeName(), entity.getProductSpec()));
            return;
        }
        entity.setProductModelId(productModel.getId());
        entity.setProductModelCode(productModel.getModelCode());
        entity.setProductModelName(StrUtil.blankToDefault(productModel.getModelName(), productModel.getModelCode()));
        entity.setSizeSpec(StrUtil.blankToDefault(productModel.getSizeSpec(), entity.getProductSpec()));
        entity.setSizeName(StrUtil.blankToDefault(productModel.getSizeName(), entity.getProductSpec()));
    }

    private List<HcFinishedGlueBoardMapItemDO> buildItemRows(HcFinishedGlueBoardMapDO parent, List<HcFinishedGlueBoardMapItemReqVO> reqItems) {
        if (CollUtil.isEmpty(reqItems)) {
            return List.of();
        }
        List<HcFinishedGlueBoardMapItemDO> rows = new ArrayList<>();
        int sort = 1;
        for (HcFinishedGlueBoardMapItemReqVO reqItem : reqItems) {
            if (reqItem == null || isBlankItem(reqItem)) {
                continue;
            }
            HcFinishedGlueBoardMapItemDO row = BeanUtils.toBean(reqItem, HcFinishedGlueBoardMapItemDO.class);
            row.setId(null);
            row.setProductModelCode(parent.getProductModelCode());
            row.setGlueProcess(StrUtil.blankToDefault(row.getGlueProcess(), PROCESS_ADHESIVE1));
            row.setGlueProcessName(resolveProcessName(row.getGlueProcess()));
            row.setPreferredFlag(Boolean.TRUE.equals(row.getPreferredFlag()));
            row.setSort(row.getSort() == null ? sort : row.getSort());
            fillMaterialSnapshot(row);
            rows.add(row);
            sort++;
        }
        return rows;
    }

    private boolean isBlankItem(HcFinishedGlueBoardMapItemReqVO reqItem) {
        return StrUtil.isBlank(reqItem.getGlueProcess())
                && reqItem.getGlueBoardMaterialId() == null
                && StrUtil.isBlank(reqItem.getGlueBoardMaterialCode())
                && StrUtil.isBlank(reqItem.getGlueBoardModel());
    }

    private void fillMaterialSnapshot(HcFinishedGlueBoardMapItemDO row) {
        HcMaterialDO material = null;
        if (row.getGlueBoardMaterialId() != null) {
            material = materialMapper.selectById(row.getGlueBoardMaterialId());
        }
        if (material == null && StrUtil.isNotBlank(row.getGlueBoardMaterialCode())) {
            material = materialMapper.selectOne(new LambdaQueryWrapperX<HcMaterialDO>()
                    .eq(HcMaterialDO::getMaterialCode, row.getGlueBoardMaterialCode()));
        }
        if (material == null) {
            return;
        }
        row.setGlueBoardMaterialId(material.getId());
        row.setGlueBoardMaterialCode(material.getMaterialCode());
        row.setGlueBoardMaterialName(material.getMaterialName());
        row.setGlueBoardSpec(material.getSpecModel());
        row.setGlueBoardModel(StrUtil.blankToDefault(row.getGlueBoardModel(), material.getSpecModel()));
    }

    private void fillSummaries(HcFinishedGlueBoardMapDO entity, List<HcFinishedGlueBoardMapItemDO> items) {
        entity.setAdhesive1Summary(buildSummary(items, PROCESS_ADHESIVE1));
        entity.setAdhesive2Summary(buildSummary(items, PROCESS_ADHESIVE2));
    }

    private String buildSummary(List<HcFinishedGlueBoardMapItemDO> items, String process) {
        return items.stream()
                .filter(item -> process.equals(item.getGlueProcess()))
                .sorted(Comparator.comparing(item -> item.getSort() == null ? 0 : item.getSort()))
                .map(this::formatSummaryItem)
                .filter(StrUtil::isNotBlank)
                .reduce((left, right) -> left + "、" + right)
                .orElse("");
    }

    private String formatSummaryItem(HcFinishedGlueBoardMapItemDO item) {
        String code = StrUtil.blankToDefault(item.getGlueBoardMaterialCode(), "");
        String model = StrUtil.blankToDefault(item.getGlueBoardModel(), "");
        if (StrUtil.isNotBlank(code) && StrUtil.isNotBlank(model)) {
            return code + " / " + model;
        }
        return StrUtil.blankToDefault(code, model);
    }

    private String resolveProcessName(String process) {
        if (PROCESS_ADHESIVE2.equals(process)) {
            return "粘胶2";
        }
        return "粘胶1";
    }

    private String normalizeGlueProcess(String glueProcess) {
        String process = StrUtil.trimToEmpty(glueProcess).toUpperCase();
        if ("ADHESIVE2".equals(process) || "OP-ADHESIVE2".equals(process)) {
            return PROCESS_ADHESIVE2;
        }
        if ("ADHESIVE1".equals(process) || "ADHESIVE".equals(process) || "OP-ADHESIVE1".equals(process)) {
            return PROCESS_ADHESIVE1;
        }
        return process;
    }

}
