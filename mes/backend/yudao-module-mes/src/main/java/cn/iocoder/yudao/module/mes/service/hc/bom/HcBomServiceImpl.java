package cn.iocoder.yudao.module.mes.service.hc.bom;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomProductImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.bom.HcBomItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.bom.HcBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.material.HcMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.recipe.HcRecipeMapper;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCBOM_BOMCODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCBOM_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMATERIAL_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPRODUCTMODEL_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCRECIPE_NOT_EXISTS;

@Service
@Validated
public class HcBomServiceImpl implements HcBomService {

    private static final int PRODUCT_BOM_DATA_START_ROW_INDEX = 2;
    private static final String PRODUCT_BOM_CODE_PREFIX = "PBOM-";
    private static final String MASS_PRODUCTION_BOM_TYPE = "量产";
    private static final String TRIAL_PRODUCTION_BOM_TYPE = "研发样";
    private static final String PRODUCT_MODEL_STATUS_ENABLE = "ENABLE";
    private static final String DEFAULT_VERSION_NO = "V1";
    private static final String DEFAULT_SUPPLY_MODE = "工序投料";
    private static final BigDecimal DEFAULT_QTY = BigDecimal.ONE;
    private static final BigDecimal ZERO_RATE = BigDecimal.ZERO;
    private static final int DEFAULT_PRESS_SLOT_CONTINUOUS_CHECK_COUNT = 20;
    private static final String COMPONENT_TYPE_INTERMEDIATE = "中间品";
    private static final String COMPONENT_TYPE_AUXILIARY = "辅料";
    private static final String OPERATION_ROUGH_GRINDING_CODE = "WC-GRIND";
    private static final String OPERATION_ROUGH_GRINDING_NAME = "磨皮";
    private static final String OPERATION_ADHESIVE1_CODE = "WC-ADH1";
    private static final String OPERATION_ADHESIVE1_NAME = "粘胶1";
    private static final String OPERATION_ADHESIVE2_CODE = "WC-ADH2";
    private static final String OPERATION_ADHESIVE2_NAME = "粘胶2";

    @Resource
    private HcBomMapper hcBomMapper;

    @Resource
    private HcBomItemMapper hcBomItemMapper;

    @Resource
    private HcMaterialMapper hcMaterialMapper;

    @Resource
    private HcProductModelMapper hcProductModelMapper;

    @Resource
    private HcRecipeMapper hcRecipeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcBom(HcBomSaveReqVO createReqVO) {
        validateBomCodeUnique(null, createReqVO.getBomCode());
        fillProductMaterialSnapshot(createReqVO);
        fillProductModelSnapshot(createReqVO);
        fillRecipeSnapshot(createReqVO);
        fillPressSlotContinuousCheckCount(createReqVO);
        HcBomDO entity = BeanUtils.toBean(createReqVO, HcBomDO.class);
        hcBomMapper.insert(entity);
        createHcBomItemList(entity.getId(), createReqVO.getBomItems());
        markProductModelReferenced(entity.getProductModelId());
        updateMaterialDefaultBom(entity.getProductMaterialId(), entity.getId(), entity.getBomCode(), entity.getBomName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcBom(HcBomSaveReqVO updateReqVO) {
        validateHcBomExists(updateReqVO.getId());
        validateBomCodeUnique(updateReqVO.getId(), updateReqVO.getBomCode());
        fillProductMaterialSnapshot(updateReqVO);
        fillProductModelSnapshot(updateReqVO);
        fillRecipeSnapshot(updateReqVO);
        fillPressSlotContinuousCheckCount(updateReqVO);
        HcBomDO updateObj = BeanUtils.toBean(updateReqVO, HcBomDO.class);
        hcBomMapper.updateById(updateObj);
        updateHcBomItemList(updateReqVO.getId(), updateReqVO.getBomItems());
        markProductModelReferenced(updateObj.getProductModelId());
        updateMaterialDefaultBom(updateReqVO.getProductMaterialId(), updateReqVO.getId(), updateReqVO.getBomCode(), updateReqVO.getBomName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcBom(Long id) {
        validateHcBomExists(id);
        hcBomMapper.deleteById(id);
        hcBomItemMapper.deleteByParentId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcBomListByIds(List<Long> ids) {
        hcBomMapper.deleteByIds(ids);
        hcBomItemMapper.deleteByParentIds(ids);
    }

    private void validateHcBomExists(Long id) {
        if (hcBomMapper.selectById(id) == null) {
            throw exception(HCBOM_NOT_EXISTS);
        }
    }

    private void validateBomCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcBomDO entity = hcBomMapper.selectOne(new LambdaQueryWrapperX<HcBomDO>().eq(HcBomDO::getBomCode, value).neIfPresent(HcBomDO::getId, id));
        if (entity != null) {
            throw exception(HCBOM_BOMCODE_EXISTS);
        }
    }

    @Override
    public HcBomDO getHcBom(Long id) {
        return hcBomMapper.selectById(id);
    }

    @Override
    public List<HcBomDO> getHcBomSimpleList() {
        LambdaQueryWrapperX<HcBomDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcBomDO::getStatus, 1);
        queryWrapper.orderByAsc(HcBomDO::getBomCode);
        queryWrapper.orderByDesc(HcBomDO::getId);
        return hcBomMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcBomDO> getHcBomSimpleListByMaterialId(Long materialId) {
        LambdaQueryWrapperX<HcBomDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcBomDO::getStatus, 1);
        queryWrapper.eqIfPresent(HcBomDO::getProductMaterialId, materialId);
        queryWrapper.orderByAsc(HcBomDO::getBomCode);
        queryWrapper.orderByDesc(HcBomDO::getId);
        return hcBomMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcBomDO> getHcBomProductModelOptionList(String keyword, Long productMaterialId) {
        LambdaQueryWrapperX<HcBomDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcBomDO::getStatus, 1);
        queryWrapper.eqIfPresent(HcBomDO::getProductMaterialId, productMaterialId);
        queryWrapper.isNotNull(HcBomDO::getProductModelId);
        queryWrapper.isNotNull(HcBomDO::getProductModelCode);
        if (StringUtils.hasText(keyword)) {
            String trimKeyword = keyword.trim();
            queryWrapper.and(wrapper -> wrapper.like(HcBomDO::getProductModelCode, trimKeyword)
                    .or()
                    .like(HcBomDO::getProductModelName, trimKeyword));
        }
        queryWrapper.orderByAsc(HcBomDO::getProductModelCode);
        queryWrapper.orderByDesc(HcBomDO::getId);
        return hcBomMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcBomDO> getHcBomList(HcBomPageReqVO reqVO) {
        return hcBomMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcBomDO> getHcBomPage(HcBomPageReqVO pageReqVO) {
        return hcBomMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcBomItemDO> getHcBomItemListByParentId(Long parentId) {
        return hcBomItemMapper.selectListByParentId(parentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcBomProductImportRespVO importProductBom(MultipartFile file, Boolean overwrite) throws IOException {
        HcBomProductImportRespVO respVO = new HcBomProductImportRespVO();
        boolean overwriteExisting = overwrite == null || overwrite;
        List<ProductBomImportRow> rows = parseProductBomRows(file, respVO);
        validateProductBomRows(rows, overwriteExisting, respVO);
        if (!respVO.getFailures().isEmpty()) {
            respVO.setFailureCount(respVO.getFailures().size());
            respVO.getMessages().add("导入校验未通过，未写入任何BOM数据");
            return respVO;
        }

        int itemCount = 0;
        for (ProductBomImportRow row : rows) {
            List<HcBomItemDO> bomItems = buildImportedBomItems(row);
            upsertImportedProductBom(row, bomItems);
            itemCount += bomItems.size();
        }
        respVO.setSuccessCount(rows.size());
        respVO.setItemCount(itemCount);
        respVO.setFailureCount(0);
        respVO.getMessages().add(String.format("导入完成：产品BOM %d 条，明细 %d 条", rows.size(), itemCount));
        return respVO;
    }

    private List<ProductBomImportRow> parseProductBomRows(MultipartFile file, HcBomProductImportRespVO respVO) throws IOException {
        List<ProductBomImportRow> rows = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return rows;
        }

        DataFormatter formatter = new DataFormatter();
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                addImportFailure(respVO, "Excel未包含任何Sheet");
                return rows;
            }
            for (int rowIndex = PRODUCT_BOM_DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row excelRow = sheet.getRow(rowIndex);
                if (excelRow == null) {
                    continue;
                }
                respVO.setTotalRows(respVO.getTotalRows() + 1);
                String productMaterialCode = normalizeMaterialCode(readCell(excelRow, 2, formatter));
                if (!StringUtils.hasText(productMaterialCode)) {
                    respVO.setSkippedRows(respVO.getSkippedRows() + 1);
                    continue;
                }
                ProductBomImportRow row = new ProductBomImportRow();
                row.rowNo = rowIndex + 1;
                row.productModelCode = normalizeText(readCell(excelRow, 0, formatter));
                row.productSpec = normalizeText(readCell(excelRow, 1, formatter));
                row.productMaterialCode = productMaterialCode;
                row.roughGrindingMaterialCode = normalizeMaterialCode(readCell(excelRow, 3, formatter));
                row.adhesive1IntermediateMaterialCode = normalizeMaterialCode(readCell(excelRow, 4, formatter));
                row.adhesive1AuxMaterialCode = normalizeMaterialCode(readCell(excelRow, 5, formatter));
                row.adhesive2AuxMaterialCode = normalizeMaterialCode(readCell(excelRow, 6, formatter));
                rows.add(row);
            }
        }
        return rows;
    }

    private void validateProductBomRows(List<ProductBomImportRow> rows, boolean overwriteExisting, HcBomProductImportRespVO respVO) {
        Set<String> productCodes = new LinkedHashSet<>();
        for (ProductBomImportRow row : rows) {
            if (!StringUtils.hasText(row.productModelCode)) {
                addImportFailure(respVO, String.format("第%d行：A列制成品型号为空", row.rowNo));
            }
            if (!StringUtils.hasText(row.productSpec)) {
                addImportFailure(respVO, String.format("第%d行：B列成品规格/尺寸为空", row.rowNo));
            }
            if (!productCodes.add(row.productMaterialCode)) {
                addImportFailure(respVO, String.format("第%d行：制成品料号%s在Excel中重复", row.rowNo, row.productMaterialCode));
            }

            row.productMaterial = selectMaterialByCode(row.productMaterialCode);
            if (row.productMaterial == null) {
                addImportFailure(respVO, String.format("第%d行：C列制成品料号%s不存在于物料主数据", row.rowNo, row.productMaterialCode));
            }
            row.productModel = selectProductModelByCode(row.productModelCode);
            if (row.productModel == null) {
                addImportFailure(respVO, String.format("第%d行：A列制成品型号%s不存在于产品型号字典或未启用", row.rowNo, row.productModelCode));
            }
            validateComponentMaterial(row, row.roughGrindingMaterialCode, "D列磨皮工序中间品", respVO);
            validateComponentMaterial(row, row.adhesive1IntermediateMaterialCode, "E列粘胶1中间品", respVO);
            validateComponentMaterial(row, row.adhesive1AuxMaterialCode, "F列粘胶1胶板辅料", respVO);
            validateComponentMaterial(row, row.adhesive2AuxMaterialCode, "G列粘胶2胶板辅料", respVO);

            HcBomDO existingBom = selectBomByCode(buildProductBomCode(row.productMaterialCode));
            if (existingBom != null && !overwriteExisting) {
                addImportFailure(respVO, String.format("第%d行：产品BOM %s 已存在", row.rowNo, existingBom.getBomCode()));
            }
        }
    }

    private void validateComponentMaterial(ProductBomImportRow row, String materialCode, String columnName,
                                           HcBomProductImportRespVO respVO) {
        if (!StringUtils.hasText(materialCode)) {
            return;
        }
        HcMaterialDO material = selectMaterialByCode(materialCode);
        if (material == null) {
            addImportFailure(respVO, String.format("第%d行：%s料号%s不存在于物料主数据", row.rowNo, columnName, materialCode));
            return;
        }
        if (Objects.equals(materialCode, row.roughGrindingMaterialCode)) {
            row.roughGrindingMaterial = material;
        }
        if (Objects.equals(materialCode, row.adhesive1IntermediateMaterialCode)) {
            row.adhesive1IntermediateMaterial = material;
        }
        if (Objects.equals(materialCode, row.adhesive1AuxMaterialCode)) {
            row.adhesive1AuxMaterial = material;
        }
        if (Objects.equals(materialCode, row.adhesive2AuxMaterialCode)) {
            row.adhesive2AuxMaterial = material;
        }
    }

    private List<HcBomItemDO> buildImportedBomItems(ProductBomImportRow row) {
        List<HcBomItemDO> items = new ArrayList<>();
        addImportedBomItem(items, row.roughGrindingMaterial, COMPONENT_TYPE_INTERMEDIATE, OPERATION_ROUGH_GRINDING_CODE,
                OPERATION_ROUGH_GRINDING_NAME, "ROUGH_INTERMEDIATE", "来源列D：磨皮工序中间品");
        addImportedBomItem(items, row.adhesive1IntermediateMaterial, COMPONENT_TYPE_INTERMEDIATE, OPERATION_ADHESIVE1_CODE,
                OPERATION_ADHESIVE1_NAME, "ADH1_INTERMEDIATE", "来源列E：粘胶1中间品");
        addImportedBomItem(items, row.adhesive1AuxMaterial, COMPONENT_TYPE_AUXILIARY, OPERATION_ADHESIVE1_CODE,
                OPERATION_ADHESIVE1_NAME, "ADH1_AUX_GLUE_BOARD", "来源列F：粘胶1胶板辅料");
        addImportedBomItem(items, row.adhesive2AuxMaterial, COMPONENT_TYPE_AUXILIARY, OPERATION_ADHESIVE2_CODE,
                OPERATION_ADHESIVE2_NAME, "ADH2_AUX_GLUE_BOARD", "来源列G：粘胶2胶板辅料");
        String bomCode = buildProductBomCode(row.productMaterialCode);
        for (int index = 0; index < items.size(); index++) {
            HcBomItemDO item = items.get(index);
            item.setLineNo(index + 1);
            item.setBomCode(bomCode);
        }
        return items;
    }

    private void addImportedBomItem(List<HcBomItemDO> items, HcMaterialDO material, String componentType,
                                    String operationCode, String operationName, String consumeGroupCode, String remark) {
        if (material == null) {
            return;
        }
        HcBomItemDO item = new HcBomItemDO();
        item.setComponentMaterialId(material.getId());
        item.setComponentMaterialCode(material.getMaterialCode());
        item.setComponentMaterialName(material.getMaterialName());
        item.setComponentType(componentType);
        item.setBaseQty(DEFAULT_QTY);
        item.setLossRate(ZERO_RATE);
        item.setSupplyMode(DEFAULT_SUPPLY_MODE);
        item.setIssueOperationCode(operationCode);
        item.setIssueOperationName(operationName);
        item.setConsumeGroupCode(consumeGroupCode);
        item.setRequiredFlag(true);
        item.setUom(StringUtils.hasText(material.getBaseUom()) ? material.getBaseUom() : "");
        item.setRemark(remark);
        items.add(item);
    }

    private void upsertImportedProductBom(ProductBomImportRow row, List<HcBomItemDO> bomItems) {
        String bomCode = buildProductBomCode(row.productMaterialCode);
        HcBomDO existingBom = selectBomByCode(bomCode);
        HcBomDO entity = new HcBomDO();
        entity.setBomCode(bomCode);
        entity.setBomName(buildProductBomName(row.productMaterial));
        entity.setProductMaterialId(row.productMaterial.getId());
        entity.setProductMaterialCode(row.productMaterial.getMaterialCode());
        entity.setProductMaterialName(row.productMaterial.getMaterialName());
        entity.setProductModelId(row.productModel.getId());
        entity.setProductModelCode(row.productModel.getModelCode());
        entity.setProductModelName(resolveProductModelName(row.productModel));
        entity.setProductSpec(row.productSpec);
        entity.setVersionNo(DEFAULT_VERSION_NO);
        entity.setBomType(resolveProductBomType(row.productModelCode));
        entity.setYieldRate(DEFAULT_QTY);
        entity.setPressSlotContinuousCheckCount(existingBom == null
                ? DEFAULT_PRESS_SLOT_CONTINUOUS_CHECK_COUNT
                : resolvePressSlotContinuousCheckCount(existingBom.getPressSlotContinuousCheckCount()));
        entity.setStatus(1);
        entity.setRemark("由产品BOM Excel导入");

        Long bomId;
        if (existingBom == null) {
            hcBomMapper.insert(entity);
            bomId = entity.getId();
            createHcBomItemList(bomId, bomItems);
        } else {
            entity.setId(existingBom.getId());
            hcBomMapper.updateById(entity);
            bomId = existingBom.getId();
            updateHcBomItemList(bomId, bomItems);
        }
        markProductModelReferenced(entity.getProductModelId());
        updateMaterialDefaultBom(row.productMaterial.getId(), bomId, bomCode, entity.getBomName());
    }

    private void fillPressSlotContinuousCheckCount(HcBomSaveReqVO reqVO) {
        reqVO.setPressSlotContinuousCheckCount(resolvePressSlotContinuousCheckCount(
                reqVO.getPressSlotContinuousCheckCount()));
    }

    private Integer resolvePressSlotContinuousCheckCount(Integer configuredCount) {
        return configuredCount != null && configuredCount > 0
                ? configuredCount
                : DEFAULT_PRESS_SLOT_CONTINUOUS_CHECK_COUNT;
    }

    private void createHcBomItemList(Long parentId, List<HcBomItemDO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(item -> {
            item.clean();
            item.setId(null);
            item.setBomId(parentId);
        });
        hcBomItemMapper.insertBatch(list);
    }

    private void updateHcBomItemList(Long parentId, List<HcBomItemDO> list) {
        List<HcBomItemDO> dbList = hcBomItemMapper.selectListByParentId(parentId);
        if (list == null) {
            list = List.of();
        }

        Set<Long> reqIds = list.stream()
                .map(HcBomItemDO::getId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> deleteIds = dbList.stream()
                .map(HcBomItemDO::getId)
                .filter(id -> !reqIds.contains(id))
                .collect(Collectors.toSet());
        if (!deleteIds.isEmpty()) {
            hcBomItemMapper.deleteBatch(HcBomItemDO::getId, deleteIds);
        }

        List<HcBomItemDO> updateList = list.stream()
                .filter(item -> item.getId() != null && item.getId() > 0)
                .peek(item -> {
                    item.clean();
                    item.setBomId(parentId);
                })
                .toList();
        if (!updateList.isEmpty()) {
            hcBomItemMapper.updateBatch(updateList);
        }

        List<HcBomItemDO> createList = list.stream()
                .filter(item -> item.getId() == null || item.getId() <= 0)
                .peek(item -> {
                    item.clean();
                    item.setId(null);
                    item.setBomId(parentId);
                })
                .toList();
        if (!createList.isEmpty()) {
            hcBomItemMapper.insertBatch(createList);
        }
    }

    private HcMaterialDO selectMaterialByCode(String materialCode) {
        if (!StringUtils.hasText(materialCode)) {
            return null;
        }
        return hcMaterialMapper.selectOne(new LambdaQueryWrapperX<HcMaterialDO>()
                .eq(HcMaterialDO::getMaterialCode, materialCode));
    }

    private HcMaterialDO selectMaterialById(Long materialId) {
        if (materialId == null) {
            return null;
        }
        return hcMaterialMapper.selectById(materialId);
    }

    private HcProductModelDO selectProductModelByCode(String modelCode) {
        if (!StringUtils.hasText(modelCode)) {
            return null;
        }
        return hcProductModelMapper.selectOne(new LambdaQueryWrapperX<HcProductModelDO>()
                .eq(HcProductModelDO::getModelCode, modelCode)
                .eq(HcProductModelDO::getStatus, PRODUCT_MODEL_STATUS_ENABLE));
    }

    private HcBomDO selectBomByCode(String bomCode) {
        if (!StringUtils.hasText(bomCode)) {
            return null;
        }
        return hcBomMapper.selectOne(new LambdaQueryWrapperX<HcBomDO>()
                .eq(HcBomDO::getBomCode, bomCode));
    }

    private void fillProductModelSnapshot(HcBomSaveReqVO reqVO) {
        if (reqVO.getProductModelId() == null) {
            return;
        }
        HcProductModelDO productModel = hcProductModelMapper.selectOne(new LambdaQueryWrapperX<HcProductModelDO>()
                .eq(HcProductModelDO::getId, reqVO.getProductModelId())
                .eq(HcProductModelDO::getStatus, PRODUCT_MODEL_STATUS_ENABLE));
        if (productModel == null) {
            throw exception(HCPRODUCTMODEL_NOT_EXISTS);
        }
        reqVO.setProductModelCode(productModel.getModelCode());
        reqVO.setProductModelName(resolveProductModelName(productModel));
    }

    private void fillProductMaterialSnapshot(HcBomSaveReqVO reqVO) {
        if (reqVO.getProductMaterialId() == null) {
            return;
        }
        HcMaterialDO productMaterial = selectMaterialById(reqVO.getProductMaterialId());
        if (productMaterial == null) {
            throw exception(HCMATERIAL_NOT_EXISTS);
        }
        reqVO.setProductMaterialCode(productMaterial.getMaterialCode());
        reqVO.setProductMaterialName(productMaterial.getMaterialName());
        reqVO.setBomName(buildProductBomName(productMaterial));
    }

    private void fillRecipeSnapshot(HcBomSaveReqVO reqVO) {
        if (reqVO.getRecipeId() == null) {
            if (!StringUtils.hasText(reqVO.getRecipeCode())) {
                reqVO.setRecipeCode(null);
                reqVO.setRecipeName(null);
            }
            return;
        }
        HcRecipeDO recipe = hcRecipeMapper.selectById(reqVO.getRecipeId());
        if (recipe == null) {
            throw exception(HCRECIPE_NOT_EXISTS);
        }
        reqVO.setRecipeCode(recipe.getRecipeCode());
        reqVO.setRecipeName(recipe.getRecipeName());
    }

    private void updateMaterialDefaultBom(Long productMaterialId, Long bomId, String bomCode, String bomName) {
        if (productMaterialId == null || bomId == null) {
            return;
        }
        HcMaterialDO updateObj = new HcMaterialDO();
        updateObj.setId(productMaterialId);
        updateObj.setDefaultBomId(bomId);
        updateObj.setDefaultBomCode(bomCode);
        updateObj.setDefaultBomName(bomName);
        hcMaterialMapper.updateById(updateObj);
    }

    private void markProductModelReferenced(Long productModelId) {
        if (productModelId == null) {
            return;
        }
        HcProductModelDO updateObj = new HcProductModelDO();
        updateObj.setId(productModelId);
        updateObj.setReferencedFlag(true);
        hcProductModelMapper.updateById(updateObj);
    }

    private String readCell(Row row, int cellIndex, DataFormatter formatter) {
        Cell cell = row.getCell(cellIndex);
        return cell == null ? "" : formatter.formatCellValue(cell);
    }

    private String normalizeMaterialCode(String value) {
        String normalized = normalizeText(value);
        int crIndex = normalized.indexOf('\r');
        int lfIndex = normalized.indexOf('\n');
        int lineEnd = -1;
        if (crIndex >= 0 && lfIndex >= 0) {
            lineEnd = Math.min(crIndex, lfIndex);
        } else if (crIndex >= 0) {
            lineEnd = crIndex;
        } else if (lfIndex >= 0) {
            lineEnd = lfIndex;
        }
        return lineEnd >= 0 ? normalized.substring(0, lineEnd).trim() : normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.replace('\u00A0', ' ').trim();
    }

    private String buildProductBomCode(String productMaterialCode) {
        return PRODUCT_BOM_CODE_PREFIX + productMaterialCode;
    }

    private String buildProductBomName(HcMaterialDO productMaterial) {
        if (productMaterial == null) {
            return "";
        }
        return StringUtils.hasText(productMaterial.getSpecModel())
                ? productMaterial.getSpecModel()
                : productMaterial.getMaterialName();
    }

    private String resolveProductModelName(HcProductModelDO productModel) {
        if (productModel == null) {
            return "";
        }
        return StringUtils.hasText(productModel.getModelName()) ? productModel.getModelName() : productModel.getModelCode();
    }

    private String resolveProductBomType(String productModelCode) {
        return StringUtils.hasText(productModelCode) && productModelCode.trim().startsWith("W")
                ? MASS_PRODUCTION_BOM_TYPE : TRIAL_PRODUCTION_BOM_TYPE;
    }

    private void addImportFailure(HcBomProductImportRespVO respVO, String message) {
        respVO.getFailures().add(message);
        respVO.setFailureCount(respVO.getFailures().size());
    }

    private static class ProductBomImportRow {
        private Integer rowNo;
        private String productModelCode;
        private String productSpec;
        private String productMaterialCode;
        private String roughGrindingMaterialCode;
        private String adhesive1IntermediateMaterialCode;
        private String adhesive1AuxMaterialCode;
        private String adhesive2AuxMaterialCode;
        private HcMaterialDO productMaterial;
        private HcProductModelDO productModel;
        private HcMaterialDO roughGrindingMaterial;
        private HcMaterialDO adhesive1IntermediateMaterial;
        private HcMaterialDO adhesive1AuxMaterial;
        private HcMaterialDO adhesive2AuxMaterial;
    }

}
