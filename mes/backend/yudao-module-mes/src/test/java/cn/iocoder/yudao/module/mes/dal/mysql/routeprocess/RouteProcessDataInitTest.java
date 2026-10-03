// 文件路径: backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/dal/mysql/routeprocess/RouteProcessDataInitTest.java
package cn.iocoder.yudao.module.mes.dal.mysql.routeprocess;

import cn.iocoder.yudao.framework.test.core.ut.BaseManualTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.material.MesMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.route.RouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessActionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.material.MesMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.process.ProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.route.RouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.routeprocess.RouteProcessActionMapper; // 修正引用
import cn.iocoder.yudao.module.mes.dal.mysql.routeprocess.RouteProcessMapper; // 修正引用
import cn.iocoder.yudao.module.mes.ut.MesTestConstants;
import jakarta.annotation.Resource;
import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * [Step 7.2 Pre] 工艺路线主数据初始化测试 (全要素版)
 * <p>
 * 核心功能：ETL 数据清洗与加载
 * 1. 初始化物料 (Material) -> 解决 mes_route.product_id 报错
 * 2. 初始化 BOM (可选)
 * 3. 初始化工序 (Process)
 * 4. 初始化工艺路线 (Route) 并关联产品
 * 5. 初始化 SOP (RouteProcessAction) 并生成 Vben Form Schema 前端配置
 */
// @Import({ ... }) // BaseManualTest 已自动扫描 Mapper，无需手动 Import
public class RouteProcessDataInitTest extends BaseManualTest {

    @Resource private RouteMapper routeMapper;
    @Resource private RouteProcessMapper routeProcessMapper;
    @Resource private RouteProcessActionMapper actionMapper;
    @Resource private ProcessMapper processMapper;
    @Resource private MesMaterialMapper materialMapper; // 必须引入
    // @Resource private BomMapper bomMapper;

    // [执行说明] 本地 CSV 数据源路径，请根据实际环境调整
    private static final String BASE_DIR = "D:\\dingding\\项目管理\\安徽禾臣\\AI\\仿真数据\\";
    private static final String CSV_SOP = BASE_DIR + "01_工序标准作业定义库_SOP_Master.csv";
    private static final String CSV_MATERIAL = BASE_DIR + "02_通用物料与图纸主数据_Material.csv";
    // private static final String CSV_BOM = BASE_DIR + "13_产品物料清单_BOM.csv";

    // [执行说明] SOP CSV 映射对象
    @Data @Builder
    static class SopCsvRow {
        String tenantId; String processName; String craftNode; String actionName;
        String desc; String trigger; String station; String device;
    }

    // [执行说明] 物料 CSV 映射对象
    @Data @Builder
    static class MaterialCsvRow {
        // 对应 CSV: 租户ID, 物料编码, 名称, 属性(Category), 计量单位, 图纸编号, 文件路径
        String tenantId;
        String code;
        String name;
        String prop;      // 对应 CSV 的 "属性"，映射到 DB 的 category
        String unit;
        String drawingId; // 对应 CSV "图纸编号"
        String filePath;  // 对应 CSV "图纸文件路径"
    }

    /**
     * 🧹 级联清空业务数据
     * [执行说明] 严格按照 "子表 -> 父表" 和 "业务数据 -> 基础数据" 的顺序清理，避免外键约束错误
     * 顺序：实绩(Action) -> 计划/工单(WorkOrder) -> 工艺定义(Route) -> 基础资料(Material/Process)
     */
    private void clearData() {
        System.out.println("🧹 开始清理测试数据...");

        // 1. 清空执行层 (Execution Layer) - 最底层，无依赖
        jdbcTemplate.execute("TRUNCATE TABLE mes_work_order_action");
        jdbcTemplate.execute("TRUNCATE TABLE mes_prod_feed");
        jdbcTemplate.execute("TRUNCATE TABLE mes_prod_check");
        jdbcTemplate.execute("TRUNCATE TABLE mes_qms_nc_record");
        jdbcTemplate.execute("TRUNCATE TABLE mes_traceability_record");

        // 2. 清空计划层 (Planning Layer)
        jdbcTemplate.execute("TRUNCATE TABLE mes_work_order_sub");
        jdbcTemplate.execute("TRUNCATE TABLE mes_work_order");
        jdbcTemplate.execute("TRUNCATE TABLE mes_plan"); // 如果有

        // 3. 清空工艺定义层 (Engineering Layer)
        jdbcTemplate.execute("TRUNCATE TABLE mes_route_process_action");
        jdbcTemplate.execute("TRUNCATE TABLE mes_route_process");
        jdbcTemplate.execute("TRUNCATE TABLE mes_route");

        // 4. 清空基础资料层 (Master Data) - 慎重选择
        // 这里我们清空 InitTest 会重新创建的表
        jdbcTemplate.execute("TRUNCATE TABLE mes_process"); // 标准工序
        jdbcTemplate.execute("TRUNCATE TABLE mes_material"); // 物料

        // 5. 可选：重置自增 ID (MySQL Truncate 默认会重置，Delete 不会)
        // 如果使用 Delete，需要: ALTER TABLE xxx AUTO_INCREMENT = 1;

        System.out.println("✅ 数据环境清理完成！");
    }

    @Test
    @DisplayName("ETL: 物料 -> 工艺 -> SOP 全流程初始化")
    public void testInitFullData() {
        // 🚨 0. 数据环境清洗 (防止污染)
        clearData();

        // 1. 初始化物料 (必须先做，否则 Route 无法关联 Product)
        // [执行说明] 读取 Material.csv 并写入 mes_material 表
        initMaterials();

        // 2. 读取 SOP CSV
        List<SopCsvRow> sopRows = readSopCsv(CSV_SOP);
        if (sopRows.isEmpty()) return;

        // [执行说明] 按租户分组处理 (T01_Film, T02_Kejie)
        Map<String, List<SopCsvRow>> tenantGroups = sopRows.stream()
                .collect(Collectors.groupingBy(SopCsvRow::getTenantId));

        tenantGroups.forEach((tenantCode, tRows) -> {
            Long tenantId = "T01_Film".equals(tenantCode) ? MesTestConstants.TENANT_ID_FILM : 1L;

            // 🚩 关键修复: 查找该租户下的“成品物料”作为工艺路线的 Product
            // T01 -> M_FILM_FIN, T02 -> M_PHONE_ASSY
            // [执行说明] 确保工艺路线挂载的成品物料存在
            String productCode = "T01_Film".equals(tenantCode) ? "M_FILM_FIN" : "M_PHONE_ASSY";
            MesMaterialDO product = ensureMaterial(tenantId, productCode, "默认成品");

            // A. 创建 Route (关联 Product)
            // [执行说明] 创建或获取主工艺路线记录 (mes_route)
            RouteDO route = createOrUpdateRoute(tenantId, tenantCode, product);
            System.out.printf("\n=== 处理工艺路线: %s (ID: %d, Product: %s) ===\n",
                    route.getName(), route.getId(), product.getName());

            // ==========================================
            // B. 处理工序 (Process) [修复版]
            // ==========================================
            // [执行说明] 将 CSV 行按工序名称分组，保持原有顺序 (LinkedHashMap)
            Map<String, List<SopCsvRow>> processGroups = tRows.stream()
                    .collect(Collectors.groupingBy(SopCsvRow::getProcessName, LinkedHashMap::new, Collectors.toList()));

            // [执行说明] 工序序号生成器，初始为 10，每次递增 10 (10, 20, 30...)
            AtomicInteger processSeq = new AtomicInteger(10);

            processGroups.forEach((procName, pRows) -> {
                // [执行说明] 1. 确保标准工序库中存在该工序 (mes_process)
                ProcessDO stdProcess = ensureStandardProcess(tenantId, procName);

                // 🚩 修复点：使用 safe 方法获取 RouteProcess，确保 ID 绝对存在
                // [执行说明] 2. 建立工艺路线与工序的关联 (mes_route_process)
                RouteProcessDO routeProcess = ensureRouteProcess(tenantId, route.getId(), stdProcess, processSeq.getAndAdd(10));

                // C. 处理动作 (Action)
                // [执行说明] 将同一工序下的行按 (节点+动作+触发时机) 聚合，因为一个动作可能对应 CSV 中多行(如多个采集参数)
                Map<String, List<SopCsvRow>> actionGroups = pRows.stream()
                        .collect(Collectors.groupingBy(row ->
                                        row.getCraftNode() + "|" + row.getActionName() + "|" + row.getTrigger(),
                                LinkedHashMap::new, Collectors.toList()));

                AtomicInteger actionSort = new AtomicInteger(1);
                actionGroups.forEach((groupKey, aRows) -> {
                    String[] keys = groupKey.split("\\|");
                    String craftNode = keys[0]; String actionName = keys[1]; String trigger = keys[2];
                    String displayName = craftNode.equals(actionName) ? actionName : craftNode + "-" + actionName;

                    // [执行说明] 构建动作配置，核心是 buildVbenFormSchema 生成前端 JSON
                    RouteProcessActionDO actionDO = RouteProcessActionDO.builder()
                            // 🚩 此时 routeProcess.getId() 绝对不为 null
                            .routeProcessId(routeProcess.getId())
                            .actionCode("ACT_" + Math.abs(displayName.hashCode()))
                            .actionName(displayName)
                            .triggerMoment(parseTrigger(trigger))
                            .sort(actionSort.getAndAdd(1))
                            .mandatory(true) // 修改为 boolean 值 (如果数据库是 bit/tinyint)
                            .actionConfig(buildVbenFormSchema(displayName, aRows))
                            .dataMapping(Map.of("target", "mes_work_order_action"))
                            .status(0)
                            .build();

                    // 同样建议加个 try-catch 或 check 逻辑，防止 actionCode 重复报错
                    try {
                        actionMapper.insert(actionDO);
                    } catch (Exception e) {
                        System.err.println("⚠️ Action 插入跳过: " + displayName);
                    }
                });
            });
        });
    }

    // =========================================================
    // 🛠️ 核心修复：确保 RouteProcess 插入并回写 ID
    // =========================================================

    private RouteProcessDO ensureRouteProcess(Long tenantId, Long routeId, ProcessDO stdProcess, int seqNo) {
        // 1. 先查是否存在 (防止重复运行报错，同时解决 ID 问题)
        // 假设同一工艺路线下，同一工序只出现一次 (如果允许多次，需要加 seqNo 查询条件)
        RouteProcessDO exist = routeProcessMapper.selectOne(
                RouteProcessDO::getRouteId, routeId,
                RouteProcessDO::getProcessCode, stdProcess.getCode()
        );

        if (exist != null) return exist;

        // 2. 插入新记录
        RouteProcessDO newRp = RouteProcessDO.builder()
                .tenantId(tenantId)
                .routeId(routeId)
                .processId(stdProcess.getId())
                .processCode(stdProcess.getCode())
                .processName(stdProcess.getName())
                .seqNo(seqNo)
                .nextProcessId(0L)
                .build();

        routeProcessMapper.insert(newRp);

        // 3. 🚨 兜底回查：如果 insert 后 ID 依然为空 (MP 常见坑点)
        // [执行说明] 应对 MyBatis-Plus 在某些主键策略下无法自动回填 ID 的情况
        if (newRp.getId() == null) {
            System.out.println("⚠️ RouteProcess ID 为空，执行回查...");
            return routeProcessMapper.selectOne(
                    RouteProcessDO::getRouteId, routeId,
                    RouteProcessDO::getProcessCode, stdProcess.getCode()
            );
        }

        return newRp;
    }


    // =========================================================
    // 🛠️ 物料初始化逻辑 (核心修复)
    // =========================================================

    private void initMaterials() {
        // 1. 读取 CSV (使用 GBK 以防中文乱码，如果文件是 UTF-8 请自行调整)
        List<MaterialCsvRow> rows = readMaterialCsv(CSV_MATERIAL);

        // 2. 兜底逻辑：如果没读到文件，手动添加默认数据，防止空指针
        if (rows.isEmpty()) {
            System.out.println("⚠️ 未读取到物料CSV，使用内置默认数据...");
            rows.add(MaterialCsvRow.builder()
                    .tenantId("T01_Film").code("M_FILM_FIN").name("防窥膜成品_盒装")
                    .prop("自制").unit("box").build());
            rows.add(MaterialCsvRow.builder()
                    .tenantId("T02_Kejie").code("M_PHONE_ASSY").name("钛灰手机中框_总成")
                    .prop("自制").unit("pcs").build());
        }

        // 3. 循环插入
        for (MaterialCsvRow row : rows) {
            Long tenantId = "T01_Film".equals(row.getTenantId()) ? MesTestConstants.TENANT_ID_FILM : 1L;
            ensureMaterialWithDetails(tenantId, row);
        }
    }

    /**
     * 用于 ensureStandardProcess 中创建 Product 时调用的简易方法
     * 必须补充 category，否则也会报同样的错
     */
    private MesMaterialDO ensureMaterial(Long tenantId, String code, String defaultName) {
        MesMaterialDO exist = materialMapper.selectOne(MesMaterialDO::getCode, code, MesMaterialDO::getTenantId, tenantId);
        if (exist != null) return exist;

        MesMaterialDO newMat = MesMaterialDO.builder()
                .tenantId(tenantId)
                .code(code)
                .name(defaultName)
                .unit("pcs")
                .category("1") // 🚨 修复点：给一个默认分类 (如 '1' 代表默认或自制)，防止报错
                .status(1)
                .build();
        materialMapper.insert(newMat);
        return newMat;
    }

    /**
     * 根据 CSV 行数据创建物料
     * [执行说明] 幂等性设计：先查后插，存在则跳过或更新
     */
    private void ensureMaterialWithDetails(Long tenantId, MaterialCsvRow row) {
        MesMaterialDO exist = materialMapper.selectOne(MesMaterialDO::getCode, row.getCode(), MesMaterialDO::getTenantId, tenantId);

        // 转换 CSV 中的 "属性" 为数据库存储的值
        // 如果数据库 category 是字典值(String)，直接存；如果是 ID(Long)，需要在这里做 switch 转换
        String categoryVal = row.getProp();
        // 示例转换逻辑（如果数据库存的是字典值 1=外购, 2=自制，请解开下面注释）
        // String categoryVal = "外购".equals(row.getProp()) ? "1" : "2";

        if (exist == null) {
            MesMaterialDO newMat = MesMaterialDO.builder()
                    .tenantId(tenantId)
                    .code(row.getCode())
                    .name(row.getName())
                    .unit(row.getUnit())
                    .category(categoryVal) // 🚨 修复点：赋值 category
                    // 将 CSV 的图纸编号存入 specification 字段 (或者你有专门的 drawing_code 字段)
                    .spec(row.getDrawingId())
                    .status(1)
                    .build();
            materialMapper.insert(newMat);
            System.out.println("✅ 成功插入物料: " + row.getName());
        } else {
            // 可选：如果已存在，更新分类，确保数据一致
            // exist.setCategory(categoryVal);
            // materialMapper.updateById(exist);
        }
    }

    private List<MaterialCsvRow> readMaterialCsv(String filePath) {
        List<MaterialCsvRow> result = new ArrayList<>();
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.err.println("❌ 物料文件不存在: " + filePath);
            return result;
        }
        try {
            // 🚨 注意：CSV含中文，建议使用 GBK 或 UTF-8 (取决于你的文件实际编码)
            // 如果你的 CSV 是 Excel 导出的，通常是 GBK；如果是编辑器写的，可能是 UTF-8
            List<String> lines = Files.readAllLines(path, Charset.forName("UTF-8")); // 根据实际情况改为 GBK

            // [执行说明] 从第2行开始读取，跳过表头 (i=1)
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(",");
                // CSV 结构: TenantID, MaterialID, Name, Prop, Unit, DrawingID, FilePath
                if (cols.length < 5) continue; // 至少要有前5列

                result.add(MaterialCsvRow.builder()
                        .tenantId(cols[0].trim())
                        .code(cols[1].trim())
                        .name(cols[2].trim())
                        .prop(cols[3].trim()) // 属性 -> Category
                        .unit(cols[4].trim())
                        .drawingId(cols.length > 5 ? cols[5].trim() : "-") // 图纸编号
                        .filePath(cols.length > 6 ? cols[6].trim() : "")   // 文件路径
                        .build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // =========================================================
    // 🛠️ 核心辅助方法
    // =========================================================

    private List<SopCsvRow> readSopCsv(String filePath) {
        List<SopCsvRow> result = new ArrayList<>();
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return result;
        try {
            List<String> lines = Files.readAllLines(path, Charset.forName("UTF8"));
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(",");
                if (cols.length < 5) continue;
                result.add(SopCsvRow.builder().tenantId(cols[0].trim()).processName(cols[1].trim()).craftNode(cols[2].trim()).actionName(cols[3].trim()).desc(cols[4].trim()).trigger(cols.length > 5 ? cols[5].trim() : "作业中").build());
            }
        } catch (Exception e) { e.printStackTrace(); }
        return result;
    }

    /**
     * 1. 确保标准工序存在 (修复 ID 回写失败问题)
     */
    private ProcessDO ensureStandardProcess(Long tenantId, String processName) {
        // 1. 先查是否存在
        ProcessDO exist = processMapper.selectOne(ProcessDO::getName, processName, ProcessDO::getTenantId, tenantId);
        if (exist != null) {
            return exist;
        }

        // 2. 不存在则创建
        // 生成一个唯一 Code，用于后续可能的反查
        String code = "PROC_" + Math.abs(processName.hashCode());

        ProcessDO newProc = ProcessDO.builder()
                .tenantId(tenantId)
                .code(code)
                .name(processName)
                .workshopId(0L)
                .processType("WORKSHOP_OP")
                .status(1)
                .build();

        processMapper.insert(newProc);

        // 3. 🚨 关键修复：如果不回写 ID，手动查回来
        if (newProc.getId() == null) {
            System.out.println("⚠️ Process 插入后 ID 为空，尝试回查: " + processName);
            ProcessDO saved = processMapper.selectOne(ProcessDO::getCode, code, ProcessDO::getTenantId, tenantId);
            if (saved == null) {
                throw new RuntimeException("❌ 严重错误：工序 " + processName + " 插入失败或无法获取 ID！");
            }
            return saved;
        }

        return newProc;
    }

    /**
     * 3. 构建 Vben Form Schema (根据组件策略)
     * [执行说明] 核心前端适配逻辑：
     * 将 CSV 中的描述行转换为 Vben Admin 识别的 Form Schema JSON
     * - "扫码/扫描" -> Input (带扫描图标)
     * - "打印" -> Button
     * - 数值(温度/厚度等) -> InputNumber (提取单位)
     * - 其他 -> 普通 Input
     */
    private Map<String, Object> buildVbenFormSchema(String groupTitle, List<SopCsvRow> rows) {
        List<Map<String, Object>> schemas = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            SopCsvRow row = rows.get(i);
            String label = row.getDesc();
            String fieldKey = "f_" + Math.abs((groupTitle + label).hashCode());

            Map<String, Object> field = new HashMap<>();
            field.put("field", fieldKey);
            field.put("label", label);
            field.put("colProps", Map.of("span", 24)); // 默认占一行

            // 🤖 策略匹配
            if (label.contains("扫码") || label.contains("扫描")) {
                field.put("component", "Input");
                field.put("componentProps", Map.of("placeholder", "请扫码", "prefixIcon", "scan"));
                field.put("rules", List.of(Map.of("required", true, "message", "必须扫码")));
            }
            else if (label.contains("打印")) {
                field.put("component", "Button");
                field.put("label", "操作");
                field.put("renderComponentContent", label);
            }
            else if (isNumericField(label)) {
                field.put("component", "InputNumber");
                String unit = extractUnit(label);
                if (unit != null) {
                    field.put("componentProps", Map.of("addonAfter", unit));
                }
            }
            else {
                // 默认文本输入
                field.put("component", "Input");
            }
            schemas.add(field);
        }

        return Map.of(
                "component", "Form",
                "labelWidth", 150,
                "schemas", schemas
        );
    }

    // 判断是否为数值型字段
    private boolean isNumericField(String label) {
        return label.contains("温度") || label.contains("湿度") ||
                label.contains("厚度") || label.contains("间隙") ||
                label.contains("浓度") || label.contains("速度") ||
                label.contains("(") || label.contains("（");
    }

    private String extractUnit(String text) {
        int start = text.lastIndexOf("(");
        int end = text.lastIndexOf(")");
        if (start == -1) start = text.lastIndexOf("（");
        if (end == -1) end = text.lastIndexOf("）");

        if (start != -1 && end != -1 && end > start) {
            return text.substring(start + 1, end);
        }
        return null;
    }

    private String parseTrigger(String text) {
        if (text == null) return "DURING_PROCESS";
        if (text.contains("前")) return "PRE_CHECK";
        if (text.contains("后")) return "POST_CHECK";
        return "DURING_PROCESS";
    }

    private RouteDO createOrUpdateRoute(Long tenantId, String tenantCode, MesMaterialDO product) {
        // 1. 确定唯一的业务标识 (Code)
        String routeCode = "RT_" + tenantCode.split("_")[1].toUpperCase();

        // 2. 🔍 关键修复：先查询数据库是否存在，存在则直接返回（防止 id 为空 或 重复插入）
        RouteDO exist = routeMapper.selectOne(RouteDO::getCode, routeCode, RouteDO::getTenantId, tenantId);
        if (exist != null) {
            System.out.println("🔄 工艺路线已存在，直接复用 ID: " + exist.getId());
            return exist;
        }

        // 3. 不存在则创建
        RouteDO route = RouteDO.builder()
                .tenantId(tenantId)
                .code(routeCode)
                .name(tenantCode + " 标准工艺仿真")
                .version("V1.0")
                .status(1)
                .productId(product.getId())
                .productCode(product.getCode())
                .productName(product.getName())
                // .processType("ROUTE") // 如果有工艺类型字段，建议补上
                .build();

        routeMapper.insert(route);

        // 4. 🚨 兜底修复：如果 insert 后 MP 没有回写 ID (通常是因为 Entity 没加 @TableId(type = AUTO))
        // 我们手动再查一次，确保 return 的对象 id 不为 null
        if (route.getId() == null) {
            System.err.println("⚠️ 警告：Insert 后 Route ID 仍为空，尝试重新查询...");
            route = routeMapper.selectOne(RouteDO::getCode, routeCode, RouteDO::getTenantId, tenantId);

            if (route == null) {
                // 如果还查不到，说明 insert 失败或者事务隔离级别问题，抛出异常阻断后续报错
                throw new RuntimeException("❌ 严重错误：无法获取工艺路线 ID，请检查 mes_route 表是否有自增主键，或 RouteDO 是否有 @TableId 注解！");
            }
        }

        System.out.println("✅ 创建工艺路线成功 ID: " + route.getId());
        return route;
    }

    private String generateCode(String name) {
        return "ACT_" + Math.abs(name.hashCode());
    }

    private Map<String, Object> buildDataMapping(List<SopCsvRow> rows) {
        return Map.of("target", "mes_work_order_action", "strategy", "JSON_MERGE");
    }
}
