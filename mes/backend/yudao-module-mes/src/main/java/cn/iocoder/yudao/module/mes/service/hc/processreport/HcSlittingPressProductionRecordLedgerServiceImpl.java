package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slittingpress.HcSlittingPressProductionRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slittingpress.HcSlittingPressProductionLedgerMapper;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
public class HcSlittingPressProductionRecordLedgerServiceImpl implements HcSlittingPressProductionRecordLedgerService {
    private static final String WAIT = "WAIT_CONFIRM";
    private static final String CONFIRMED = "CONFIRMED";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Resource private HcSlittingPressProductionLedgerMapper mapper;
    @Resource private HcSlittingPressProductionRecordService sourceService;

    @Override @Transactional(rollbackFor = Exception.class)
    public Long create(HcSlittingPressProductionRecordSaveReqVO req) {
        normalize(req); validateBiz(req, null);
        HcSlittingPressProductionRecordDO row = BeanUtils.toBean(req, HcSlittingPressProductionRecordDO.class);
        row.setStatus(WAIT); row.setSourceType("MANUAL"); row.setTenantId(TenantContextHolder.getTenantId());
        mapper.insert(row); return row.getId();
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public void update(HcSlittingPressProductionRecordSaveReqVO req) {
        HcSlittingPressProductionRecordDO old = exists(req.getId()); pending(old); normalize(req); validateBiz(req, old.getId());
        HcSlittingPressProductionRecordDO row = BeanUtils.toBean(req, HcSlittingPressProductionRecordDO.class);
        row.setStatus(WAIT); row.setConfirmerName(null); row.setConfirmTime(null); mapper.updateById(row);
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) { pending(exists(id)); mapper.physicalDeleteById(id); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw invalidParamException("请选择要删除的分切&压槽生产记录");
        List<Long> distinct = ids.stream().distinct().toList();
        List<HcSlittingPressProductionRecordDO> rows = mapper.selectByRecordIds(distinct);
        if (rows.size() != distinct.size()) throw invalidParamException("部分记录不存在，请刷新后重试");
        rows.forEach(this::pending); mapper.physicalDeleteByIds(distinct);
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public Integer confirm(HcSlittingPressProductionRecordConfirmReqVO req) {
        List<Long> ids = req.getIds().stream().distinct().toList();
        List<HcSlittingPressProductionRecordDO> rows = mapper.selectByRecordIds(ids);
        if (rows.size() != ids.size()) throw invalidParamException("部分记录不存在，请刷新后重试");
        String name = first(req.getConfirmerName(), SecurityFrameworkUtils.getLoginUserNickname(), "当前用户");
        LocalDateTime now = LocalDateTime.now(); int count = 0;
        for (HcSlittingPressProductionRecordDO row : rows) {
            if (CONFIRMED.equals(row.getStatus())) continue;
            HcSlittingPressProductionRecordDO update = new HcSlittingPressProductionRecordDO();
            update.setId(row.getId()); update.setStatus(CONFIRMED); update.setConfirmerName(name); update.setConfirmTime(now);
            mapper.updateById(update); count++;
        }
        if (count == 0) throw invalidParamException("所选记录均已确认，无需重复确认"); return count;
    }
    @Override public HcSlittingPressProductionRecordDO get(Long id) { return exists(id); }
    @Override public PageResult<HcSlittingPressProductionRecordDO> getPage(HcSlittingPressProductionRecordPageReqVO req) {
        normalize(req); return mapper.selectPage(req);
    }
    @Override public List<HcSlittingPressProductionRecordExcelVO> buildExportList(HcSlittingPressProductionRecordPageReqVO req) {
        normalize(req); return mapper.selectList(req).stream().map(this::toExcel).toList();
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public HcSlittingPressProductionRecordImportRespVO importExcel(MultipartFile file) throws IOException {
        HcSlittingPressProductionRecordImportRespVO resp = new HcSlittingPressProductionRecordImportRespVO();
        if (file == null || file.isEmpty()) { fail(resp, "导入文件为空"); return resp; }
        List<HcSlittingPressProductionRecordExcelVO> excelRows = ExcelUtils.read(file, HcSlittingPressProductionRecordExcelVO.class);
        List<ImportRow> rows = new ArrayList<>(); Map<String, Integer> keys = new HashMap<>();
        for (int i = 0; i < excelRows.size(); i++) {
            HcSlittingPressProductionRecordExcelVO excel = excelRows.get(i);
            if (blank(excel)) { resp.setSkippedRows(resp.getSkippedRows() + 1); continue; }
            int rowNo = i + 3; resp.setTotalRows(resp.getTotalRows() + 1); int before = resp.getFailureCount();
            ImportRow row = parse(excel, rowNo, resp); if (row == null || before != resp.getFailureCount()) continue;
            String key = String.join("|", row.date.toString(), row.model, row.material, row.batch);
            Integer prior = keys.putIfAbsent(key, rowNo); if (prior != null) { fail(resp, "第" + rowNo + "行：业务关键词与第" + prior + "行重复"); continue; }
            HcSlittingPressProductionRecordDO old = mapper.selectByBizKey(row.date, row.model, row.material, row.batch);
            if (old != null && CONFIRMED.equals(old.getStatus())) { fail(resp, "第" + rowNo + "行：匹配记录已确认，禁止导入覆盖"); continue; }
            row.id = old == null ? null : old.getId(); rows.add(row);
        }
        if (!resp.getFailures().isEmpty()) { resp.getMessages().add("导入校验未通过，未写入任何数据"); return resp; }
        if (rows.isEmpty()) { fail(resp, "导入文件没有有效数据行"); return resp; }
        Long tenant = TenantContextHolder.getTenantId();
        for (ImportRow item : rows) {
            HcSlittingPressProductionRecordDO row = item.toDO(); row.setStatus(WAIT); row.setSourceType("IMPORT"); row.setTenantId(tenant);
            if (item.id == null) { mapper.insert(row); resp.setCreateCount(resp.getCreateCount() + 1); }
            else { row.setId(item.id); mapper.updateById(row); resp.setUpdateCount(resp.getUpdateCount() + 1); }
        }
        resp.getMessages().add(String.format("导入完成：新增 %d 条，更新 %d 条；导入记录均为待确认", resp.getCreateCount(), resp.getUpdateCount()));
        return resp;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public HcSlittingPressProductionRecordInitRespVO initializeFromReports(HcSlittingPressProductionRecordPageReqVO req) {
        normalize(req); req.setPageNo(1); req.setPageSize(PageParam.PAGE_SIZE_NONE); req.setStatus(null);
        List<HcSlittingPressProductionRecordRespVO> sources = sourceService.getList(req);
        HcSlittingPressProductionRecordInitRespVO resp = new HcSlittingPressProductionRecordInitRespVO();
        resp.setSourceCount(sources.size()); Long tenant = TenantContextHolder.getTenantId(); LocalDateTime now = LocalDateTime.now();
        for (HcSlittingPressProductionRecordRespVO source : sources) {
            String model = trim(source.getModelCode()), material = trim(source.getMaterialCode()), batch = trim(source.getBatchNo());
            if (source.getReportDate() == null || model == null || material == null || batch == null) {
                resp.setSkippedCount(resp.getSkippedCount() + 1); resp.getMessages().add("报工数据缺少日期/型号/料号/批号，已跳过"); continue;
            }
            if (mapper.selectByBizKey(source.getReportDate(), model, material, batch) != null) {
                resp.setSkippedCount(resp.getSkippedCount() + 1); continue;
            }
            HcSlittingPressProductionRecordDO row = HcSlittingPressProductionRecordDO.builder()
                    .reportDate(source.getReportDate()).modelCode(model).materialCode(material).batchNo(batch)
                    .slittingInputM(source.getSlittingInputM()).slittingOutputPcs(source.getSlittingOutputPcs())
                    .pressSlotInputPcs(source.getPressSlotActualInputPcs()).pressSlotOutputPcs(source.getPressSlotOutputPcs())
                    .rollerCleanAccumulatedPcs(source.getRollerCleanAccumulatedPcs()).rollerCleanUseDays(source.getRollerCleanUseDays())
                    .bearingReplaceAccumulatedPcs(source.getBearingReplaceAccumulatedPcs()).bearingReplaceUseDays(source.getBearingReplaceUseDays())
                    .recorderName(first(source.getRecorderName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统初始化"))
                    .recordTime(now).status(WAIT).sourceType("REPORT_INIT").remark(trim(source.getRemark())).tenantId(tenant).build();
            mapper.insert(row); resp.setCreateCount(resp.getCreateCount() + 1);
        }
        resp.getMessages().add(String.format("报工初始化完成：读取 %d 条，新增 %d 条，跳过 %d 条", resp.getSourceCount(), resp.getCreateCount(), resp.getSkippedCount()));
        return resp;
    }

    private HcSlittingPressProductionRecordExcelVO toExcel(HcSlittingPressProductionRecordDO r) {
        HcSlittingPressProductionRecordExcelVO e = new HcSlittingPressProductionRecordExcelVO();
        e.setReportDate(r.getReportDate() == null ? null : DATE.format(r.getReportDate())); e.setModelCode(r.getModelCode());
        e.setMaterialCode(r.getMaterialCode()); e.setBatchNo(r.getBatchNo()); e.setSlittingInputM(r.getSlittingInputM());
        e.setSlittingOutputPcs(r.getSlittingOutputPcs()); e.setPressSlotActualInputPcs(r.getPressSlotInputPcs());
        // 旧台账只有一个压槽产出字段，无法区分实际与合格，导出时保持两列一致以兼容历史记录。
        e.setPressSlotActualOutputPcs(r.getPressSlotOutputPcs()); e.setPressSlotOutputPcs(r.getPressSlotOutputPcs());
        e.setRollerCleanAccumulatedPcs(r.getRollerCleanAccumulatedPcs());
        e.setRollerCleanUseDays(r.getRollerCleanUseDays()); e.setBearingReplaceAccumulatedPcs(r.getBearingReplaceAccumulatedPcs());
        e.setBearingReplaceUseDays(r.getBearingReplaceUseDays()); e.setRecorderName(r.getRecorderName());
        e.setRecordTime(format(r.getRecordTime())); e.setConfirmerName(r.getConfirmerName()); e.setConfirmTime(format(r.getConfirmTime())); e.setRemark(r.getRemark()); return e;
    }
    private ImportRow parse(HcSlittingPressProductionRecordExcelVO e, int no, HcSlittingPressProductionRecordImportRespVO resp) {
        ImportRow r = new ImportRow(); r.date = parseDate(e.getReportDate(), no, resp); r.model = required(e.getModelCode(), no, "型号", resp);
        r.material = required(e.getMaterialCode(), no, "料号", resp); r.batch = required(e.getBatchNo(), no, "批号", resp);
        r.slittingInput = e.getSlittingInputM(); r.slittingOutput = e.getSlittingOutputPcs(); r.pressInput = e.getPressSlotActualInputPcs();
        r.pressOutput = e.getPressSlotOutputPcs() == null ? e.getPressSlotActualOutputPcs() : e.getPressSlotOutputPcs(); r.rollerPcs = e.getRollerCleanAccumulatedPcs(); r.rollerDays = e.getRollerCleanUseDays();
        r.bearingPcs = e.getBearingReplaceAccumulatedPcs(); r.bearingDays = e.getBearingReplaceUseDays();
        r.recorder = required(e.getRecorderName(), no, "记录人", resp); r.recordTime = parseTime(e.getRecordTime(), no, resp); r.remark = trim(e.getRemark());
        for (Number value : List.of(nz(r.slittingInput), nz(r.slittingOutput), nz(r.pressInput), nz(r.pressOutput), nz(r.rollerPcs), nz(r.rollerDays), nz(r.bearingPcs), nz(r.bearingDays)))
            if (new BigDecimal(value.toString()).signum() < 0) { fail(resp, "第" + no + "行：数量和使用天数不能为负数"); break; }
        return r.date == null || r.model == null || r.material == null || r.batch == null || r.recorder == null || r.recordTime == null ? null : r;
    }
    private void normalize(HcSlittingPressProductionRecordSaveReqVO r) {
        r.setModelCode(trim(r.getModelCode())); r.setMaterialCode(trim(r.getMaterialCode())); r.setBatchNo(trim(r.getBatchNo()));
        r.setRecorderName(trim(r.getRecorderName())); r.setRemark(trim(r.getRemark()));
        if (r.getRecordTime() == null || r.getRecordTime().getYear() < 2000) throw invalidParamException("记录时间无效");
        for (Number v : List.of(nz(r.getSlittingInputM()), nz(r.getSlittingOutputPcs()), nz(r.getPressSlotInputPcs()), nz(r.getPressSlotOutputPcs()), nz(r.getRollerCleanAccumulatedPcs()), nz(r.getRollerCleanUseDays()), nz(r.getBearingReplaceAccumulatedPcs()), nz(r.getBearingReplaceUseDays())))
            if (new BigDecimal(v.toString()).signum() < 0) throw invalidParamException("数量和使用天数不能为负数");
    }
    private void normalize(HcSlittingPressProductionRecordPageReqVO r) { r.setModelCode(trim(r.getModelCode())); r.setMaterialCode(trim(r.getMaterialCode())); r.setBatchNo(trim(r.getBatchNo())); r.setRecorderName(trim(r.getRecorderName())); r.setStatus(trim(r.getStatus())); }
    private void validateBiz(HcSlittingPressProductionRecordSaveReqVO r, Long current) { HcSlittingPressProductionRecordDO old = mapper.selectByBizKey(r.getReportDate(), r.getModelCode(), r.getMaterialCode(), r.getBatchNo()); if (old != null && (current == null || !current.equals(old.getId()))) throw invalidParamException("相同日期、型号、料号、批号的记录已存在"); }
    private HcSlittingPressProductionRecordDO exists(Long id) { HcSlittingPressProductionRecordDO r = id == null ? null : mapper.selectById(id); if (r == null) throw invalidParamException("分切&压槽生产记录不存在"); return r; }
    private void pending(HcSlittingPressProductionRecordDO r) { if (CONFIRMED.equals(r.getStatus())) throw invalidParamException("记录已确认，不能修改或删除"); }
    private void fail(HcSlittingPressProductionRecordImportRespVO r, String message) { r.getFailures().add(message); r.setFailureCount(r.getFailures().size()); }
    private String required(String value, int row, String field, HcSlittingPressProductionRecordImportRespVO resp) { String t = trim(value); if (t == null) fail(resp, "第" + row + "行：" + field + "不能为空"); return t; }
    private LocalDate parseDate(String value, int row, HcSlittingPressProductionRecordImportRespVO resp) { String t = trim(value); if (t == null) { fail(resp, "第" + row + "行：日期不能为空"); return null; } try { String n=t.replace('/','-'); return LocalDate.parse(n.substring(0,Math.min(10,n.length())),DateTimeFormatter.ofPattern("yyyy-M-d")); } catch (RuntimeException ex) { fail(resp,"第"+row+"行：日期格式应为yyyy-MM-dd"); return null; } }
    private LocalDateTime parseTime(String value, int row, HcSlittingPressProductionRecordImportRespVO resp) { String t=trim(value); if(t==null){fail(resp,"第"+row+"行：记录时间不能为空");return null;} String n=t.replace('/','-').replace('T',' '); for(DateTimeFormatter f:List.of(DATE_TIME,DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),DateTimeFormatter.ofPattern("yyyy-M-d H:m:s"),DateTimeFormatter.ofPattern("yyyy-M-d H:m"))){try{return LocalDateTime.parse(n,f);}catch(DateTimeParseException ignored){}} fail(resp,"第"+row+"行：记录时间格式应为yyyy-MM-dd HH:mm:ss");return null; }
    private boolean blank(HcSlittingPressProductionRecordExcelVO e) { return e==null || trim(e.getReportDate())==null&&trim(e.getModelCode())==null&&trim(e.getMaterialCode())==null&&trim(e.getBatchNo())==null&&e.getSlittingInputM()==null&&e.getSlittingOutputPcs()==null&&e.getPressSlotActualInputPcs()==null&&e.getPressSlotActualOutputPcs()==null&&e.getPressSlotOutputPcs()==null&&e.getRollerCleanAccumulatedPcs()==null&&e.getRollerCleanUseDays()==null&&e.getBearingReplaceAccumulatedPcs()==null&&e.getBearingReplaceUseDays()==null&&trim(e.getRecorderName())==null&&trim(e.getRecordTime())==null&&trim(e.getRemark())==null; }
    private Number nz(Number v){return v==null?0:v;} private String format(LocalDateTime v){return v==null?null:DATE_TIME.format(v);} private String trim(String v){String t=v==null?null:v.trim();return t==null||t.isEmpty()?null:t;} private String first(String... values){for(String v:values){String t=trim(v);if(t!=null)return t;}return null;}
    private static class ImportRow { Long id; LocalDate date; String model,material,batch,recorder,remark; BigDecimal slittingInput,pressInput,pressOutput; Integer slittingOutput,rollerPcs,rollerDays,bearingPcs,bearingDays; LocalDateTime recordTime; HcSlittingPressProductionRecordDO toDO(){return HcSlittingPressProductionRecordDO.builder().reportDate(date).modelCode(model).materialCode(material).batchNo(batch).slittingInputM(slittingInput).slittingOutputPcs(slittingOutput).pressSlotInputPcs(pressInput).pressSlotOutputPcs(pressOutput).rollerCleanAccumulatedPcs(rollerPcs).rollerCleanUseDays(rollerDays).bearingReplaceAccumulatedPcs(bearingPcs).bearingReplaceUseDays(bearingDays).recorderName(recorder).recordTime(recordTime).remark(remark).build();} }
}
