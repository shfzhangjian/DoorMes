package cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HcProductionInstructionEffectiveChangeoverTest {

    private static final LocalDateTime FIRST_START = LocalDateTime.of(2026, 9, 5, 10, 0);
    private static final String BATCH_NO = "W26H163AP";
    private SqlSession session;
    private HcProductionInstructionMapper mapper;

    @BeforeEach
    void setUp() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:changeover_" + UUID.randomUUID() + ";MODE=MySQL");
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("changeover-test", new JdbcTransactionFactory(), dataSource));
        configuration.addMapper(HcProductionInstructionMapper.class);
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true);
        mapper = session.getMapper(HcProductionInstructionMapper.class);

        TableInfo tableInfo = TableInfoHelper.getTableInfo(HcProductionInstructionDO.class);
        List<String> columns = new ArrayList<>();
        columns.add(tableInfo.getKeyColumn() + " BIGINT PRIMARY KEY");
        for (TableFieldInfo field : tableInfo.getFieldList()) {
            columns.add(field.getColumn() + " " + columnType(field));
        }
        try (var statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE " + tableInfo.getTableName() + " (" + String.join(", ", columns) + ")");
        }
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    void shouldPreferSecondExecutingChangeoverOverFirstCompletedWithEndTime() throws SQLException {
        insert(33L, 894L, BATCH_NO, "COMPLETED", "CONFIRMED", FIRST_START, FIRST_START.plusMinutes(10));
        insert(34L, 894L, BATCH_NO, "EXECUTING", "CONFIRMED", FIRST_START.plusMinutes(16), null);

        assertEquals(34L, mapper.selectLatestEffectiveChangeover(134L, 894L, BATCH_NO).getId());
        assertEquals(34L, mapper.selectLatestEffectiveChangeoverForUpdate(134L, 894L, BATCH_NO).getId());
    }

    @Test
    void shouldPreferExecutingEvenWhenCompletedInstructionHasLaterStart() throws SQLException {
        insert(33L, 894L, BATCH_NO, "EXECUTING", "CONFIRMED", FIRST_START, null);
        insert(34L, 894L, BATCH_NO, "COMPLETED", "CONFIRMED", FIRST_START.plusMinutes(16), FIRST_START.plusMinutes(20));

        assertEquals(33L, mapper.selectLatestEffectiveChangeover(134L, 894L, BATCH_NO).getId());
    }

    @Test
    void shouldUseLatestStartInsteadOfLatestEndWhenAllChangesCompleted() throws SQLException {
        insert(33L, 894L, BATCH_NO, "COMPLETED", "CONFIRMED", FIRST_START, FIRST_START.plusHours(2));
        insert(34L, 894L, BATCH_NO, "COMPLETED", "CONFIRMED", FIRST_START.plusMinutes(16), FIRST_START.plusHours(1));

        assertEquals(34L, mapper.selectLatestEffectiveChangeover(134L, 894L, BATCH_NO).getId());
    }

    @Test
    void shouldResolveSameStartByIdAndIgnoreRevokedPendingAndOtherScopes() throws SQLException {
        insert(33L, 894L, BATCH_NO, "COMPLETED", "CONFIRMED", FIRST_START, FIRST_START.plusMinutes(10));
        insert(34L, 894L, BATCH_NO, "COMPLETED", "CONFIRMED", FIRST_START, FIRST_START.plusMinutes(10));
        insert(35L, 894L, BATCH_NO, "EXECUTING", "REVOKED", FIRST_START.plusMinutes(20), null);
        insert(36L, 894L, BATCH_NO, "PENDING", "CONFIRMED", null, null);
        insert(37L, 895L, BATCH_NO, "EXECUTING", "CONFIRMED", FIRST_START.plusMinutes(20), null);
        insert(38L, 894L, "OTHER-BATCH", "EXECUTING", "CONFIRMED", FIRST_START.plusMinutes(20), null);

        assertEquals(34L, mapper.selectLatestEffectiveChangeover(134L, 894L, BATCH_NO).getId());
        assertNull(mapper.selectLatestEffectiveChangeover(999L, 894L, BATCH_NO));
    }

    private void insert(Long id, Long operationId, String batchNo, String executeStatus, String status,
                        LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        Connection connection = session.getConnection();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO mes_pp_production_instruction
                    (id, plan_id, plan_operation_id, segment_batch_no, instruction_type,
                     execute_status, status, execute_start_time, execute_end_time, deleted)
                VALUES (?, 134, ?, ?, 'CHANGEOVER', ?, ?, ?, ?, 0)
                """)) {
            statement.setLong(1, id);
            statement.setLong(2, operationId);
            statement.setString(3, batchNo);
            statement.setString(4, executeStatus);
            statement.setString(5, status);
            statement.setObject(6, startTime);
            statement.setObject(7, endTime);
            statement.executeUpdate();
        }
    }

    private String columnType(TableFieldInfo field) {
        if ("deleted".equals(field.getProperty())) return "INTEGER DEFAULT 0";
        Class<?> type = field.getPropertyType();
        if (type == Long.class) return "BIGINT";
        if (type == Integer.class) return "INTEGER";
        if (type == Boolean.class) return "BOOLEAN";
        if (type == LocalDateTime.class) return "TIMESTAMP";
        return "VARCHAR(2048)";
    }
}
