package cn.iocoder.yudao.module.mes.dal.mysql.supplier;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.iocoder.yudao.framework.test.core.ut.BaseManualTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.supplier.MesSupplierDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 供应商 Mapper 单元测试
 * 对应表: mes_supplier
 */
@Import(MesSupplierMapper.class)
public class MesSupplierMapperTest extends BaseManualTest {

    @Resource
    private MesSupplierMapper mesSupplierMapper;

    @BeforeEach
    public void clean() {
        // Clean up data that might conflict (assuming codes start with SUP)
        //jdbcTemplate.update("DELETE FROM mes_supplier WHERE supplier_code LIKE 'SUP%'");
    }

    @Test
    @DisplayName("从本地绝对路径导入供应商CSV数据并验证")
    public void testInsertFromLocalCsv_Persistence_Check() {
        // 1. Define local absolute path
        String filePath = "D:\\dingding\\项目管理\\安徽禾臣\\AI\\仿真数据\\12_供应商主数据_Supplier.csv";

        if (!FileUtil.exist(filePath)) {
            System.err.println("错误：文件不存在，请检查路径：" + filePath);
            return;
        }

        // 2. Read CSV file
        CsvReader reader = CsvUtil.getReader();
        // Use StandardCharsets.UTF_8
        CsvData data = reader.read(FileUtil.file(filePath), StandardCharsets.UTF_8);

        List<CsvRow> rows = data.getRows();

        // Remove header row based on the actual first column name
        if (!rows.isEmpty() && rows.get(0).get(0).contains("租户ID")) {
            rows.remove(0);
        }

        assertFalse(rows.isEmpty(), "CSV 文件内容为空，无法进行测试");

        System.out.println("开始导入数据，共 " + rows.size() + " 条...");

        int successCount = 0;
        for (CsvRow row : rows) {
            try {
                MesSupplierDO supplier = buildSupplierFromCsvRow(row);

                // Insert
                mesSupplierMapper.insert(supplier);

                // Track for cleanup
                track("mes_supplier", supplier.getId());

                // 3. Verify
                MesSupplierDO dbRecord = mesSupplierMapper.selectById(supplier.getId());
                assertNotNull(dbRecord, "数据插入后无法查询到: " + supplier.getSupplierCode());

                // Assertions
                assertEquals(supplier.getSupplierName(), dbRecord.getSupplierName(), "供应商名称不一致");
                // Verify tenant ID is correctly parsed from CSV (index 0)
                assertEquals(supplier.getTenantId(), dbRecord.getTenantId(), "租户ID不一致");

                successCount++;
            } catch (Exception e) {
                System.err.println("导入失败行: " + row.getRawList());
                e.printStackTrace();
            }
        }

        System.out.println("测试结束，成功导入验证: " + successCount + " 条数据。");
    }

    /**
     * Map CSV row to DO object
     * CSV Structure based on user input:
     * 0: 租户ID_TenantID
     * 1: 供应商编码_SupplierID
     * 2: 名称_Name
     * 3: 订单类型_Type (Stored in remark)
     * 4: 交期天数_LeadTime (Stored in remark)
     */
    private MesSupplierDO buildSupplierFromCsvRow(CsvRow row) {
        // Parse Tenant ID safely
        Long tenantId = 1L; // Default
        try {
            String tenantStr = row.get(0);
            if (tenantStr != null && !tenantStr.isEmpty()) {
                tenantId = Long.parseLong(tenantStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid Tenant ID format: " + row.get(0));
        }

        // Construct Remark from Type and LeadTime
        String remark = String.format("类型:%s, 交期:%s天", row.get(3), row.get(4));

        return MesSupplierDO.builder()
                .tenantId(tenantId)           // Index 0
                .supplierCode(row.get(1))     // Index 1
                .supplierName(row.get(2))     // Index 2
                .shortName(row.get(2))        // Use Name as Short Name by default
                .status("1")                  // Default Active
                .remark(remark)               // Combine Index 3 & 4
                .sort(0)
                .version(0)
                .build();
    }
}
