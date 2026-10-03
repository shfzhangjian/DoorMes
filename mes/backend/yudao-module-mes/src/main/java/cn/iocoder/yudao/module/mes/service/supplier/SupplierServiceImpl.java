package cn.iocoder.yudao.module.mes.service.supplier;

import cn.idev.excel.FastExcelFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import cn.iocoder.yudao.module.mes.controller.admin.supplier.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.supplier.MesSupplierDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;

import cn.iocoder.yudao.module.mes.dal.mysql.supplier.MesSupplierMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierScopeMapper;
import cn.iocoder.yudao.module.mes.service.srm.SrmSupplierScopeService;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * 供应商主数据 Service 实现类
 *
 * @author 演示管理员
 */
@Service
@Validated
public class SupplierServiceImpl implements SupplierService {

    private static final int INITIAL_VERSION = 0;
    private static final int DEFAULT_SORT = 0;
    private static final String SUPPLIER_IMPORT_SHEET_NAME = "供应商名录";
    private static final Set<String> MATERIAL_GRADES = Set.of("A", "B", "C", "D");
    private static final Set<String> SUPPLIER_STATUSES = Set.of("PENDING", "QUALIFIED", "UNQUALIFIED", "FROZEN",
            "ELIMINATED", "EXITED");
    private static final DateTimeFormatter IMPORT_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("uuuu/M/d")
            .withResolverStyle(ResolverStyle.STRICT);

    @Resource
    private MesSupplierMapper mesSupplierMapper;
    @Resource
    private SrmSupplierScopeMapper supplierScopeMapper;
    @Resource
    private SrmSupplierScopeService supplierScopeService;

    @Override
    public Long createSupplier(MesSupplierSaveReqVO createReqVO) {
        // 插入
        MesSupplierDO supplier = BeanUtils.toBean(createReqVO, MesSupplierDO.class);
        applyScopeSnapshot(supplier, createReqVO.getScopeId());
        applyMaterialGradeCompatibility(supplier);
        supplierScopeService.assertSupplierEditable(supplier);
        // 新增记录由服务端统一初始化版本号，禁止使用客户端传入值。
        supplier.setVersion(INITIAL_VERSION);
        mesSupplierMapper.insert(supplier);

        // 返回
        return supplier.getId();
    }

    @Override
    public void updateSupplier(MesSupplierSaveReqVO updateReqVO) {
        // 校验存在
        MesSupplierDO original = validateSupplierExists(updateReqVO.getId());
        supplierScopeService.assertSupplierEditable(original);
        // 更新
        MesSupplierDO updateObj = BeanUtils.toBean(updateReqVO, MesSupplierDO.class);
        Long targetScopeId = updateReqVO.getScopeId() == null ? original.getScopeId() : updateReqVO.getScopeId();
        applyScopeSnapshot(updateObj, targetScopeId);
        applyMaterialGradeCompatibility(updateObj);
        supplierScopeService.assertSupplierEditable(updateObj);
        if (mesSupplierMapper.updateById(updateObj) == 0) {
            throw exception(SUPPLIER_VERSION_CONFLICT);
        }
    }

    @Override
    public void deleteSupplier(Long id) {
        // 校验存在
        MesSupplierDO supplier = validateSupplierExists(id);
        supplierScopeService.assertSupplierEditable(supplier);
        // 删除
        mesSupplierMapper.deleteById(id);
    }

    @Override
        public void deleteSupplierListByIds(List<Long> ids) {
        List<MesSupplierDO> suppliers = mesSupplierMapper.selectBatchIds(ids);
        suppliers.forEach(supplierScopeService::assertSupplierEditable);
        mesSupplierMapper.deleteByIds(ids);
        }


    private MesSupplierDO validateSupplierExists(Long id) {
        MesSupplierDO supplier = mesSupplierMapper.selectById(id);
        if (supplier == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
        return supplier;
    }

    @Override
    public MesSupplierRespVO getSupplier(Long id) {
        MesSupplierDO supplier = validateSupplierExists(id);
        supplierScopeService.assertSupplierVisible(supplier);
        return supplierScopeService.buildSupplierResp(supplier);
    }

    @Override
    public PageResult<MesSupplierRespVO> getSupplierPage(MesSupplierPageReqVO pageReqVO) {
        supplierScopeService.applySupplierScopeFilter(pageReqVO);
        PageResult<MesSupplierDO> page = mesSupplierMapper.selectPage(pageReqVO);
        return new PageResult<>(page.getList().stream().map(supplierScopeService::buildSupplierResp).toList(),
                page.getTotal());
    }

    @Override
    public List<MesSupplierImportExcelVO> buildImportTemplate() {
        // 仅输出表头及字典下拉项；导入日期由使用者按模板格式填写。
        return Collections.emptyList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesSupplierImportRespVO importSupplierExcel(MultipartFile file) throws IOException {
        if (!supplierScopeService.isSupplierSuperAdmin(SecurityFrameworkUtils.getLoginUserId())) {
            throw exception(SRM_SUPPLIER_SCOPE_ADMIN_REQUIRED);
        }
        MesSupplierImportRespVO respVO = new MesSupplierImportRespVO();
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return respVO;
        }
        if (!isExcelFile(file.getOriginalFilename())) {
            addImportFailure(respVO, "仅支持 .xls 或 .xlsx 格式的供应商名录模板");
            return respVO;
        }

        final List<MesSupplierImportExcelVO> excelRows;
        try {
            excelRows = readSupplierImportSheet(file);
        } catch (RuntimeException exception) {
            addImportFailure(respVO, "Excel 解析失败，请使用系统下载的供应商名录导入模板");
            return respVO;
        }

        List<SupplierImportRow> rows = new ArrayList<>();
        for (int index = 0; index < excelRows.size(); index++) {
            MesSupplierImportExcelVO excelRow = excelRows.get(index);
            if (isBlankImportRow(excelRow)) {
                respVO.setSkippedRows(respVO.getSkippedRows() + 1);
                continue;
            }
            int rowNo = index + 2; // 第 1 行是模板表头
            respVO.setTotalRows(respVO.getTotalRows() + 1);
            SupplierImportRow row = normalizeImportRow(excelRow, rowNo, respVO);
            validateImportRow(row, respVO);
            rows.add(row);
        }

        if (rows.isEmpty() && respVO.getFailures().isEmpty()) {
            addImportFailure(respVO, "未读取到可导入的数据行，请使用系统下载的供应商名录导入模板");
        }

        if (!respVO.getFailures().isEmpty()) {
            respVO.setFailureCount(respVO.getFailures().size());
            respVO.getMessages().add("导入校验未通过，未写入任何供应商数据");
            return respVO;
        }

        for (SupplierImportRow row : rows) {
            mesSupplierMapper.insert(row.supplier);
        }
        respVO.setSuccessCount(rows.size());
        respVO.getMessages().add(String.format("供应商名录导入完成：成功 %d 行", respVO.getSuccessCount()));
        return respVO;
    }

    /**
     * 供应商模板包含供下拉选项使用的“字典sheet”，导入时只能读取“供应商名录”业务 Sheet。
     */
    private static List<MesSupplierImportExcelVO> readSupplierImportSheet(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return FastExcelFactory.read(inputStream, MesSupplierImportExcelVO.class, null)
                    .autoCloseStream(false)
                    .sheet(SUPPLIER_IMPORT_SHEET_NAME)
                    .doReadSync();
        }
    }

    private SupplierImportRow normalizeImportRow(MesSupplierImportExcelVO excelRow, int rowNo,
                                                  MesSupplierImportRespVO respVO) {
        MesSupplierDO supplier = new MesSupplierDO();
        supplier.setUsingDepartment(trimToNull(excelRow.getUsingDepartment()));
        supplier.setSupplierName(trimToNull(excelRow.getSupplierName()));
        supplier.setSupplierCode(trimToNull(excelRow.getSupplierCode()));
        supplier.setContactPerson(trimToNull(excelRow.getContactPerson()));
        supplier.setContactPhone(trimToNull(excelRow.getContactPhone()));
        supplier.setAddress(trimToNull(excelRow.getAddress()));
        supplier.setCompanyNature(trimToNull(excelRow.getCompanyNature()));
        supplier.setOriginPlace(trimToNull(excelRow.getOriginPlace()));
        supplier.setOriginalFactoryInfo(trimToNull(excelRow.getOriginalFactoryInfo()));
        supplier.setProvidedProduct(trimToNull(excelRow.getProvidedProduct()));
        supplier.setModel(trimToNull(excelRow.getModel()));
        supplier.setMaterialCode(trimToNull(excelRow.getMaterialCode()));
        supplier.setApplicableProduct(trimToNull(excelRow.getApplicableProduct()));
        supplier.setMaterialGrade(normalizeMaterialGrade(excelRow.getMaterialGrade()));
        supplier.setMaterialCategory(supplier.getMaterialGrade());
        supplier.setStatus(normalizeSupplierStatus(excelRow.getStatus()));
        supplier.setImportDate(parseImportDate(excelRow.getImportDate(), rowNo, respVO));
        if (supplier.getScopeId() == null) {
            applyScopeSnapshot(supplier, null);
        }
        supplier.setSort(DEFAULT_SORT);
        supplier.setVersion(INITIAL_VERSION);
        return new SupplierImportRow(rowNo, supplier);
    }

    private void validateImportRow(SupplierImportRow row, MesSupplierImportRespVO respVO) {
        MesSupplierDO supplier = row.supplier;
        if (supplier.getSupplierName() == null) {
            addImportFailure(respVO, String.format("第%d行：供应商名称不能为空", row.rowNo));
        }
        if (supplier.getSupplierCode() == null) {
            addImportFailure(respVO, String.format("第%d行：供应商代码不能为空", row.rowNo));
        }
        validateMaxLength(row, respVO, supplier.getUsingDepartment(), 64, "使用部门");
        validateMaxLength(row, respVO, supplier.getAddress(), 255, "供应商地址");
        validateMaxLength(row, respVO, supplier.getCompanyNature(), 64, "企业性质");
        validateMaxLength(row, respVO, supplier.getOriginPlace(), 128, "产地");
        validateMaxLength(row, respVO, supplier.getOriginalFactoryInfo(), 255, "原厂信息");
        validateMaxLength(row, respVO, supplier.getMainProducts(), 255, "主营产品");
        validateMaxLength(row, respVO, supplier.getProvidedProduct(), 255, "提供/协作产品");
        validateMaxLength(row, respVO, supplier.getModel(), 128, "型号");
        validateMaxLength(row, respVO, supplier.getMaterialCode(), 64, "物料代码");
        validateMaxLength(row, respVO, supplier.getApplicableProduct(), 255, "适用产品");
        if (supplier.getMaterialGrade() != null && !MATERIAL_GRADES.contains(supplier.getMaterialGrade())) {
            addImportFailure(respVO, String.format("第%d行：物料等级只能为 A、B、C 或 D", row.rowNo));
        }
        if (supplier.getStatus() == null) {
            addImportFailure(respVO, String.format("第%d行：供应商状态不能为空", row.rowNo));
        } else if (!SUPPLIER_STATUSES.contains(supplier.getStatus())) {
            addImportFailure(respVO, String.format("第%d行：供应商状态只能填写考察中、合格、不合格、冻结、淘汰或退出", row.rowNo));
        }
    }

    private static void validateMaxLength(SupplierImportRow row, MesSupplierImportRespVO respVO,
                                          String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            addImportFailure(respVO, String.format("第%d行：%s不能超过%d个字符", row.rowNo, fieldName, maxLength));
        }
    }

    private static boolean isBlankImportRow(MesSupplierImportExcelVO row) {
        return row == null
                || (trimToNull(row.getUsingDepartment()) == null
                && trimToNull(row.getSupplierName()) == null
                && trimToNull(row.getSupplierCode()) == null
                && trimToNull(row.getContactPerson()) == null
                && trimToNull(row.getContactPhone()) == null
                && trimToNull(row.getAddress()) == null
                && trimToNull(row.getCompanyNature()) == null
                && trimToNull(row.getOriginPlace()) == null
                && trimToNull(row.getOriginalFactoryInfo()) == null
                && trimToNull(row.getProvidedProduct()) == null
                && trimToNull(row.getModel()) == null
                && trimToNull(row.getMaterialCode()) == null
                && trimToNull(row.getApplicableProduct()) == null
                && trimToNull(row.getImportDate()) == null
                && trimToNull(row.getMaterialGrade()) == null
                && trimToNull(row.getStatus()) == null);
    }

    private static LocalDate parseImportDate(String value, int rowNo, MesSupplierImportRespVO respVO) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            addImportFailure(respVO, String.format("第%d行：导入日期不能为空，格式应为 yyyy/M/d，例如2021/5/20", rowNo));
            return null;
        }
        try {
            return LocalDate.parse(normalized, IMPORT_DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            addImportFailure(respVO, String.format("第%d行：导入日期“%s”格式错误，应为 yyyy/M/d，例如2021/5/20",
                    rowNo, normalized));
            return null;
        }
    }

    private static String normalizeMaterialGrade(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeSupplierStatus(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if ("考察中".equals(normalized) || "PENDING".equalsIgnoreCase(normalized)) {
            return "PENDING";
        }
        if ("合格".equals(normalized) || "QUALIFIED".equalsIgnoreCase(normalized)) {
            return "QUALIFIED";
        }
        if ("不合格".equals(normalized) || "UNQUALIFIED".equalsIgnoreCase(normalized)) {
            return "UNQUALIFIED";
        }
        if ("冻结".equals(normalized) || "FROZEN".equalsIgnoreCase(normalized)) {
            return "FROZEN";
        }
        if ("淘汰".equals(normalized) || "ELIMINATED".equalsIgnoreCase(normalized)) {
            return "ELIMINATED";
        }
        if ("退出".equals(normalized) || "EXITED".equalsIgnoreCase(normalized)) {
            return "EXITED";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean isExcelFile(String fileName) {
        String normalized = trimToNull(fileName);
        if (normalized == null) {
            return false;
        }
        String lowerCaseFileName = normalized.toLowerCase(Locale.ROOT);
        return lowerCaseFileName.endsWith(".xls") || lowerCaseFileName.endsWith(".xlsx");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static void addImportFailure(MesSupplierImportRespVO respVO, String failure) {
        respVO.getFailures().add(failure);
    }

    private void applyScopeSnapshot(MesSupplierDO supplier, Long scopeId) {
        if (scopeId == null) {
            supplier.setScopeId(null);
            supplier.setScopeCode(null);
            supplier.setScopeName(null);
            return;
        }
        SrmSupplierScopeDO scope = supplierScopeMapper.selectById(scopeId);
        if (scope == null) {
            throw exception(SRM_SUPPLIER_SCOPE_NOT_EXISTS);
        }
        supplier.setScopeId(scope.getId());
        supplier.setScopeCode(scope.getScopeCode());
        supplier.setScopeName(scope.getScopeName());
    }

    private void applyMaterialGradeCompatibility(MesSupplierDO supplier) {
        if (supplier.getMaterialGrade() == null && supplier.getMaterialCategory() != null) {
            supplier.setMaterialGrade(supplier.getMaterialCategory());
        }
        if (supplier.getMaterialCategory() == null && supplier.getMaterialGrade() != null) {
            supplier.setMaterialCategory(supplier.getMaterialGrade());
        }
    }

    private static final class SupplierImportRow {

        private final int rowNo;
        private final MesSupplierDO supplier;

        private SupplierImportRow(int rowNo, MesSupplierDO supplier) {
            this.rowNo = rowNo;
            this.supplier = supplier;
        }
    }

}
