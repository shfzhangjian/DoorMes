package cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory;

import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPiecePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryPieceDO;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.session.SqlSession;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 在隔离内存库执行真实 Mapper 和分页，验证分类条件在 count/limit 前生效。 */
class HcNgInventoryUnqualifiedQueryTest {
    private SqlSession session;
    private HcNgInventoryPieceMapper mapper;

    @BeforeEach void setup() throws Exception {
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:ng_split_" + UUID.randomUUID() + ";MODE=MySQL");
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.setEnvironment(new Environment("ng-test", new JdbcTransactionFactory(), ds));
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        config.addInterceptor(interceptor);
        config.addMapper(HcNgInventoryPieceMapper.class);
        session = new MybatisSqlSessionFactoryBuilder().build(config).openSession(true);
        mapper = session.getMapper(HcNgInventoryPieceMapper.class);
        var table = TableInfoHelper.getTableInfo(HcNgInventoryPieceDO.class);
        List<String> columns = new ArrayList<>();
        columns.add(table.getKeyColumn() + " BIGINT PRIMARY KEY");
        table.getFieldList().forEach(field -> {
            Class<?> type = field.getPropertyType();
            String sqlType = type == Boolean.class || type == boolean.class ? "BOOLEAN" :
                    type == LocalDateTime.class ? "TIMESTAMP" :
                    type == BigDecimal.class ? "DECIMAL(18,4)" :
                    Number.class.isAssignableFrom(type) ? "BIGINT" : "VARCHAR(255)";
            columns.add(field.getColumn() + " " + sqlType);
        });
        try (var stmt = session.getConnection().createStatement()) {
            stmt.execute("CREATE TABLE " + table.getTableName() + " (" + String.join(",", columns) + ")");
            stmt.execute("""
                INSERT INTO mes_inv_ng_piece(id,process_type,quality_result,status,deleted,create_time) VALUES
                (1,'SLITTING','NG','STORED',false,CURRENT_TIMESTAMP),
                (2,'SLITTING','NG','FROZEN',false,CURRENT_TIMESTAMP),
                (3,'SLITTING','OK','FROZEN',false,CURRENT_TIMESTAMP),
                (4,'PRESS_SLOT','NG','FROZEN',false,CURRENT_TIMESTAMP),
                (5,'SLITTING',NULL,'WAIT_SHELF',false,CURRENT_TIMESTAMP),
                (6,'SLITTING','','WAIT_FREEZE_SHELF',false,CURRENT_TIMESTAMP),
                (7,'SLITTING','NG','OUTBOUNDED',false,CURRENT_TIMESTAMP),
                (8,'SLITTING','NG','SCRAPPED',false,CURRENT_TIMESTAMP),
                (9,'SLITTING','NG','STORED',true,CURRENT_TIMESTAMP)
                """);
        }
    }
    @AfterEach void close() { if (session != null) session.close(); }
    private NgPiecePageReqVO request(String process) {
        var req = new NgPiecePageReqVO();
        req.setProcessType(process);
        req.setUnqualifiedOnly(true);
        req.setPageNo(1);
        req.setPageSize(2);
        return req;
    }
    @Test void excludesOkFrozenOtherProcessAndDisposedBeforePagination() {
        var req = request("SLITTING");
        var page = mapper.selectPage(req);
        assertEquals(4L, page.getTotal());
        assertEquals(List.of(6L, 5L), page.getList().stream().map(HcNgInventoryPieceDO::getId).toList());
        req.setPageNo(2);
        assertEquals(List.of(2L, 1L), mapper.selectPage(req).getList().stream().map(HcNgInventoryPieceDO::getId).toList());
        assertEquals(List.of(4L), mapper.selectPage(request("PRESS_SLOT")).getList().stream().map(HcNgInventoryPieceDO::getId).toList());
    }
    @Test void frozenFilterKeepsOnlyNgAndCannotQueryDisposedViaOverride() {
        var req = request("SLITTING");
        req.setStatus("FROZEN");
        assertEquals(List.of(2L), mapper.selectPage(req).getList().stream().map(HcNgInventoryPieceDO::getId).toList());
        req.setStatus("SCRAPPED");
        req.setIncludeScrapped(true);
        assertEquals(0L, mapper.selectPage(req).getTotal());
    }
    @Test void existingQueryWithoutFlagStillIncludesOkFrozen() {
        var req = request("SLITTING");
        req.setUnqualifiedOnly(null);
        req.setPageSize(20);
        assertEquals(List.of(3L, 2L, 1L), mapper.selectPage(req).getList().stream().map(HcNgInventoryPieceDO::getId).toList());
    }
}
