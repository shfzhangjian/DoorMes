package cn.iocoder.yudao.module.mes.service.hc.visualprintdesigner;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintCustomerInfoPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintCustomerInfoRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintCustomerInfoSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintDesignAttachReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintDesignRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintDesignSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintFieldOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintProductItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo.HcVisualPrintProductItemSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner.HcVisualPrintCustomerInfoDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner.HcVisualPrintDesignDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner.HcVisualPrintLabelBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner.HcVisualPrintProductItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.visualprintdesigner.HcVisualPrintCustomerInfoMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.visualprintdesigner.HcVisualPrintDesignMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.visualprintdesigner.HcVisualPrintLabelBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.visualprintdesigner.HcVisualPrintProductItemMapper;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcVisualPrintDesignerServiceImpl implements HcVisualPrintDesignerService {

    private static final int DATA_START_ROW_INDEX = 1;
    private static final Pattern IMAGE_ID_PATTERN = Pattern.compile("(ID_[A-Za-z0-9]+)");
    private static final DateTimeFormatter BATCH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final Map<String, String> IMAGE_FILE_MAP = buildImageFileMap();
    private static final Map<String, String> LABEL_KIND_NAME_MAP = Map.of(
            "padBack", "pad背面标签",
            "cleanBag", "洁净袋标签",
            "boxFront", "包装盒正面大标签",
            "customerSide", "客户侧标");

    @Resource
    private HcVisualPrintCustomerInfoMapper customerInfoMapper;
    @Resource
    private HcVisualPrintDesignMapper designMapper;
    @Resource
    private HcVisualPrintLabelBindingMapper labelBindingMapper;
    @Resource
    private HcVisualPrintProductItemMapper productItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomerInfo(HcVisualPrintCustomerInfoSaveReqVO createReqVO) {
        List<HcVisualPrintProductItemSaveReqVO> productItems = resolveSaveProductItems(createReqVO);
        HcVisualPrintCustomerInfoDO entity = BeanUtils.toBean(createReqVO, HcVisualPrintCustomerInfoDO.class);
        applyCustomerSummaryFromProductItems(entity, productItems);
        normalizeCustomerInfo(entity);
        Long tenantId = currentTenantId();
        entity.setTenantId(tenantId);
        customerInfoMapper.insert(entity);
        saveProductItems(entity.getId(), productItems, tenantId, false);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerInfo(HcVisualPrintCustomerInfoSaveReqVO updateReqVO) {
        HcVisualPrintCustomerInfoDO exists = validateCustomerInfoExists(updateReqVO.getId());
        List<HcVisualPrintProductItemSaveReqVO> productItems = resolveSaveProductItems(updateReqVO);
        HcVisualPrintCustomerInfoDO entity = BeanUtils.toBean(updateReqVO, HcVisualPrintCustomerInfoDO.class);
        entity.setSourceRow(exists.getSourceRow());
        applyCustomerSummaryFromProductItems(entity, productItems);
        normalizeCustomerInfo(entity);
        customerInfoMapper.updateById(entity);
        saveProductItems(exists.getId(), productItems, currentTenantId(), updateReqVO.getProductItems() != null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomerInfo(Long id, Long productItemId) {
        validateCustomerInfoExists(id);
        if (productItemId != null) {
            validateProductItemExists(id, productItemId);
            labelBindingMapper.physicalDeleteByProductItemId(productItemId);
            designMapper.physicalDeleteByProductItemId(productItemId);
            productItemMapper.deleteById(productItemId);
            if (!productItemMapper.selectListByCustomerInfoId(id).isEmpty()) {
                return;
            }
        }
        labelBindingMapper.physicalDeleteByCustomerInfoId(id);
        designMapper.physicalDeleteByCustomerInfoId(id);
        productItemMapper.physicalDeleteByCustomerInfoId(id);
        customerInfoMapper.deleteById(id);
    }

    @Override
    public HcVisualPrintCustomerInfoRespVO getCustomerInfoDetail(Long id) {
        HcVisualPrintCustomerInfoDO entity = validateCustomerInfoExists(id);
        HcVisualPrintCustomerInfoRespVO respVO = BeanUtils.toBean(entity, HcVisualPrintCustomerInfoRespVO.class);
        List<HcVisualPrintProductItemDO> productItems = productItemMapper.selectListByCustomerInfoId(id);
        respVO.setProductItems(BeanUtils.toBean(productItems, HcVisualPrintProductItemRespVO.class));
        respVO.setDesigns(getDesigns(id, null));
        return respVO;
    }

    @Override
    public PageResult<HcVisualPrintCustomerInfoRespVO> getCustomerInfoPage(HcVisualPrintCustomerInfoPageReqVO pageReqVO) {
        List<HcVisualPrintCustomerInfoRespVO> rows = buildCustomerInfoRows(
                customerInfoMapper.selectList(pageReqVO), pageReqVO);
        int pageNo = pageReqVO.getPageNo() == null ? 1 : pageReqVO.getPageNo();
        int pageSize = pageReqVO.getPageSize() == null ? 10 : pageReqVO.getPageSize();
        int fromIndex = Math.min(Math.max(0, (pageNo - 1) * pageSize), rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    @Override
    public List<HcVisualPrintCustomerInfoRespVO> getCustomerInfoList(HcVisualPrintCustomerInfoPageReqVO reqVO) {
        return buildCustomerInfoRows(customerInfoMapper.selectList(reqVO), reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcVisualPrintImportRespVO importCustomerInfoExcel(MultipartFile file) throws IOException {
        HcVisualPrintImportRespVO respVO = new HcVisualPrintImportRespVO();
        if (file == null || file.isEmpty()) {
            respVO.getMessages().add("导入文件为空");
            return respVO;
        }

        String importBatchNo = "VP" + LocalDateTime.now().format(BATCH_TIME_FORMATTER);
        respVO.setImportBatchNo(importBatchNo);
        List<ImportCustomer> customers = parseImportRows(file, importBatchNo, respVO);
        Long tenantId = currentTenantId();
        Map<Integer, Long> masterIdBySourceRow = new LinkedHashMap<>();
        Map<Integer, Long> itemIdBySourceRow = new LinkedHashMap<>();
        Map<Integer, Long> itemMasterIdBySourceRow = new LinkedHashMap<>();
        for (ImportCustomer customer : customers) {
            HcVisualPrintCustomerInfoDO entity = customer.masterRow.toCustomerEntity();
            entity.setTenantId(tenantId);
            HcVisualPrintCustomerInfoDO exists = entity.getSourceRow() == null ? null
                    : customerInfoMapper.selectBySourceRow(tenantId, entity.getSourceRow());
            if (exists == null) {
                customerInfoMapper.insert(entity);
            } else {
                entity.setId(exists.getId());
                customerInfoMapper.updateById(entity);
            }
            masterIdBySourceRow.put(entity.getSourceRow(), entity.getId());
            for (ImportRow row : customer.productRows) {
                HcVisualPrintProductItemDO item = row.toProductItemEntity(entity.getId());
                item.setTenantId(tenantId);
                normalizeProductItem(item);
                HcVisualPrintProductItemDO existsItem = item.getSourceRow() == null ? null
                        : productItemMapper.selectByCustomerInfoIdAndSourceRow(tenantId, entity.getId(),
                                item.getSourceRow());
                if (existsItem == null) {
                    productItemMapper.insert(item);
                    respVO.setCreateCount(respVO.getCreateCount() + 1);
                } else {
                    item.setId(existsItem.getId());
                    productItemMapper.updateById(item);
                    respVO.setUpdateCount(respVO.getUpdateCount() + 1);
                }
                itemIdBySourceRow.put(item.getSourceRow(), item.getId());
                itemMasterIdBySourceRow.put(item.getSourceRow(), entity.getId());
                syncLabelBindingsForItem(entity.getId(), item, tenantId);
            }
        }
        cleanupFlattenedCustomerRows(tenantId, masterIdBySourceRow, itemMasterIdBySourceRow, itemIdBySourceRow);
        respVO.getMessages().add(StrUtil.format("导入完成：读取 {} 条产品尺寸明细，客户主表 {} 个，新建明细 {} 条，更新明细 {} 条，跳过 {} 行",
                respVO.getTotalCount(), customers.size(), respVO.getCreateCount(), respVO.getUpdateCount(),
                respVO.getSkipCount()));
        return respVO;
    }

    @Override
    public List<HcVisualPrintFieldOptionRespVO> getFieldOptions() {
        return List.of(
                field("serialNo", "序号", "A"),
                field("customer", "客户", "B"),
                field("productType", "产品类型", "C"),
                field("productModel", "产品型号", ""),
                field("productTypeName", "产品名称", ""),
                field("sizeMm", "尺寸/mm", "D"),
                field("padBackLabelImageId", "pad背面标签", "E"),
                field("cleanBagLabelImageId", "洁净袋标签", "F"),
                field("boxFrontLabelImageId", "包装盒正面大标签", "G"),
                field("customerSideLabelImageId", "客户侧标", "H"),
                field("customerSideSize", "客户侧标尺寸", "I"),
                field("customerSideMethod", "客户侧标方式", "J"),
                field("shippingMethod", "发货方式", "K"),
                field("needPaperCoa", "是否需要随货纸版COA", "L"),
                field("needEcoa", "是否需要ECOA", "M"),
                field("hasMark", "是否有唛头", "N"),
                field("deliveryNote", "送货单", "O"),
                field("shipmentFilePackageMethod", "随货文件包装方式", "P"),
                field("customerSideTemplate", "客户侧标模板", "Q"),
                field("specialRemark", "特殊备注", "R"),
                field("productNo", "产品编码 Product No.", ""),
                field("productInfo", "产品信息 Product Infor.", ""),
                field("quantity", "数量 Quantity", ""),
                field("padNo", "片号 Pad No.", ""),
                field("batchNo", "生产批号 Batch No.", ""),
                field("productionDate", "生产日期 Production Date", ""),
                field("expirationDate", "失效日期 Expiration Date", ""),
                field("plant", "Plant", ""),
                field("pn", "PN", ""),
                field("materialDescription", "Material Description", ""),
                field("vendorPn", "Vendor PN", ""),
                field("info", "INFO", ""),
                field("packageIndex", "PKG of TTL", ""),
                field("expDate", "EXP Date", ""),
                field("shippingDate", "Shipping Date", ""),
                field("purUom", "PUR UOM", ""),
                field("po", "PO", ""));
    }

    @Override
    public List<HcVisualPrintDesignRespVO> getDesigns(Long customerInfoId, Long productItemId) {
        validateCustomerInfoExists(customerInfoId);
        validateProductItemExists(customerInfoId, productItemId);
        if (productItemId == null) {
            return BeanUtils.toBean(designMapper.selectListByInfoId(customerInfoId, null),
                    HcVisualPrintDesignRespVO.class);
        }
        List<HcVisualPrintDesignRespVO> rows = new ArrayList<>();
        for (HcVisualPrintLabelBindingDO binding : labelBindingMapper.selectListByProductItemId(productItemId)) {
            HcVisualPrintDesignDO design = resolveDesignByBinding(binding);
            if (design != null) {
                rows.add(buildDesignRespVO(design, binding));
            }
        }
        return rows;
    }

    @Override
    public HcVisualPrintDesignRespVO getDesign(Long customerInfoId, Long productItemId, String labelKind) {
        validateCustomerInfoExists(customerInfoId);
        validateProductItemExists(customerInfoId, productItemId);
        String normalizedKind = normalizeLabelKind(labelKind);
        if (productItemId == null) {
            HcVisualPrintDesignDO entity = designMapper.selectByInfoIdAndKind(customerInfoId, null, normalizedKind);
            return entity == null ? null : buildDesignRespVO(entity, null);
        }
        HcVisualPrintLabelBindingDO binding = labelBindingMapper.selectByProductItemIdAndKind(productItemId,
                normalizedKind);
        HcVisualPrintDesignDO entity = resolveDesignByBinding(binding);
        return entity == null ? null : buildDesignRespVO(entity, binding);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDesign(HcVisualPrintDesignSaveReqVO saveReqVO) {
        validateCustomerInfoExists(saveReqVO.getCustomerInfoId());
        HcVisualPrintProductItemDO productItem = validateProductItemExists(saveReqVO.getCustomerInfoId(),
                saveReqVO.getProductItemId());
        HcVisualPrintDesignDO entity = BeanUtils.toBean(saveReqVO, HcVisualPrintDesignDO.class);
        normalizeDesign(entity);
        entity.setTenantId(currentTenantId());
        boolean forceNew = Boolean.TRUE.equals(saveReqVO.getForceNew());
        HcVisualPrintLabelBindingDO binding = null;
        if (productItem != null) {
            if (forceNew) {
                entity.setId(null);
                entity.setProductItemId(null);
                entity.setImageId(normalizeImageKey(firstNotBlank(getItemLabelImageId(productItem, entity.getLabelKind()),
                        entity.getImageId())));
                entity.setImageFile(normalizeImageKey(firstNotBlank(getItemLabelImageFile(productItem, entity.getLabelKind()),
                        entity.getImageFile())));
            } else {
                binding = ensureLabelBinding(productItem, entity);
                entity.setProductItemId(hasBindingImage(binding) ? null : productItem.getId());
            }
        }
        HcVisualPrintDesignDO exists = forceNew ? null : resolveDesignForSave(entity, binding);
        if (exists == null) {
            designMapper.insert(entity);
        } else {
            entity.setId(exists.getId());
            designMapper.updateById(entity);
        }
        if (binding != null && !forceNew) {
            attachSharedDesignToBindings(binding, entity.getId());
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcVisualPrintDesignRespVO attachDesign(HcVisualPrintDesignAttachReqVO attachReqVO) {
        validateCustomerInfoExists(attachReqVO.getCustomerInfoId());
        HcVisualPrintDesignDO design = designMapper.selectById(attachReqVO.getDesignId());
        if (design == null || !attachReqVO.getCustomerInfoId().equals(design.getCustomerInfoId())) {
            throw invalidParamException("设计稿不存在或不属于当前客户");
        }
        String labelKind = normalizeLabelKind(attachReqVO.getLabelKind());
        if (!labelKind.equals(design.getLabelKind())) {
            throw invalidParamException("设计稿标签类型与挂接标签类型不一致");
        }
        Set<Long> selectedIds = new LinkedHashSet<>(attachReqVO.getProductItemIds());
        if (selectedIds.isEmpty()) {
            throw invalidParamException("请选择需要挂接的产品型号尺寸");
        }

        Long tenantId = currentTenantId();
        Map<Long, HcVisualPrintProductItemDO> itemMap = new LinkedHashMap<>();
        for (HcVisualPrintProductItemDO item : productItemMapper.selectListByCustomerInfoId(attachReqVO.getCustomerInfoId())) {
            itemMap.put(item.getId(), item);
        }
        for (Long productItemId : selectedIds) {
            if (!itemMap.containsKey(productItemId)) {
                throw invalidParamException("产品型号尺寸不存在或不属于当前客户：{}", productItemId);
            }
        }

        for (HcVisualPrintLabelBindingDO binding : labelBindingMapper.selectListByDesignId(design.getId())) {
            if (!attachReqVO.getCustomerInfoId().equals(binding.getCustomerInfoId())
                    || !labelKind.equals(binding.getLabelKind())
                    || selectedIds.contains(binding.getProductItemId())) {
                continue;
            }
            binding.setDesignId(null);
            labelBindingMapper.updateById(binding);
        }

        HcVisualPrintLabelBindingDO firstBinding = null;
        for (Long productItemId : selectedIds) {
            HcVisualPrintProductItemDO item = itemMap.get(productItemId);
            if (StrUtil.isNotBlank(design.getImageId()) || StrUtil.isNotBlank(design.getImageFile())) {
                setItemLabelImage(item, labelKind, design.getImageId(), design.getImageFile());
            }
            normalizeProductItem(item);
            productItemMapper.updateById(item);
            syncLabelBindingsForItem(attachReqVO.getCustomerInfoId(), item, tenantId);
            HcVisualPrintLabelBindingDO binding = labelBindingMapper.selectByProductItemIdAndKind(productItemId,
                    labelKind);
            if (binding == null) {
                binding = ensureLabelBinding(item, design);
            }
            binding.setDesignId(design.getId());
            binding.setImageId(normalizeImageKey(firstNotBlank(binding.getImageId(), design.getImageId())));
            binding.setImageFile(normalizeImageKey(firstNotBlank(binding.getImageFile(), design.getImageFile())));
            labelBindingMapper.updateById(binding);
            if (firstBinding == null) {
                firstBinding = binding;
            }
        }
        return buildDesignRespVO(design, firstBinding);
    }

    private HcVisualPrintDesignRespVO buildDesignRespVO(HcVisualPrintDesignDO entity,
            HcVisualPrintLabelBindingDO binding) {
        HcVisualPrintDesignRespVO respVO = BeanUtils.toBean(entity, HcVisualPrintDesignRespVO.class);
        if (binding != null) {
            respVO.setBindingId(binding.getId());
            respVO.setProductItemId(binding.getProductItemId());
        }
        List<HcVisualPrintLabelBindingDO> linkedBindings = resolveLinkedBindings(entity, binding);
        respVO.setSharedCount(linkedBindings.size());
        respVO.setLinkedProductItems(buildLinkedProductItems(entity, binding, linkedBindings));
        return respVO;
    }

    private List<HcVisualPrintLabelBindingDO> resolveLinkedBindings(HcVisualPrintDesignDO entity,
            HcVisualPrintLabelBindingDO binding) {
        if (entity.getId() != null) {
            List<HcVisualPrintLabelBindingDO> bindings = labelBindingMapper.selectListByDesignId(entity.getId());
            if (!bindings.isEmpty()) {
                return bindings;
            }
        }
        if (binding != null && (StrUtil.isNotBlank(binding.getImageId()) || StrUtil.isNotBlank(binding.getImageFile()))) {
            return labelBindingMapper.selectListByTemplateKey(binding.getCustomerInfoId(), binding.getLabelKind(),
                    binding.getImageId(), binding.getImageFile());
        }
        return binding == null ? List.of() : List.of(binding);
    }

    private List<HcVisualPrintProductItemRespVO> buildLinkedProductItems(HcVisualPrintDesignDO entity,
            HcVisualPrintLabelBindingDO binding, List<HcVisualPrintLabelBindingDO> linkedBindings) {
        List<Long> productItemIds = linkedBindings.stream()
                .map(HcVisualPrintLabelBindingDO::getProductItemId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (productItemIds.isEmpty()) {
            Long fallbackProductItemId = binding == null ? entity.getProductItemId() : binding.getProductItemId();
            if (fallbackProductItemId != null) {
                productItemIds = List.of(fallbackProductItemId);
            }
        }
        if (productItemIds.isEmpty()) {
            return List.of();
        }
        return productItemMapper.selectBatchIds(productItemIds).stream()
                .sorted(Comparator.comparing(HcVisualPrintProductItemDO::getSourceRow,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(HcVisualPrintProductItemDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(item -> BeanUtils.toBean(item, HcVisualPrintProductItemRespVO.class))
                .toList();
    }

    private HcVisualPrintDesignDO resolveDesignByBinding(HcVisualPrintLabelBindingDO binding) {
        if (binding == null) {
            return null;
        }
        if (binding.getDesignId() != null) {
            return designMapper.selectById(binding.getDesignId());
        }
        if (StrUtil.isBlank(binding.getImageId()) && StrUtil.isBlank(binding.getImageFile())) {
            return null;
        }
        return designMapper.selectSharedTemplateByImage(binding.getCustomerInfoId(), binding.getLabelKind(),
                binding.getImageId(), binding.getImageFile());
    }

    private HcVisualPrintDesignDO resolveDesignForSave(HcVisualPrintDesignDO entity,
            HcVisualPrintLabelBindingDO binding) {
        if (entity.getId() != null) {
            return designMapper.selectById(entity.getId());
        }
        if (binding != null) {
            if (binding.getDesignId() != null) {
                return designMapper.selectById(binding.getDesignId());
            }
            if (StrUtil.isNotBlank(binding.getImageId()) || StrUtil.isNotBlank(binding.getImageFile())) {
                return designMapper.selectSharedTemplateByImage(entity.getCustomerInfoId(), entity.getLabelKind(),
                        binding.getImageId(), binding.getImageFile());
            }
        }
        return designMapper.selectByInfoIdAndKind(entity.getCustomerInfoId(), entity.getProductItemId(),
                entity.getLabelKind());
    }

    private HcVisualPrintLabelBindingDO ensureLabelBinding(HcVisualPrintProductItemDO productItem,
            HcVisualPrintDesignDO design) {
        HcVisualPrintLabelBindingDO binding = labelBindingMapper.selectByProductItemIdAndKind(productItem.getId(),
                design.getLabelKind());
        String imageId = normalizeImageKey(firstNotBlank(getItemLabelImageId(productItem, design.getLabelKind()),
                binding == null ? "" : binding.getImageId(), design.getImageId()));
        String imageFile = normalizeImageKey(firstNotBlank(getItemLabelImageFile(productItem, design.getLabelKind()),
                binding == null ? "" : binding.getImageFile(), design.getImageFile()));
        Long designId = binding == null ? null : binding.getDesignId();
        if (designId == null && (StrUtil.isNotBlank(imageId) || StrUtil.isNotBlank(imageFile))) {
            HcVisualPrintDesignDO exists = designMapper.selectSharedTemplateByImage(productItem.getCustomerInfoId(),
                    design.getLabelKind(), imageId, imageFile);
            designId = exists == null ? null : exists.getId();
        }
        if (binding == null) {
            binding = HcVisualPrintLabelBindingDO.builder()
                    .customerInfoId(productItem.getCustomerInfoId())
                    .productItemId(productItem.getId())
                    .labelKind(design.getLabelKind())
                    .sourceRow(productItem.getSourceRow())
                    .status(0)
                    .importBatchNo(productItem.getImportBatchNo())
                    .rawJson(productItem.getRawJson())
                    .tenantId(currentTenantId())
                    .build();
            binding.setImageId(imageId);
            binding.setImageFile(imageFile);
            binding.setDesignId(designId);
            labelBindingMapper.insert(binding);
        } else {
            binding.setImageId(imageId);
            binding.setImageFile(imageFile);
            binding.setDesignId(designId);
            binding.setSourceRow(productItem.getSourceRow());
            binding.setStatus(0);
            binding.setImportBatchNo(productItem.getImportBatchNo());
            binding.setRawJson(productItem.getRawJson());
            labelBindingMapper.updateById(binding);
        }
        return binding;
    }

    private void attachSharedDesignToBindings(HcVisualPrintLabelBindingDO binding, Long designId) {
        if (binding == null || designId == null) {
            return;
        }
        if (!hasBindingImage(binding)) {
            binding.setDesignId(designId);
            labelBindingMapper.updateById(binding);
            return;
        }
        for (HcVisualPrintLabelBindingDO row : labelBindingMapper.selectListByTemplateKey(binding.getCustomerInfoId(),
                binding.getLabelKind(), binding.getImageId(), binding.getImageFile())) {
            row.setDesignId(designId);
            labelBindingMapper.updateById(row);
        }
    }

    private boolean hasBindingImage(HcVisualPrintLabelBindingDO binding) {
        return binding != null
                && (StrUtil.isNotBlank(binding.getImageId()) || StrUtil.isNotBlank(binding.getImageFile()));
    }

    private void syncLabelBindingsForItem(Long customerInfoId, HcVisualPrintProductItemDO item, Long tenantId) {
        upsertLabelBinding(customerInfoId, item, tenantId, "padBack", item.getPadBackLabelImageId(),
                item.getPadBackLabelImageFile());
        upsertLabelBinding(customerInfoId, item, tenantId, "cleanBag", item.getCleanBagLabelImageId(),
                item.getCleanBagLabelImageFile());
        upsertLabelBinding(customerInfoId, item, tenantId, "boxFront", item.getBoxFrontLabelImageId(),
                item.getBoxFrontLabelImageFile());
        upsertLabelBinding(customerInfoId, item, tenantId, "customerSide", item.getCustomerSideLabelImageId(),
                item.getCustomerSideLabelImageFile());
    }

    private void upsertLabelBinding(Long customerInfoId, HcVisualPrintProductItemDO item, Long tenantId,
            String labelKind, String imageId, String imageFile) {
        HcVisualPrintLabelBindingDO binding = labelBindingMapper.selectByProductItemIdAndKind(item.getId(), labelKind);
        String normalizedImageId = normalizeImageKey(imageId);
        String normalizedImageFile = normalizeImageKey(imageFile);
        if (StrUtil.isBlank(normalizedImageId) && StrUtil.isBlank(normalizedImageFile)) {
            if (binding != null && (binding.getDesignId() == null || hasBindingImage(binding))) {
                labelBindingMapper.physicalDeleteByProductItemIdAndKind(item.getId(), labelKind);
            }
            return;
        }
        boolean sameTemplateKey = binding != null
                && normalizedImageId.equals(normalizeImageKey(binding.getImageId()))
                && normalizedImageFile.equals(normalizeImageKey(binding.getImageFile()));
        Long designId = sameTemplateKey ? binding.getDesignId() : null;
        HcVisualPrintDesignDO sharedTemplate = designMapper.selectSharedTemplateByImage(customerInfoId, labelKind,
                normalizedImageId, normalizedImageFile);
        if (sharedTemplate != null) {
            designId = sharedTemplate.getId();
        }
        if (binding == null) {
            binding = HcVisualPrintLabelBindingDO.builder()
                    .customerInfoId(customerInfoId)
                    .productItemId(item.getId())
                    .labelKind(labelKind)
                    .designId(designId)
                    .imageId(normalizedImageId)
                    .imageFile(normalizedImageFile)
                    .sourceRow(item.getSourceRow())
                    .status(0)
                    .importBatchNo(item.getImportBatchNo())
                    .rawJson(item.getRawJson())
                    .tenantId(tenantId)
                    .build();
            labelBindingMapper.insert(binding);
            return;
        }
        binding.setCustomerInfoId(customerInfoId);
        binding.setImageId(normalizedImageId);
        binding.setImageFile(normalizedImageFile);
        binding.setDesignId(designId);
        binding.setSourceRow(item.getSourceRow());
        binding.setStatus(0);
        binding.setImportBatchNo(item.getImportBatchNo());
        binding.setRawJson(item.getRawJson());
        labelBindingMapper.updateById(binding);
    }

    private List<HcVisualPrintCustomerInfoRespVO> buildCustomerInfoRows(List<HcVisualPrintCustomerInfoDO> customers,
            HcVisualPrintCustomerInfoPageReqVO reqVO) {
        Map<Long, List<HcVisualPrintProductItemDO>> itemMap = buildProductItemMap(customers);
        List<HcVisualPrintCustomerInfoRespVO> rows = new ArrayList<>();
        for (HcVisualPrintCustomerInfoDO customer : customers) {
            List<HcVisualPrintProductItemDO> productItems = itemMap.get(customer.getId());
            if (productItems == null || productItems.isEmpty()) {
                HcVisualPrintCustomerInfoRespVO row = buildCustomerInfoRow(customer, null);
                if (matchesRow(row, reqVO)) {
                    rows.add(row);
                }
                continue;
            }
            for (HcVisualPrintProductItemDO productItem : productItems) {
                HcVisualPrintCustomerInfoRespVO row = buildCustomerInfoRow(customer, productItem);
                if (matchesRow(row, reqVO)) {
                    rows.add(row);
                }
            }
        }
        rows.sort(Comparator.comparingInt((HcVisualPrintCustomerInfoRespVO row) -> sortableNumber(row.getSerialNo()))
                .thenComparingInt(row -> row.getSourceRow() == null ? Integer.MAX_VALUE : row.getSourceRow())
                .thenComparingLong(row -> row.getProductItemId() == null ? Long.MAX_VALUE : row.getProductItemId())
                .thenComparingLong(row -> row.getId() == null ? Long.MAX_VALUE : row.getId()));
        return rows;
    }

    private Map<Long, List<HcVisualPrintProductItemDO>> buildProductItemMap(
            List<HcVisualPrintCustomerInfoDO> customers) {
        List<Long> customerInfoIds = new ArrayList<>();
        for (HcVisualPrintCustomerInfoDO customer : customers) {
            if (customer.getId() != null) {
                customerInfoIds.add(customer.getId());
            }
        }
        Map<Long, List<HcVisualPrintProductItemDO>> itemMap = new LinkedHashMap<>();
        for (HcVisualPrintProductItemDO item : productItemMapper.selectListByCustomerInfoIds(customerInfoIds)) {
            itemMap.computeIfAbsent(item.getCustomerInfoId(), key -> new ArrayList<>()).add(item);
        }
        return itemMap;
    }

    private HcVisualPrintCustomerInfoRespVO buildCustomerInfoRow(HcVisualPrintCustomerInfoDO customer,
            HcVisualPrintProductItemDO productItem) {
        HcVisualPrintCustomerInfoRespVO row = BeanUtils.toBean(customer, HcVisualPrintCustomerInfoRespVO.class);
        row.setMasterSourceRow(customer.getSourceRow());
        if (productItem == null) {
            return row;
        }
        row.setProductItemId(productItem.getId());
        row.setSourceRow(productItem.getSourceRow());
        row.setProductType(productItem.getProductType());
        row.setSizeMm(productItem.getSizeMm());
        row.setPadBackLabelImageId(firstNotBlank(productItem.getPadBackLabelImageId(),
                customer.getPadBackLabelImageId()));
        row.setPadBackLabelImageFile(firstNotBlank(productItem.getPadBackLabelImageFile(),
                customer.getPadBackLabelImageFile()));
        row.setCleanBagLabelImageId(firstNotBlank(productItem.getCleanBagLabelImageId(),
                customer.getCleanBagLabelImageId()));
        row.setCleanBagLabelImageFile(firstNotBlank(productItem.getCleanBagLabelImageFile(),
                customer.getCleanBagLabelImageFile()));
        row.setBoxFrontLabelImageId(firstNotBlank(productItem.getBoxFrontLabelImageId(),
                customer.getBoxFrontLabelImageId()));
        row.setBoxFrontLabelImageFile(firstNotBlank(productItem.getBoxFrontLabelImageFile(),
                customer.getBoxFrontLabelImageFile()));
        row.setCustomerSideLabelImageId(firstNotBlank(productItem.getCustomerSideLabelImageId(),
                customer.getCustomerSideLabelImageId()));
        row.setCustomerSideLabelImageFile(firstNotBlank(productItem.getCustomerSideLabelImageFile(),
                customer.getCustomerSideLabelImageFile()));
        fillLabelTemplateSummary(row, productItem.getId(), "padBack");
        fillLabelTemplateSummary(row, productItem.getId(), "cleanBag");
        fillLabelTemplateSummary(row, productItem.getId(), "boxFront");
        fillLabelTemplateSummary(row, productItem.getId(), "customerSide");
        return row;
    }

    private void fillLabelTemplateSummary(HcVisualPrintCustomerInfoRespVO row, Long productItemId, String labelKind) {
        HcVisualPrintLabelBindingDO binding = labelBindingMapper.selectByProductItemIdAndKind(productItemId, labelKind);
        HcVisualPrintDesignDO design = resolveDesignByBinding(binding);
        Long designId = design == null ? null : design.getId();
        Long sharedTotal = designId == null ? 0L : labelBindingMapper.selectCountByDesignId(designId);
        int sharedCount = Math.toIntExact(sharedTotal == null ? 0L : sharedTotal);
        if ("padBack".equals(labelKind)) {
            row.setPadBackDesignId(designId);
            row.setPadBackSharedCount(sharedCount);
        } else if ("cleanBag".equals(labelKind)) {
            row.setCleanBagDesignId(designId);
            row.setCleanBagSharedCount(sharedCount);
        } else if ("boxFront".equals(labelKind)) {
            row.setBoxFrontDesignId(designId);
            row.setBoxFrontSharedCount(sharedCount);
        } else if ("customerSide".equals(labelKind)) {
            row.setCustomerSideDesignId(designId);
            row.setCustomerSideSharedCount(sharedCount);
        }
    }

    private boolean matchesRow(HcVisualPrintCustomerInfoRespVO row, HcVisualPrintCustomerInfoPageReqVO reqVO) {
        return matchesLike(row.getProductType(), reqVO.getProductType())
                && matchesLike(row.getSizeMm(), reqVO.getSizeMm())
                && matchesLabelKind(row, reqVO.getLabelKind());
    }

    private boolean matchesLike(String value, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return true;
        }
        return StrUtil.containsIgnoreCase(StrUtil.nullToEmpty(value), StrUtil.trim(keyword));
    }

    private boolean matchesLabelKind(HcVisualPrintCustomerInfoRespVO row, String labelKind) {
        if (StrUtil.isBlank(labelKind)) {
            return true;
        }
        if ("padBack".equals(labelKind)) {
            return StrUtil.isNotBlank(row.getPadBackLabelImageId())
                    || StrUtil.isNotBlank(row.getPadBackLabelImageFile());
        }
        if ("cleanBag".equals(labelKind)) {
            return StrUtil.isNotBlank(row.getCleanBagLabelImageId())
                    || StrUtil.isNotBlank(row.getCleanBagLabelImageFile());
        }
        if ("boxFront".equals(labelKind)) {
            return StrUtil.isNotBlank(row.getBoxFrontLabelImageId())
                    || StrUtil.isNotBlank(row.getBoxFrontLabelImageFile());
        }
        if ("customerSide".equals(labelKind)) {
            return StrUtil.isNotBlank(row.getCustomerSideLabelImageId())
                    || StrUtil.isNotBlank(row.getCustomerSideLabelImageFile());
        }
        return true;
    }

    private int sortableNumber(String value) {
        String text = StrUtil.trim(value);
        if (StrUtil.isBlank(text)) {
            return Integer.MAX_VALUE;
        }
        Matcher matcher = Pattern.compile("\\d+").matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group()) : Integer.MAX_VALUE;
    }

    private String firstNotBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : StrUtil.nullToEmpty(second);
    }

    private String firstNotBlank(String first, String second, String third) {
        return firstNotBlank(firstNotBlank(first, second), third);
    }

    private String normalizeImageKey(String value) {
        return StrUtil.nullToEmpty(StrUtil.trim(value));
    }

    private String getItemLabelImageId(HcVisualPrintProductItemDO item, String labelKind) {
        if ("padBack".equals(labelKind)) {
            return item.getPadBackLabelImageId();
        }
        if ("cleanBag".equals(labelKind)) {
            return item.getCleanBagLabelImageId();
        }
        if ("boxFront".equals(labelKind)) {
            return item.getBoxFrontLabelImageId();
        }
        if ("customerSide".equals(labelKind)) {
            return item.getCustomerSideLabelImageId();
        }
        return "";
    }

    private String getItemLabelImageFile(HcVisualPrintProductItemDO item, String labelKind) {
        if ("padBack".equals(labelKind)) {
            return item.getPadBackLabelImageFile();
        }
        if ("cleanBag".equals(labelKind)) {
            return item.getCleanBagLabelImageFile();
        }
        if ("boxFront".equals(labelKind)) {
            return item.getBoxFrontLabelImageFile();
        }
        if ("customerSide".equals(labelKind)) {
            return item.getCustomerSideLabelImageFile();
        }
        return "";
    }

    private void setItemLabelImage(HcVisualPrintProductItemDO item, String labelKind, String imageId,
            String imageFile) {
        if ("padBack".equals(labelKind)) {
            item.setPadBackLabelImageId(imageId);
            item.setPadBackLabelImageFile(imageFile);
        } else if ("cleanBag".equals(labelKind)) {
            item.setCleanBagLabelImageId(imageId);
            item.setCleanBagLabelImageFile(imageFile);
        } else if ("boxFront".equals(labelKind)) {
            item.setBoxFrontLabelImageId(imageId);
            item.setBoxFrontLabelImageFile(imageFile);
        } else if ("customerSide".equals(labelKind)) {
            item.setCustomerSideLabelImageId(imageId);
            item.setCustomerSideLabelImageFile(imageFile);
        }
    }

    private HcVisualPrintProductItemDO buildProductItem(Long customerInfoId,
            HcVisualPrintCustomerInfoSaveReqVO reqVO) {
        HcVisualPrintProductItemSaveReqVO itemReqVO = new HcVisualPrintProductItemSaveReqVO();
        itemReqVO.setId(reqVO.getProductItemId());
        itemReqVO.setSourceRow(reqVO.getSourceRow());
        itemReqVO.setProductType(reqVO.getProductType());
        itemReqVO.setSizeMm(reqVO.getSizeMm());
        itemReqVO.setPadBackLabelImageId(reqVO.getPadBackLabelImageId());
        itemReqVO.setPadBackLabelImageFile(reqVO.getPadBackLabelImageFile());
        itemReqVO.setCleanBagLabelImageId(reqVO.getCleanBagLabelImageId());
        itemReqVO.setCleanBagLabelImageFile(reqVO.getCleanBagLabelImageFile());
        itemReqVO.setBoxFrontLabelImageId(reqVO.getBoxFrontLabelImageId());
        itemReqVO.setBoxFrontLabelImageFile(reqVO.getBoxFrontLabelImageFile());
        itemReqVO.setCustomerSideLabelImageId(reqVO.getCustomerSideLabelImageId());
        itemReqVO.setCustomerSideLabelImageFile(reqVO.getCustomerSideLabelImageFile());
        itemReqVO.setStatus(reqVO.getStatus());
        return buildProductItem(customerInfoId, itemReqVO);
    }

    private HcVisualPrintProductItemDO buildProductItem(Long customerInfoId,
            HcVisualPrintProductItemSaveReqVO reqVO) {
        return HcVisualPrintProductItemDO.builder()
                .id(reqVO.getId())
                .customerInfoId(customerInfoId)
                .sourceRow(reqVO.getSourceRow())
                .productType(reqVO.getProductType())
                .sizeMm(reqVO.getSizeMm())
                .padBackLabelImageId(reqVO.getPadBackLabelImageId())
                .padBackLabelImageFile(reqVO.getPadBackLabelImageFile())
                .cleanBagLabelImageId(reqVO.getCleanBagLabelImageId())
                .cleanBagLabelImageFile(reqVO.getCleanBagLabelImageFile())
                .boxFrontLabelImageId(reqVO.getBoxFrontLabelImageId())
                .boxFrontLabelImageFile(reqVO.getBoxFrontLabelImageFile())
                .customerSideLabelImageId(reqVO.getCustomerSideLabelImageId())
                .customerSideLabelImageFile(reqVO.getCustomerSideLabelImageFile())
                .status(reqVO.getStatus())
                .build();
    }

    private List<HcVisualPrintProductItemSaveReqVO> resolveSaveProductItems(
            HcVisualPrintCustomerInfoSaveReqVO reqVO) {
        if (reqVO.getProductItems() != null && !reqVO.getProductItems().isEmpty()) {
            return reqVO.getProductItems();
        }
        HcVisualPrintProductItemSaveReqVO legacyItem = new HcVisualPrintProductItemSaveReqVO();
        legacyItem.setId(reqVO.getProductItemId());
        legacyItem.setSourceRow(reqVO.getSourceRow());
        legacyItem.setProductType(reqVO.getProductType());
        legacyItem.setSizeMm(reqVO.getSizeMm());
        legacyItem.setPadBackLabelImageId(reqVO.getPadBackLabelImageId());
        legacyItem.setPadBackLabelImageFile(reqVO.getPadBackLabelImageFile());
        legacyItem.setCleanBagLabelImageId(reqVO.getCleanBagLabelImageId());
        legacyItem.setCleanBagLabelImageFile(reqVO.getCleanBagLabelImageFile());
        legacyItem.setBoxFrontLabelImageId(reqVO.getBoxFrontLabelImageId());
        legacyItem.setBoxFrontLabelImageFile(reqVO.getBoxFrontLabelImageFile());
        legacyItem.setCustomerSideLabelImageId(reqVO.getCustomerSideLabelImageId());
        legacyItem.setCustomerSideLabelImageFile(reqVO.getCustomerSideLabelImageFile());
        legacyItem.setStatus(reqVO.getStatus());
        return List.of(legacyItem);
    }

    private void applyCustomerSummaryFromProductItems(HcVisualPrintCustomerInfoDO entity,
            List<HcVisualPrintProductItemSaveReqVO> productItems) {
        if (productItems == null || productItems.isEmpty()) {
            throw invalidParamException("至少需要维护一条产品型号尺寸");
        }
        HcVisualPrintProductItemSaveReqVO firstItem = productItems.get(0);
        if (StrUtil.isBlank(firstItem.getProductType()) || StrUtil.isBlank(firstItem.getSizeMm())) {
            throw invalidParamException("首条产品型号尺寸不完整");
        }
        entity.setProductType(StrUtil.blankToDefault(StrUtil.trim(entity.getProductType()),
                StrUtil.trim(firstItem.getProductType())));
        entity.setSizeMm(StrUtil.blankToDefault(StrUtil.trim(entity.getSizeMm()), StrUtil.trim(firstItem.getSizeMm())));
        entity.setPadBackLabelImageId(StrUtil.blankToDefault(StrUtil.trim(entity.getPadBackLabelImageId()),
                StrUtil.trim(firstItem.getPadBackLabelImageId())));
        entity.setPadBackLabelImageFile(StrUtil.blankToDefault(StrUtil.trim(entity.getPadBackLabelImageFile()),
                StrUtil.trim(firstItem.getPadBackLabelImageFile())));
        entity.setCleanBagLabelImageId(StrUtil.blankToDefault(StrUtil.trim(entity.getCleanBagLabelImageId()),
                StrUtil.trim(firstItem.getCleanBagLabelImageId())));
        entity.setCleanBagLabelImageFile(StrUtil.blankToDefault(StrUtil.trim(entity.getCleanBagLabelImageFile()),
                StrUtil.trim(firstItem.getCleanBagLabelImageFile())));
        entity.setBoxFrontLabelImageId(StrUtil.blankToDefault(StrUtil.trim(entity.getBoxFrontLabelImageId()),
                StrUtil.trim(firstItem.getBoxFrontLabelImageId())));
        entity.setBoxFrontLabelImageFile(StrUtil.blankToDefault(StrUtil.trim(entity.getBoxFrontLabelImageFile()),
                StrUtil.trim(firstItem.getBoxFrontLabelImageFile())));
        entity.setCustomerSideLabelImageId(StrUtil.blankToDefault(StrUtil.trim(entity.getCustomerSideLabelImageId()),
                StrUtil.trim(firstItem.getCustomerSideLabelImageId())));
        entity.setCustomerSideLabelImageFile(StrUtil.blankToDefault(StrUtil.trim(entity.getCustomerSideLabelImageFile()),
                StrUtil.trim(firstItem.getCustomerSideLabelImageFile())));
    }

    private void saveProductItems(Long customerInfoId, List<HcVisualPrintProductItemSaveReqVO> reqItems,
            Long tenantId, boolean deleteMissing) {
        Set<Long> keepIds = new LinkedHashSet<>();
        for (HcVisualPrintProductItemSaveReqVO reqItem : reqItems) {
            HcVisualPrintProductItemDO item = buildProductItem(customerInfoId, reqItem);
            item.setTenantId(tenantId);
            normalizeProductItem(item);
            if (item.getId() != null) {
                validateProductItemExists(customerInfoId, item.getId());
                productItemMapper.updateById(item);
            } else {
                productItemMapper.insert(item);
            }
            keepIds.add(item.getId());
            syncLabelBindingsForItem(customerInfoId, item, tenantId);
        }
        if (!deleteMissing) {
            return;
        }
        for (HcVisualPrintProductItemDO existsItem : productItemMapper.selectListByCustomerInfoId(customerInfoId)) {
            if (keepIds.contains(existsItem.getId())) {
                continue;
            }
            labelBindingMapper.physicalDeleteByProductItemId(existsItem.getId());
            designMapper.physicalDeleteByProductItemId(existsItem.getId());
            productItemMapper.deleteById(existsItem.getId());
        }
    }

    private void cleanupFlattenedCustomerRows(Long tenantId, Map<Integer, Long> masterIdBySourceRow,
            Map<Integer, Long> itemMasterIdBySourceRow, Map<Integer, Long> itemIdBySourceRow) {
        for (Map.Entry<Integer, Long> entry : itemIdBySourceRow.entrySet()) {
            Integer sourceRow = entry.getKey();
            if (sourceRow == null || masterIdBySourceRow.containsKey(sourceRow)) {
                continue;
            }
            HcVisualPrintCustomerInfoDO staleCustomer = customerInfoMapper.selectBySourceRow(tenantId, sourceRow);
            Long targetCustomerId = itemMasterIdBySourceRow.get(sourceRow);
            Long targetProductItemId = entry.getValue();
            if (staleCustomer == null || targetCustomerId == null || targetProductItemId == null
                    || staleCustomer.getId().equals(targetCustomerId)) {
                continue;
            }
            transferDesignsToProductItem(staleCustomer.getId(), targetCustomerId, targetProductItemId);
            labelBindingMapper.physicalDeleteByCustomerInfoId(staleCustomer.getId());
            productItemMapper.physicalDeleteByCustomerInfoId(staleCustomer.getId());
            customerInfoMapper.deleteById(staleCustomer.getId());
        }
    }

    private void transferDesignsToProductItem(Long staleCustomerId, Long targetCustomerId, Long targetProductItemId) {
        HcVisualPrintProductItemDO targetProductItem = productItemMapper.selectByIdAndCustomerInfoId(targetProductItemId,
                targetCustomerId);
        if (targetProductItem == null) {
            return;
        }
        for (HcVisualPrintDesignDO design : designMapper.selectListByCustomerInfoId(staleCustomerId)) {
            design.setCustomerInfoId(targetCustomerId);
            HcVisualPrintLabelBindingDO binding = ensureLabelBinding(targetProductItem, design);
            design.setProductItemId(hasBindingImage(binding) ? null : targetProductItemId);
            HcVisualPrintDesignDO exists = binding.getDesignId() == null ? null
                    : designMapper.selectById(binding.getDesignId());
            if (exists != null && !exists.getId().equals(design.getId())) {
                designMapper.deleteById(design.getId());
                continue;
            }
            designMapper.updateById(design);
            attachSharedDesignToBindings(binding, design.getId());
        }
    }

    private List<ImportCustomer> parseImportRows(MultipartFile file, String importBatchNo,
            HcVisualPrintImportRespVO respVO)
            throws IOException {
        List<ImportCustomer> customers = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                respVO.getMessages().add("Excel未包含任何Sheet");
                return customers;
            }
            ImportRowContext context = new ImportRowContext();
            ImportCustomer currentCustomer = null;
            for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row excelRow = sheet.getRow(rowIndex);
                if (excelRow == null || !hasAnyValue(excelRow, formatter)) {
                    continue;
                }
                boolean newCustomerRow = StrUtil.isNotBlank(readText(excelRow, 0, formatter))
                        || StrUtil.isNotBlank(readText(excelRow, 1, formatter));
                ImportRow row = readImportRow(excelRow, rowIndex + 1, formatter, context, importBatchNo);
                if (newCustomerRow || currentCustomer == null) {
                    if (StrUtil.isBlank(row.customer)) {
                        respVO.setSkipCount(respVO.getSkipCount() + 1);
                        respVO.getMessages().add(StrUtil.format("第 {} 行：客户为空，无法建立客户主表，已跳过", row.rowNo));
                        currentCustomer = null;
                        continue;
                    }
                    currentCustomer = new ImportCustomer(row);
                    customers.add(currentCustomer);
                }
                if (currentCustomer == null || StrUtil.isBlank(row.productType) || StrUtil.isBlank(row.sizeMm)) {
                    respVO.setSkipCount(respVO.getSkipCount() + 1);
                    respVO.getMessages().add(StrUtil.format("第 {} 行：产品类型或尺寸为空，已跳过明细", row.rowNo));
                    continue;
                }
                respVO.setTotalCount(respVO.getTotalCount() + 1);
                currentCustomer.productRows.add(row);
            }
        }
        return customers;
    }

    private ImportRow readImportRow(Row excelRow, int rowNo, DataFormatter formatter, ImportRowContext context,
            String importBatchNo) {
        ImportRow row = new ImportRow();
        row.rowNo = rowNo;
        row.sourceRow = rowNo;
        row.importBatchNo = importBatchNo;
        row.serialNo = context.next("serialNo", readText(excelRow, 0, formatter));
        row.customer = context.next("customer", readText(excelRow, 1, formatter));
        row.productType = context.next("productType", readText(excelRow, 2, formatter));
        row.sizeMm = context.next("sizeMm", readText(excelRow, 3, formatter));
        row.padBackLabelImageId = extractImageId(readRawCell(excelRow, 4, formatter));
        row.padBackLabelImageFile = resolveImageFile(row.padBackLabelImageId);
        row.cleanBagLabelImageId = extractImageId(readRawCell(excelRow, 5, formatter));
        row.cleanBagLabelImageFile = resolveImageFile(row.cleanBagLabelImageId);
        row.boxFrontLabelImageId = extractImageId(readRawCell(excelRow, 6, formatter));
        row.boxFrontLabelImageFile = resolveImageFile(row.boxFrontLabelImageId);
        row.customerSideLabelImageId = extractImageId(readRawCell(excelRow, 7, formatter));
        row.customerSideLabelImageFile = resolveImageFile(row.customerSideLabelImageId);
        row.customerSideSize = context.next("customerSideSize", readText(excelRow, 8, formatter));
        row.customerSideMethod = context.next("customerSideMethod", readText(excelRow, 9, formatter));
        row.shippingMethod = context.next("shippingMethod", readText(excelRow, 10, formatter));
        row.needPaperCoa = context.next("needPaperCoa", readText(excelRow, 11, formatter));
        row.needEcoa = context.next("needEcoa", readText(excelRow, 12, formatter));
        row.hasMark = context.next("hasMark", readText(excelRow, 13, formatter));
        row.deliveryNote = context.next("deliveryNote", readText(excelRow, 14, formatter));
        row.shipmentFilePackageMethod = context.next("shipmentFilePackageMethod", readText(excelRow, 15, formatter));
        row.customerSideTemplate = context.next("customerSideTemplate", readText(excelRow, 16, formatter));
        row.specialRemark = context.next("specialRemark", readText(excelRow, 17, formatter));
        row.rawJson = JsonUtils.toJsonString(row.toRawMap());
        return row;
    }

    private boolean hasAnyValue(Row row, DataFormatter formatter) {
        for (int i = 0; i <= 17; i++) {
            if (StrUtil.isNotBlank(readRawCell(row, i, formatter))) {
                return true;
            }
        }
        return false;
    }

    private String readText(Row row, int cellIndex, DataFormatter formatter) {
        return StrUtil.trim(readRawCell(row, cellIndex, formatter));
    }

    private String readRawCell(Row row, int cellIndex, DataFormatter formatter) {
        Cell cell = resolveMergedSourceCell(row, cellIndex);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                String formula = cell.getCellFormula();
                if (StrUtil.containsIgnoreCase(formula, "DISPIMG")) {
                    return formula;
                }
            } catch (RuntimeException ignored) {
                return "";
            }
        }
        return formatter.formatCellValue(cell);
    }

    private Cell resolveMergedSourceCell(Row row, int cellIndex) {
        if (row == null) {
            return null;
        }
        Sheet sheet = row.getSheet();
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (!region.isInRange(row.getRowNum(), cellIndex)) {
                continue;
            }
            Row firstRow = sheet.getRow(region.getFirstRow());
            return firstRow == null ? null : firstRow.getCell(region.getFirstColumn());
        }
        return row.getCell(cellIndex);
    }

    private String extractImageId(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        Matcher matcher = IMAGE_ID_PATTERN.matcher(value);
        return matcher.find() ? matcher.group(1) : StrUtil.trim(value);
    }

    private String resolveImageFile(String imageId) {
        if (StrUtil.isBlank(imageId)) {
            return "";
        }
        return IMAGE_FILE_MAP.getOrDefault(imageId, imageId + ".png");
    }

    private void normalizeCustomerInfo(HcVisualPrintCustomerInfoDO entity) {
        entity.setSerialNo(StrUtil.trim(entity.getSerialNo()));
        entity.setCustomer(StrUtil.trim(entity.getCustomer()));
        entity.setProductType(StrUtil.trim(entity.getProductType()));
        entity.setSizeMm(StrUtil.trim(entity.getSizeMm()));
        if (StrUtil.isBlank(entity.getCustomer())) {
            throw invalidParamException("客户不能为空");
        }
        entity.setPadBackLabelImageId(StrUtil.trim(entity.getPadBackLabelImageId()));
        entity.setPadBackLabelImageFile(StrUtil.trim(entity.getPadBackLabelImageFile()));
        entity.setCleanBagLabelImageId(StrUtil.trim(entity.getCleanBagLabelImageId()));
        entity.setCleanBagLabelImageFile(StrUtil.trim(entity.getCleanBagLabelImageFile()));
        entity.setBoxFrontLabelImageId(StrUtil.trim(entity.getBoxFrontLabelImageId()));
        entity.setBoxFrontLabelImageFile(StrUtil.trim(entity.getBoxFrontLabelImageFile()));
        entity.setCustomerSideLabelImageId(StrUtil.trim(entity.getCustomerSideLabelImageId()));
        entity.setCustomerSideLabelImageFile(StrUtil.trim(entity.getCustomerSideLabelImageFile()));
        entity.setCustomerSideSize(StrUtil.trim(entity.getCustomerSideSize()));
        entity.setCustomerSideMethod(StrUtil.trim(entity.getCustomerSideMethod()));
        entity.setShippingMethod(StrUtil.trim(entity.getShippingMethod()));
        entity.setNeedPaperCoa(StrUtil.trim(entity.getNeedPaperCoa()));
        entity.setNeedEcoa(StrUtil.trim(entity.getNeedEcoa()));
        entity.setHasMark(StrUtil.trim(entity.getHasMark()));
        entity.setDeliveryNote(StrUtil.trim(entity.getDeliveryNote()));
        entity.setShipmentFilePackageMethod(StrUtil.trim(entity.getShipmentFilePackageMethod()));
        entity.setCustomerSideTemplate(StrUtil.trim(entity.getCustomerSideTemplate()));
        entity.setSpecialRemark(StrUtil.trim(entity.getSpecialRemark()));
        entity.setStatus(entity.getStatus() == null ? 0 : entity.getStatus());
    }

    private void normalizeProductItem(HcVisualPrintProductItemDO entity) {
        entity.setProductType(StrUtil.trim(entity.getProductType()));
        entity.setSizeMm(StrUtil.trim(entity.getSizeMm()));
        if (StrUtil.isBlank(entity.getProductType())) {
            throw invalidParamException("产品类型不能为空");
        }
        if (StrUtil.isBlank(entity.getSizeMm())) {
            throw invalidParamException("尺寸/mm不能为空");
        }
        entity.setPadBackLabelImageId(StrUtil.trim(entity.getPadBackLabelImageId()));
        entity.setPadBackLabelImageFile(StrUtil.trim(entity.getPadBackLabelImageFile()));
        entity.setCleanBagLabelImageId(StrUtil.trim(entity.getCleanBagLabelImageId()));
        entity.setCleanBagLabelImageFile(StrUtil.trim(entity.getCleanBagLabelImageFile()));
        entity.setBoxFrontLabelImageId(StrUtil.trim(entity.getBoxFrontLabelImageId()));
        entity.setBoxFrontLabelImageFile(StrUtil.trim(entity.getBoxFrontLabelImageFile()));
        entity.setCustomerSideLabelImageId(StrUtil.trim(entity.getCustomerSideLabelImageId()));
        entity.setCustomerSideLabelImageFile(StrUtil.trim(entity.getCustomerSideLabelImageFile()));
        entity.setStatus(entity.getStatus() == null ? 0 : entity.getStatus());
    }

    private void normalizeDesign(HcVisualPrintDesignDO entity) {
        entity.setLabelKind(normalizeLabelKind(entity.getLabelKind()));
        entity.setLabelName(StrUtil.blankToDefault(StrUtil.trim(entity.getLabelName()),
                LABEL_KIND_NAME_MAP.get(entity.getLabelKind())));
        if (entity.getWidthMm() == null || BigDecimal.ZERO.compareTo(entity.getWidthMm()) >= 0) {
            throw invalidParamException("画布宽度必须大于0");
        }
        if (entity.getHeightMm() == null || BigDecimal.ZERO.compareTo(entity.getHeightMm()) >= 0) {
            throw invalidParamException("画布高度必须大于0");
        }
        if (StrUtil.isBlank(entity.getDesignJson())) {
            throw invalidParamException("设计JSON不能为空");
        }
        entity.setDpi(entity.getDpi() == null ? 300 : entity.getDpi());
        entity.setImageId(normalizeImageKey(entity.getImageId()));
        entity.setImageFile(normalizeImageKey(entity.getImageFile()));
        entity.setBtwTemplateRootDir(StrUtil.trim(entity.getBtwTemplateRootDir()));
        entity.setBtwCallFile(StrUtil.trim(entity.getBtwCallFile()));
        entity.setRemark(StrUtil.trim(entity.getRemark()));
        entity.setStatus(entity.getStatus() == null ? 0 : entity.getStatus());
    }

    private String normalizeLabelKind(String labelKind) {
        String value = StrUtil.trim(labelKind);
        if (!LABEL_KIND_NAME_MAP.containsKey(value)) {
            throw invalidParamException("标签类型不正确：{}", labelKind);
        }
        return value;
    }

    private HcVisualPrintCustomerInfoDO validateCustomerInfoExists(Long id) {
        if (id == null) {
            throw invalidParamException("客户打印信息ID不能为空");
        }
        HcVisualPrintCustomerInfoDO entity = customerInfoMapper.selectById(id);
        if (entity == null) {
            throw invalidParamException("客户打印信息不存在");
        }
        return entity;
    }

    private HcVisualPrintProductItemDO validateProductItemExists(Long customerInfoId, Long productItemId) {
        if (productItemId == null) {
            return null;
        }
        HcVisualPrintProductItemDO entity = productItemMapper.selectByIdAndCustomerInfoId(productItemId,
                customerInfoId);
        if (entity == null) {
            throw invalidParamException("产品尺寸明细不存在或不属于当前客户");
        }
        return entity;
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private HcVisualPrintFieldOptionRespVO field(String fieldKey, String fieldLabel, String excelColumn) {
        return new HcVisualPrintFieldOptionRespVO(fieldKey, fieldLabel, excelColumn);
    }

    private static Map<String, String> buildImageFileMap() {
        Map<String, String> map = new LinkedHashMap<>();
        putImageFile(map, "ID_023A38800E014219B6208BB0C43A43BF", "png");
        putImageFile(map, "ID_14B7EA2AB1E84124A26455330966566D", "png");
        putImageFile(map, "ID_1E6B1C2D56C14021A2F02D6785DC1E2F", "png");
        putImageFile(map, "ID_2BAB7DB17A80486C916778DD99A598BD", "png");
        putImageFile(map, "ID_2E1381B10A4045DD9DD61F550B57E61D", "png");
        putImageFile(map, "ID_2E14C02675C6483B9BE6B2AF3D750BEA", "png");
        putImageFile(map, "ID_3CE22EE508524717BA2B17E17F545A73", "png");
        putImageFile(map, "ID_525B824893374CB39A2F08874680F0DB", "png");
        putImageFile(map, "ID_5F490F8E9523459384F1AC17599D3233", "png");
        putImageFile(map, "ID_676D0BF104E045EE9B44C846D3A5E22D", "png");
        putImageFile(map, "ID_698B369B3C8442A7A14BDD2705A0588B", "png");
        putImageFile(map, "ID_79DEFDA095454ACBBA189AEE15D9DA27", "png");
        putImageFile(map, "ID_816287B98425497E9D21CC7C921DAA5E", "png");
        putImageFile(map, "ID_A1E06817044342BDB20EBCDC3274BCA0", "png");
        putImageFile(map, "ID_A51C9112F87249EBAFA74317F3EE97D9", "png");
        putImageFile(map, "ID_B2AD45ED492B4183A54282110EB77FA1", "png");
        putImageFile(map, "ID_C32FED4FB99E491288CC753DD9F454AB", "png");
        putImageFile(map, "ID_C3F39F4AF34E4A9A863D4F286CB9A6E7", "png");
        putImageFile(map, "ID_CB2EAAC6CF624F948FF6CEE722AB8239", "png");
        putImageFile(map, "ID_CDAA23130F4549D284DA1C72585B8AF4", "png");
        putImageFile(map, "ID_D0DD1611F04E4F2486DB28E0256A802D", "png");
        putImageFile(map, "ID_DBD42E81AE924CE5956719E83E1B079E", "png");
        putImageFile(map, "ID_E0652AD177B3481289967632409B986A", "png");
        putImageFile(map, "ID_E189F229E9E948BBB5F14B809CFA92AA", "png");
        putImageFile(map, "ID_E69FFF290AE541B08799B8EB45E46CA0", "png");
        putImageFile(map, "ID_ED9F8968265742779F996B4799361E74", "png");
        putImageFile(map, "ID_EEB659BB262C48CC86ABB849EDA5B09E", "png");
        putImageFile(map, "ID_F4CE8D322A204637927C7A462B0FCBFC", "jpeg");
        putImageFile(map, "ID_F5326A90AB484E51B36073326E56509F", "png");
        putImageFile(map, "ID_FAF5A727808C413FA270D862B8A51C2B", "png");
        putImageFile(map, "ID_FCF909C83F2446C8BB1890ED6592FF33", "png");
        return map;
    }

    private static void putImageFile(Map<String, String> map, String imageId, String extension) {
        map.put(imageId, imageId + "." + extension);
    }

    private static class ImportRowContext {
        private final Map<String, String> values = new LinkedHashMap<>();

        String next(String key, String value) {
            if (StrUtil.isNotBlank(value)) {
                values.put(key, value);
                return value;
            }
            return StrUtil.nullToEmpty(values.get(key));
        }
    }

    private static class ImportCustomer {
        private final ImportRow masterRow;
        private final List<ImportRow> productRows = new ArrayList<>();

        ImportCustomer(ImportRow masterRow) {
            this.masterRow = masterRow;
        }
    }

    private static class ImportRow {
        private int rowNo;
        private Integer sourceRow;
        private String serialNo;
        private String customer;
        private String productType;
        private String sizeMm;
        private String padBackLabelImageId;
        private String padBackLabelImageFile;
        private String cleanBagLabelImageId;
        private String cleanBagLabelImageFile;
        private String boxFrontLabelImageId;
        private String boxFrontLabelImageFile;
        private String customerSideLabelImageId;
        private String customerSideLabelImageFile;
        private String customerSideSize;
        private String customerSideMethod;
        private String shippingMethod;
        private String needPaperCoa;
        private String needEcoa;
        private String hasMark;
        private String deliveryNote;
        private String shipmentFilePackageMethod;
        private String customerSideTemplate;
        private String specialRemark;
        private String importBatchNo;
        private String rawJson;

        HcVisualPrintCustomerInfoDO toCustomerEntity() {
            return HcVisualPrintCustomerInfoDO.builder()
                    .sourceRow(sourceRow)
                    .serialNo(serialNo)
                    .customer(customer)
                    .productType(productType)
                    .sizeMm(sizeMm)
                    .padBackLabelImageId(padBackLabelImageId)
                    .padBackLabelImageFile(padBackLabelImageFile)
                    .cleanBagLabelImageId(cleanBagLabelImageId)
                    .cleanBagLabelImageFile(cleanBagLabelImageFile)
                    .boxFrontLabelImageId(boxFrontLabelImageId)
                    .boxFrontLabelImageFile(boxFrontLabelImageFile)
                    .customerSideLabelImageId(customerSideLabelImageId)
                    .customerSideLabelImageFile(customerSideLabelImageFile)
                    .customerSideSize(customerSideSize)
                    .customerSideMethod(customerSideMethod)
                    .shippingMethod(shippingMethod)
                    .needPaperCoa(needPaperCoa)
                    .needEcoa(needEcoa)
                    .hasMark(hasMark)
                    .deliveryNote(deliveryNote)
                    .shipmentFilePackageMethod(shipmentFilePackageMethod)
                    .customerSideTemplate(customerSideTemplate)
                    .specialRemark(specialRemark)
                    .status(0)
                    .importBatchNo(importBatchNo)
                    .rawJson(rawJson)
                    .build();
        }

        HcVisualPrintProductItemDO toProductItemEntity(Long customerInfoId) {
            return HcVisualPrintProductItemDO.builder()
                    .customerInfoId(customerInfoId)
                    .sourceRow(sourceRow)
                    .productType(productType)
                    .sizeMm(sizeMm)
                    .padBackLabelImageId(padBackLabelImageId)
                    .padBackLabelImageFile(padBackLabelImageFile)
                    .cleanBagLabelImageId(cleanBagLabelImageId)
                    .cleanBagLabelImageFile(cleanBagLabelImageFile)
                    .boxFrontLabelImageId(boxFrontLabelImageId)
                    .boxFrontLabelImageFile(boxFrontLabelImageFile)
                    .customerSideLabelImageId(customerSideLabelImageId)
                    .customerSideLabelImageFile(customerSideLabelImageFile)
                    .status(0)
                    .importBatchNo(importBatchNo)
                    .rawJson(rawJson)
                    .build();
        }

        Map<String, Object> toRawMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sourceRow", sourceRow);
            map.put("serialNo", serialNo);
            map.put("customer", customer);
            map.put("productType", productType);
            map.put("sizeMm", sizeMm);
            map.put("padBackLabelImageId", padBackLabelImageId);
            map.put("cleanBagLabelImageId", cleanBagLabelImageId);
            map.put("boxFrontLabelImageId", boxFrontLabelImageId);
            map.put("customerSideLabelImageId", customerSideLabelImageId);
            map.put("customerSideSize", customerSideSize);
            map.put("customerSideMethod", customerSideMethod);
            map.put("shippingMethod", shippingMethod);
            map.put("needPaperCoa", needPaperCoa);
            map.put("needEcoa", needEcoa);
            map.put("hasMark", hasMark);
            map.put("deliveryNote", deliveryNote);
            map.put("shipmentFilePackageMethod", shipmentFilePackageMethod);
            map.put("customerSideTemplate", customerSideTemplate);
            map.put("specialRemark", specialRemark);
            return map;
        }
    }

}
