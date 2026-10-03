package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeSaveItemReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMaterialMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class HcFinishedPackagingShippingNoticeExcelImportTest {

    private static final Logger log = LoggerFactory.getLogger(HcFinishedPackagingShippingNoticeExcelImportTest.class);

    private static final String EXCEL_PATH_PROPERTY = "shipping.notice.excel.path";
    private static final String DB_URL_PROPERTY = "shipping.notice.db.url";
    private static final String DB_USER_PROPERTY = "shipping.notice.db.user";
    private static final String DB_PASSWORD_PROPERTY = "shipping.notice.db.password";

    @InjectMocks
    private HcFinishedPackagingServiceImpl service;

    @Mock
    private HcProductModelMaterialMapper productModelMaterialMapper;

    @Test
    void shouldParseProvidedExcelAndInsertEveryDetailIntoRealSchemaThenRollback() throws Exception {
        Path excelPath = requiredPathProperty(EXCEL_PATH_PROPERTY);
        List<?> sheets = parseExcel(excelPath);

        assertEquals(2, sheets.size(), "样例Excel应解析出2张发货需求单");
        verifySheet(sheets.get(0), "DTP0100(62260516002)", "DTP0100", "10190083",
                "62260516002", LocalDate.of(2026, 5, 16), LocalDate.of(2027, 3, 15),
                10, "C12P0200", "C26D001AP", "001", "010");
        verifySheet(sheets.get(1), "DTP0100(62260516003)", "DTP0100", "10190082",
                "62260516003", LocalDate.of(2026, 5, 16), LocalDate.of(2026, 3, 15),
                5, "C12P0300", "C26D001AP", "001", "005");

        String dbUrl = requiredProperty(DB_URL_PROPERTY);
        String dbUser = requiredProperty(DB_USER_PROPERTY);
        String dbPassword = System.getProperty(DB_PASSWORD_PROPERTY, "");
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            connection.setAutoCommit(false);
            try {
                Map<String, Integer> noticeColumnLengths = loadColumnLengths(connection, "mes_inv_fg_shipping_notice");
                Map<String, Integer> itemColumnLengths = loadColumnLengths(connection, "mes_inv_fg_shipping_notice_item");
                Map<String, Integer> attachmentColumnLengths = loadColumnLengths(connection, "mes_inv_fg_shipping_notice_attachment");
                Map<Object, Long> noticeIds = insertParsedNotices(connection, sheets, noticeColumnLengths);
                int insertedItems = insertParsedDetails(connection, sheets, noticeIds, itemColumnLengths);
                int insertedAttachments = insertParsedAttachments(connection, excelPath, sheets, noticeIds, attachmentColumnLengths);
                assertEquals(2, noticeIds.size(), "数据库事务内应成功写入2张主单");
                assertEquals(15, insertedItems, "数据库事务内应成功写入15条解析明细");
                assertEquals(2, insertedAttachments, "数据库事务内应成功写入2条Excel附件");
                log.info("数据库主从表实写验证通过：mes_inv_fg_shipping_notice成功INSERT {}条，"
                        + "mes_inv_fg_shipping_notice_item成功INSERT {}条，"
                        + "mes_inv_fg_shipping_notice_attachment成功INSERT {}条，随后执行ROLLBACK，不保留测试数据",
                        noticeIds.size(), insertedItems, insertedAttachments);
            } finally {
                connection.rollback();
                log.info("测试事务已回滚，数据库未保留Excel解析测试数据");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<?> parseExcel(Path excelPath) throws Exception {
        assertTrue(Files.isRegularFile(excelPath), "Excel文件不存在：" + excelPath);
        byte[] bytes = Files.readAllBytes(excelPath);
        MultipartFile file = new MockMultipartFile("file", excelPath.getFileName().toString(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
        Method method = HcFinishedPackagingServiceImpl.class.getDeclaredMethod("parseShippingNoticeExcel", MultipartFile.class);
        method.setAccessible(true);
        List<?> sheets = (List<?>) method.invoke(service, file);
        log.info("Excel解析完成：文件={}，Sheet数量={}", excelPath, sheets.size());
        return sheets;
    }

    private void verifySheet(Object sheet, String expectedSheetName, String expectedProductModel,
                             String expectedProductCode, String expectedBatchNo,
                             LocalDate expectedProductionDate, LocalDate expectedExpiryDate,
                             int expectedItemCount, String expectedInternalModel,
                             String expectedInternalItem, String expectedFirstPackageSlice,
                             String expectedLastPackageSlice) throws Exception {
        String sheetName = field(sheet, "sheetName");
        String productModel = field(sheet, "externalProductModel");
        String productCode = field(sheet, "externalProductCode");
        String batchNo = field(sheet, "requiredBatchNo");
        LocalDate productionDate = field(sheet, "requiredProductionDate");
        LocalDate expiryDate = field(sheet, "requiredExpiryDate");
        Integer sheetIndex = field(sheet, "sheetIndex");
        Integer sheetTotal = field(sheet, "sheetTotal");
        List<ShippingNoticeSaveItemReqVO> items = field(sheet, "items");

        assertEquals(expectedSheetName, sheetName.trim());
        assertEquals(expectedProductModel, productModel);
        assertEquals(expectedProductCode, productCode);
        assertEquals(expectedBatchNo, batchNo);
        assertEquals(expectedProductionDate, productionDate);
        assertEquals(expectedExpiryDate, expiryDate);
        assertTrue(sheetIndex != null && sheetIndex > 0, "Sheet序号应写入导入上下文");
        assertEquals(2, sheetTotal, "样例Excel有效Sheet总数应为2");
        assertEquals(expectedItemCount, items.size(), "页脚确认/备注/签字行不得进入发货明细");
        assertEquals(expectedInternalModel, items.get(0).getInternalModelCode());
        assertEquals(expectedInternalItem, items.get(0).getInternalItemCode());
        assertEquals(expectedFirstPackageSlice, items.get(0).getPackageSliceNo());
        assertEquals(expectedLastPackageSlice, items.get(items.size() - 1).getPackageSliceNo());

        log.info("Sheet解析：sheet={}，产品型号={}，产品编码={}，批号={}，生产日期={}，失效日期={}，明细数={}",
                sheetName, productModel, productCode, batchNo, productionDate, expiryDate, items.size());
        for (int i = 0; i < items.size(); i++) {
            ShippingNoticeSaveItemReqVO item = items.get(i);
            log.info("明细解析：sheet={}，序号={}，internal_model_code={}，internal_item_code={}，batch_no={}，"
                            + "package_slice_no={}，model_code={}，customer_slice_batch_no={}，customer_model_code={}",
                    sheetName, i + 1, item.getInternalModelCode(), item.getInternalItemCode(), item.getBatchNo(),
                    item.getPackageSliceNo(), item.getModelCode(), item.getCustomerSliceBatchNo(), item.getCustomerModelCode());
        }
    }

    private Map<Object, Long> insertParsedNotices(Connection connection, List<?> sheets,
                                                   Map<String, Integer> columnLengths) throws Exception {
        String sql = "INSERT INTO mes_inv_fg_shipping_notice ("
                + "id, notice_no, customer_name, product_type, model_code, external_product_model, "
                + "external_product_code, external_product_info, required_ship_qty, required_slice_range, "
                + "required_batch_no, required_production_date, required_expiry_date, packing_requirement, "
                + "notice_qty, locked_qty, notice_status, recorder_name, recorder_time, remark, "
                + "creator, updater, deleted, tenant_id"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        long temporaryNoticeId = -System.currentTimeMillis();
        Map<Object, Long> noticeIds = new IdentityHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < sheets.size(); index++) {
                Object sheet = sheets.get(index);
                long noticeId = temporaryNoticeId - index;
                String noticeNo = "TEST-EXCEL-MAIN-" + Math.abs(noticeId);
                String customerName = field(sheet, "customerName");
                String externalProductModel = field(sheet, "externalProductModel");
                String externalProductCode = field(sheet, "externalProductCode");
                String externalProductInfo = field(sheet, "externalProductInfo");
                Integer requiredShipQty = field(sheet, "requiredShipQty");
                String requiredSliceRange = field(sheet, "requiredSliceRange");
                String requiredBatchNo = field(sheet, "requiredBatchNo");
                LocalDate requiredProductionDate = field(sheet, "requiredProductionDate");
                LocalDate requiredExpiryDate = field(sheet, "requiredExpiryDate");
                String packingRequirement = field(sheet, "packingRequirement");

                assertFits(columnLengths, "notice_no", noticeNo);
                assertFits(columnLengths, "customer_name", customerName);
                assertFits(columnLengths, "model_code", externalProductModel);
                assertFits(columnLengths, "external_product_model", externalProductModel);
                assertFits(columnLengths, "external_product_code", externalProductCode);
                assertFits(columnLengths, "external_product_info", externalProductInfo);
                assertFits(columnLengths, "required_slice_range", requiredSliceRange);
                assertFits(columnLengths, "required_batch_no", requiredBatchNo);
                assertFits(columnLengths, "packing_requirement", packingRequirement);

                statement.setLong(1, noticeId);
                statement.setString(2, noticeNo);
                statement.setString(3, customerName);
                statement.setString(4, "MASS");
                statement.setString(5, externalProductModel);
                statement.setString(6, externalProductModel);
                statement.setString(7, externalProductCode);
                statement.setString(8, externalProductInfo);
                statement.setInt(9, requiredShipQty);
                statement.setString(10, requiredSliceRange);
                statement.setString(11, requiredBatchNo);
                statement.setDate(12, java.sql.Date.valueOf(requiredProductionDate));
                statement.setDate(13, java.sql.Date.valueOf(requiredExpiryDate));
                statement.setString(14, packingRequirement);
                statement.setInt(15, requiredShipQty);
                statement.setInt(16, 0);
                statement.setString(17, "DRAFT");
                statement.setString(18, "excel-import-test");
                statement.setTimestamp(19, new Timestamp(System.currentTimeMillis()));
                statement.setString(20, "Excel后端解析测试");
                statement.setString(21, "excel-import-test");
                statement.setString(22, "excel-import-test");
                statement.setBoolean(23, false);
                statement.setLong(24, 1L);
                assertEquals(1, statement.executeUpdate(), "主表写入失败：" + noticeNo);
                noticeIds.put(sheet, noticeId);
                log.info("主表实写：notice_id={}，notice_no={}，customer_name={}，model_code={}，"
                                + "external_product_code={}，required_batch_no={}，notice_qty={}",
                        noticeId, noticeNo, customerName, externalProductModel, externalProductCode,
                        requiredBatchNo, requiredShipQty);
            }
        }
        return noticeIds;
    }

    private int insertParsedAttachments(Connection connection, Path excelPath, List<?> sheets,
                                        Map<Object, Long> noticeIds, Map<String, Integer> columnLengths)
            throws Exception {
        String sql = "INSERT INTO mes_inv_fg_shipping_notice_attachment ("
                + "notice_id, notice_no, attachment_name, attachment_url, attachment_type, source_file_name, "
                + "source_sheet_name, source_sheet_index, source_sheet_total, file_size, remark, "
                + "creator, updater, deleted, tenant_id"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String fileName = excelPath.getFileName().toString();
        String fileUrl = "https://example.invalid/" + fileName;
        long fileSize = Files.size(excelPath);
        int inserted = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Object sheet : sheets) {
                String sheetName = field(sheet, "sheetName");
                Integer sheetIndex = field(sheet, "sheetIndex");
                Integer sheetTotal = field(sheet, "sheetTotal");
                Long temporaryNoticeId = noticeIds.get(sheet);
                assertNotNull(temporaryNoticeId, "未找到Sheet对应的测试主表ID：" + sheetName);
                String noticeNo = "TEST-EXCEL-MAIN-" + Math.abs(temporaryNoticeId);

                assertFits(columnLengths, "notice_no", noticeNo);
                assertFits(columnLengths, "attachment_name", fileName);
                assertFits(columnLengths, "attachment_url", fileUrl);
                assertFits(columnLengths, "attachment_type", "EXCEL_IMPORT");
                assertFits(columnLengths, "source_file_name", fileName);
                assertFits(columnLengths, "source_sheet_name", sheetName);
                assertFits(columnLengths, "remark", "Excel导入原始文件");

                statement.setLong(1, temporaryNoticeId);
                statement.setString(2, noticeNo);
                statement.setString(3, fileName);
                statement.setString(4, fileUrl);
                statement.setString(5, "EXCEL_IMPORT");
                statement.setString(6, fileName);
                statement.setString(7, sheetName);
                statement.setInt(8, sheetIndex);
                statement.setInt(9, sheetTotal);
                statement.setLong(10, fileSize);
                statement.setString(11, "Excel导入原始文件");
                statement.setString(12, "excel-import-test");
                statement.setString(13, "excel-import-test");
                statement.setBoolean(14, false);
                statement.setLong(15, 1L);
                int affected = statement.executeUpdate();
                assertEquals(1, affected, "附件表写入失败：sheet=" + sheetName);
                inserted += affected;
                log.info("附件表实写：notice_id={}，notice_no={}，source_file_name={}，source_sheet={}，sheet_index={}/{}",
                        temporaryNoticeId, noticeNo, fileName, sheetName, sheetIndex, sheetTotal);
            }
        }
        return inserted;
    }

    private int insertParsedDetails(Connection connection, List<?> sheets, Map<Object, Long> noticeIds,
                                    Map<String, Integer> columnLengths)
            throws Exception {
        String sql = "INSERT INTO mes_inv_fg_shipping_notice_item ("
                + "notice_id, notice_no, slice_batch_no, batch_no, internal_model_code, internal_item_code, "
                + "customer_product_batch_no, package_slice_no, material_code, material_name, model_code, product_size, "
                + "locked_qty, lock_status, customer_slice_batch_no, customer_model_code, creator, updater, deleted, tenant_id"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int inserted = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Object sheet : sheets) {
                String sheetName = field(sheet, "sheetName");
                Long temporaryNoticeId = noticeIds.get(sheet);
                assertNotNull(temporaryNoticeId, "未找到Sheet对应的测试主表ID：" + sheetName);
                List<ShippingNoticeSaveItemReqVO> items = field(sheet, "items");
                for (int i = 0; i < items.size(); i++) {
                    ShippingNoticeSaveItemReqVO item = items.get(i);
                    String noticeNo = "TEST-EXCEL-MAIN-" + Math.abs(temporaryNoticeId);
                    assertFits(columnLengths, "notice_no", noticeNo);
                    assertFits(columnLengths, "slice_batch_no", item.getPackageSliceNo());
                    assertFits(columnLengths, "batch_no", item.getBatchNo());
                    assertFits(columnLengths, "internal_model_code", item.getInternalModelCode());
                    assertFits(columnLengths, "internal_item_code", item.getInternalItemCode());
                    assertFits(columnLengths, "customer_product_batch_no", item.getCustomerProductBatchNo());
                    assertFits(columnLengths, "package_slice_no", item.getPackageSliceNo());
                    assertFits(columnLengths, "material_code", item.getMaterialCode());
                    assertFits(columnLengths, "material_name", item.getMaterialName());
                    assertFits(columnLengths, "model_code", item.getModelCode());
                    assertFits(columnLengths, "customer_slice_batch_no", item.getCustomerSliceBatchNo());
                    assertFits(columnLengths, "customer_model_code", item.getCustomerModelCode());

                    statement.setLong(1, temporaryNoticeId);
                    statement.setString(2, noticeNo);
                    statement.setString(3, item.getPackageSliceNo());
                    statement.setString(4, item.getBatchNo());
                    statement.setString(5, item.getInternalModelCode());
                    statement.setString(6, item.getInternalItemCode());
                    statement.setString(7, item.getCustomerProductBatchNo());
                    statement.setString(8, item.getPackageSliceNo());
                    statement.setString(9, item.getMaterialCode());
                    statement.setString(10, item.getMaterialName());
                    statement.setString(11, item.getModelCode());
                    statement.setString(12, null);
                    statement.setInt(13, 1);
                    statement.setString(14, "DRAFT");
                    statement.setString(15, item.getCustomerSliceBatchNo());
                    statement.setString(16, item.getCustomerModelCode());
                    statement.setString(17, "excel-import-test");
                    statement.setString(18, "excel-import-test");
                    statement.setBoolean(19, false);
                    statement.setLong(20, 1L);
                    int affected = statement.executeUpdate();
                    assertEquals(1, affected, "数据库写入失败：sheet=" + sheetName + "，明细=" + (i + 1));
                    inserted += affected;
                }
            }
        }
        return inserted;
    }

    private Map<String, Integer> loadColumnLengths(Connection connection, String tableName) throws SQLException {
        Map<String, Integer> lengths = new HashMap<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, tableName, null)) {
            while (columns.next()) {
                lengths.put(columns.getString("COLUMN_NAME").toLowerCase(), columns.getInt("COLUMN_SIZE"));
            }
        }
        assertFalse(lengths.isEmpty(), "未读取到表结构：" + tableName);
        log.info("数据库字段长度：notice_no={}，batch_no={}，internal_model_code={}，internal_item_code={}，"
                        + "package_slice_no={}，model_code={}，customer_slice_batch_no={}，customer_model_code={}",
                lengths.get("notice_no"), lengths.get("batch_no"), lengths.get("internal_model_code"),
                lengths.get("internal_item_code"), lengths.get("package_slice_no"), lengths.get("model_code"),
                lengths.get("customer_slice_batch_no"), lengths.get("customer_model_code"));
        return lengths;
    }

    private void assertFits(Map<String, Integer> columnLengths, String columnName, String value) {
        Integer maxLength = columnLengths.get(columnName);
        assertNotNull(maxLength, "数据库不存在字段：" + columnName);
        if (value != null) {
            assertTrue(value.length() <= maxLength,
                    () -> columnName + "长度超限：实际=" + value.length() + "，允许=" + maxLength + "，值=" + value);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private Path requiredPathProperty(String propertyName) {
        return Path.of(requiredProperty(propertyName));
    }

    private String requiredProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        assertNotNull(value, "请通过-D" + propertyName + "传入测试参数");
        assertFalse(value.isBlank(), "测试参数不能为空：" + propertyName);
        return value;
    }

}
