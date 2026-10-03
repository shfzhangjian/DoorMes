package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableReplaceReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 用真实 H2 事务/行锁验证服务失败回滚与并发；Mapper 适配为最小 JDBC 表，不替代线上 MySQL 验收。 */
class HcCutRoundBladeTransactionTest {
    JdbcTemplate jdbc;
    TransactionTemplate tx;
    HcCutRoundBladeConsumptionService service;
    HcCutRoundSpareMapper spare;
    @BeforeEach void setup() {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        jdbc=new JdbcTemplate(ds);var tm=new DataSourceTransactionManager(ds);tx=new TransactionTemplate(tm);
        jdbc.execute("CREATE TABLE spare(id BIGINT PRIMARY KEY, use_count INT)");
        jdbc.execute("CREATE TABLE ledger(id BIGINT PRIMARY KEY, qty DECIMAL(18,3))");
        jdbc.execute("CREATE TABLE consume(id BIGINT AUTO_INCREMENT PRIMARY KEY, ledger_id BIGINT, qty DECIMAL(18,3))");
        jdbc.execute("CREATE TABLE record(request_key VARCHAR(64) PRIMARY KEY, request_hash VARCHAR(64), spare_id BIGINT, consume_id BIGINT)");
        jdbc.update("INSERT INTO spare VALUES(5,500),(6,500)");jdbc.update("INSERT INTO ledger VALUES(10,1)");
        spare=mock(HcCutRoundSpareMapper.class);var ledgers=mock(HcToolingConsumableLedgerMapper.class);
        var consumes=mock(HcToolingConsumableConsumeMapper.class);var records=mock(HcCutRoundSpareRecordMapper.class);
        when(spare.selectForUpdate(anyLong(),eq("CUTTING_BLADE"))).thenAnswer(inv->jdbc.queryForObject(
                "SELECT * FROM spare WHERE id=? FOR UPDATE",(rs,n)->HcCutRoundSpareDO.builder().id(rs.getLong("id"))
                        .equipmentId(rs.getLong("id")).tenantId(1L).equipmentCode("CUT").useCount(rs.getInt("use_count")).build(),inv.<Long>getArgument(0)));
        when(ledgers.selectByIdForUpdate(10L)).thenAnswer(inv->jdbc.queryForObject(
                "SELECT * FROM ledger WHERE id=10 FOR UPDATE",(rs,n)->HcToolingConsumableLedgerDO.builder().id(10L)
                        .tenantId(1L).processCode("CUT_ROUND").consumableType("BLADE").usageStatus("ACTIVE")
                        .receiveQty(rs.getBigDecimal("qty")).model("M1").batchNo("B1").build()));
        when(consumes.sumConsumeQtyForUpdate(10L)).thenAnswer(inv->jdbc.query("SELECT qty FROM consume WHERE ledger_id=10 FOR UPDATE",
                (rs,n)->rs.getBigDecimal(1)).stream().reduce(BigDecimal.ZERO,BigDecimal::add));
        when(consumes.insert(any(HcToolingConsumableConsumeDO.class))).thenAnswer(inv->{
            var row=inv.<HcToolingConsumableConsumeDO>getArgument(0);
            jdbc.update("INSERT INTO consume(ledger_id,qty) VALUES(?,?)",row.getLedgerId(),row.getConsumeQty());
            row.setId(jdbc.queryForObject("SELECT MAX(id) FROM consume",Long.class));return 1;
        });
        when(records.byRequest(anyString())).thenAnswer(inv->{
            var rows=jdbc.query("SELECT * FROM record WHERE request_key=? FOR UPDATE",(rs,n)->HcCutRoundSpareRecordDO.builder()
                    .spareId(rs.getLong("spare_id")).equipmentId(rs.getLong("spare_id")).planOperationId(2L)
                    .requestHash(rs.getString("request_hash")).build(),inv.<String>getArgument(0));
            return rows.isEmpty()?null:rows.get(0);
        });
        when(records.insert(any(HcCutRoundSpareRecordDO.class))).thenAnswer(inv->{
            var row=inv.<HcCutRoundSpareRecordDO>getArgument(0);
            return jdbc.update("INSERT INTO record VALUES(?,?,?,?)",row.getRequestKey(),row.getRequestHash(),row.getSpareId(),row.getConsumeId());
        });
        when(records.updateById(any(HcCutRoundSpareRecordDO.class))).thenAnswer(inv->{
            var row=inv.<HcCutRoundSpareRecordDO>getArgument(0);
            return jdbc.update("UPDATE record SET consume_id=? WHERE request_key=?",row.getConsumeId(),row.getRequestKey());
        });
        when(spare.updateById(any(HcCutRoundSpareDO.class))).thenAnswer(inv->{var row=inv.<HcCutRoundSpareDO>getArgument(0);
            return jdbc.update("UPDATE spare SET use_count=? WHERE id=?",row.getUseCount(),row.getId());});
        var target=new HcCutRoundBladeConsumptionService();
        ReflectionTestUtils.setField(target,"spareMapper",spare);ReflectionTestUtils.setField(target,"ledgerMapper",ledgers);
        ReflectionTestUtils.setField(target,"consumeMapper",consumes);ReflectionTestUtils.setField(target,"recordMapper",records);
        var proxy=new ProxyFactory(target);proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(tm,new AnnotationTransactionAttributeSource()));
        service=(HcCutRoundBladeConsumptionService)proxy.getProxy();
    }
    HcPressSlotConsumableReplaceReqVO request(String key) {
        var req=new HcPressSlotConsumableReplaceReqVO();req.setRequestKey(key);req.setLedgerId(10L);
        req.setReplaceQuantity(BigDecimal.ONE);req.setReplaceTime(LocalDateTime.of(2026,9,23,12,0));
        req.setReplaceReason("磨损");req.setOperatorId(20L);req.setOperatorName("测试");return req;
    }
    Long replace(String key,long equipment) {
        TenantContextHolder.setTenantId(1L);
        try { var op=new HcPlanOrderOperationDO();op.setId(2L);op.setTenantId(1L);op.setPlanId(3L);
            return tx.execute(status->service.replace(request(key),op,equipment));
        } finally {TenantContextHolder.clear();}
    }
    @Test void stateWriteFailureRollsBackConsumeAndRequestReservation() {
        doThrow(new IllegalStateException("模拟状态更新失败")).when(spare).updateById(any(HcCutRoundSpareDO.class));
        assertThrows(IllegalStateException.class,()->replace("blade-rollback-12345",5));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM consume",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM record",Integer.class));
        assertEquals(500,jdbc.queryForObject("SELECT use_count FROM spare WHERE id=5",Integer.class));
    }
    @Test void concurrentEquipmentCannotConsumeTheLastBladeTwice() throws Exception {
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var one=pool.submit(()->attempt(start,"blade-concurrent-111",5));
            var two=pool.submit(()->attempt(start,"blade-concurrent-222",6));start.countDown();
            assertEquals(1,one.get(10,TimeUnit.SECONDS)+two.get(10,TimeUnit.SECONDS));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM consume",Integer.class));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM spare WHERE use_count=0",Integer.class));
        } finally {pool.shutdownNow();}
    }
    int attempt(CountDownLatch start,String key,long equipment) throws Exception {
        start.await();try{replace(key,equipment);return 1;}catch(cn.iocoder.yudao.framework.common.exception.ServiceException expected){return 0;}
    }
    @Test void committedRetryDoesNotDeductAgainEvenWhenNoStockRemains() {
        assertEquals(5L,replace("blade-repeat-1234567",5));
        assertEquals(5L,replace("blade-repeat-1234567",5));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM consume",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM record",Integer.class));
    }
}
