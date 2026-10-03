package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.transaction.BpmTransactionExecutor;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.message.BpmMessageService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.alibaba.druid.pool.DruidDataSource;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmTaskServiceImplNcrTransactionTest {

    private static final String PROCESS_KEY = "qms_raw_material_ncr_disposition";
    private static final String START_USER_NODE = "StartUserNode";
    private static final String QUALITY_CONFIRM_NODE = "quality_confirm";

    private DruidDataSource dataSource;
    private ProcessEngine processEngine;
    private JdbcTemplate jdbcTemplate;
    private JdbcTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;
    private BpmMessageService messageService;
    private BpmnModel ncrModel;

    @BeforeEach
    void setUp() {
        String databaseName = "bpm_ncr_tx_" + UUID.randomUUID().toString().replace("-", "");
        dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:mem:" + databaseName + ";MODE=LEGACY;DB_CLOSE_DELAY=-1");
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(1);
        dataSource.setMaxActive(5);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestOnBorrow(true);

        transactionManager = new JdbcTransactionManager(dataSource);
        transactionTemplate = new TransactionTemplate(transactionManager);
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE TABLE bpm_tx_probe (id BIGINT PRIMARY KEY, probe_value VARCHAR(32))");

        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        configuration.setAsyncExecutorActivate(false);
        processEngine = configuration.buildProcessEngine();

        var deployment = processEngine.getRepositoryService().createDeployment()
                .tenantId("1")
                .addClasspathResource("bpmn/qms_raw_material_ncr_disposition.bpmn20.xml")
                .deploy();
        var processDefinition = processEngine.getRepositoryService().createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        ncrModel = processEngine.getRepositoryService().getBpmnModel(processDefinition.getId());
    }

    @AfterEach
    void tearDown() {
        if (processEngine != null) {
            processEngine.close();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void shouldKeepConnectionUsableAfterCurrentNcrStartNodeCompletes() {
        String processInstanceId = assertDoesNotThrow(() -> transactionTemplate.execute(status -> {
            RuntimeService runtimeService = processEngine.getRuntimeService();
            processEngine.getIdentityService().setAuthenticatedUserId("144");
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey(PROCESS_KEY)
                    .tenantId("1")
                    .variables(Map.of(
                            "rawMaterialTransferRoute", "",
                            "coll_userList", List.of("200")))
                    .start();

            Task startTask = processEngine.getTaskService().createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .taskDefinitionKey(START_USER_NODE)
                    .singleResult();
            assertNotNull(startTask);
            processEngine.getTaskService().setAssignee(startTask.getId(), "144");
            startTask = processEngine.getTaskService().createTaskQuery().taskId(startTask.getId()).singleResult();

            BpmTaskServiceImpl taskService = buildTaskService(processInstance);
            taskService.processTaskCreated(startTask);
            taskService.processTaskAssigned(startTask);
            processEngine.getTaskService().complete(startTask.getId());

            Task qualityConfirmTask = processEngine.getTaskService().createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .taskDefinitionKey(QUALITY_CONFIRM_NODE)
                    .singleResult();
            assertNotNull(qualityConfirmTask);
            processEngine.getTaskService().setAssignee(qualityConfirmTask.getId(), "200");
            qualityConfirmTask = processEngine.getTaskService().createTaskQuery()
                    .taskId(qualityConfirmTask.getId())
                    .singleResult();
            taskService.processTaskCreated(qualityConfirmTask);
            taskService.processTaskAssigned(qualityConfirmTask);

            jdbcTemplate.update("INSERT INTO bpm_tx_probe (id, probe_value) VALUES (?, ?)", 1L, "submitted");
            return processInstance.getId();
        }));
        assertNotNull(processInstanceId);

        assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
        for (int i = 0; i < 20; i++) {
            assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bpm_tx_probe", Integer.class));
            assertEquals(QUALITY_CONFIRM_NODE, processEngine.getTaskService().createTaskQuery()
                    .processInstanceId(processInstanceId).singleResult().getTaskDefinitionKey());
        }
        verify(messageService).sendMessageWhenTaskAssigned(any());
    }

    private BpmTaskServiceImpl buildTaskService(ProcessInstance processInstance) {
        BpmTaskServiceImpl taskService = new BpmTaskServiceImpl();
        ReflectionTestUtils.setField(taskService, "taskService", processEngine.getTaskService());
        ReflectionTestUtils.setField(taskService, "historyService", processEngine.getHistoryService());
        ReflectionTestUtils.setField(taskService, "runtimeService", processEngine.getRuntimeService());
        ReflectionTestUtils.setField(taskService, "bpmTransactionExecutor",
                new BpmTransactionExecutor(transactionManager));

        BpmProcessInstanceService processInstanceService = mock(BpmProcessInstanceService.class);
        when(processInstanceService.getProcessInstance(processInstance.getId())).thenAnswer(invocation ->
                processEngine.getRuntimeService().createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId()).singleResult());
        ReflectionTestUtils.setField(taskService, "processInstanceService", processInstanceService);

        BpmProcessDefinitionService processDefinitionService = mock(BpmProcessDefinitionService.class);
        when(processDefinitionService.getProcessDefinitionInfo(any()))
                .thenReturn(new BpmProcessDefinitionInfoDO());
        ReflectionTestUtils.setField(taskService, "bpmProcessDefinitionService", processDefinitionService);

        BpmModelService modelService = mock(BpmModelService.class);
        when(modelService.getBpmnModelByDefinitionId(any())).thenReturn(ncrModel);
        ReflectionTestUtils.setField(taskService, "modelService", modelService);

        AdminUserApi adminUserApi = mock(AdminUserApi.class);
        when(adminUserApi.getUser(144L)).thenReturn(new AdminUserRespDTO().setId(144L).setNickname("发起人"));
        ReflectionTestUtils.setField(taskService, "adminUserApi", adminUserApi);

        messageService = mock(BpmMessageService.class);
        ReflectionTestUtils.setField(taskService, "messageService", messageService);
        return taskService;
    }

}
