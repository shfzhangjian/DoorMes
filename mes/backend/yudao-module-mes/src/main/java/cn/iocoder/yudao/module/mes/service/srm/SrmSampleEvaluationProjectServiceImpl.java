package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationProjectDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationProjectUserDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleEvaluationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleEvaluationProjectMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleEvaluationProjectUserMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_ADMIN_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_PROJECT_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_PROJECT_INITIATOR_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_PROJECT_IN_USE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_PROJECT_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_PROJECT_USER_REQUIRED;

@Service
@Validated
public class SrmSampleEvaluationProjectServiceImpl implements SrmSampleEvaluationProjectService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String EVALUATION_ADMIN_ROLE = "srm_evaluation_admin";
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";

    @Resource
    private SrmSampleEvaluationProjectMapper projectMapper;
    @Resource
    private SrmSampleEvaluationProjectUserMapper projectUserMapper;
    @Resource
    private SrmSampleEvaluationMapper sampleEvaluationMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProject(SrmSampleEvaluationProjectSaveReqVO reqVO) {
        assertEvaluationAdmin();
        if (projectMapper.selectByProjectCode(StrUtil.trim(reqVO.getProjectCode())) != null) {
            throw exception(SRM_SAMPLE_EVALUATION_PROJECT_CODE_EXISTS);
        }
        SrmSampleEvaluationProjectDO project = new SrmSampleEvaluationProjectDO();
        copyProjectFields(reqVO, project);
        project.setVersion(0);
        projectMapper.insert(project);
        saveProjectUsers(project, reqVO.getUsers());
        return project.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(SrmSampleEvaluationProjectSaveReqVO reqVO) {
        assertEvaluationAdmin();
        SrmSampleEvaluationProjectDO project = validateProject(reqVO.getId());
        SrmSampleEvaluationProjectDO sameCode = projectMapper.selectByProjectCode(StrUtil.trim(reqVO.getProjectCode()));
        if (sameCode != null && !Objects.equals(sameCode.getId(), project.getId())) {
            throw exception(SRM_SAMPLE_EVALUATION_PROJECT_CODE_EXISTS);
        }
        copyProjectFields(reqVO, project);
        projectMapper.updateById(project);
        saveProjectUsers(project, reqVO.getUsers());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id) {
        assertEvaluationAdmin();
        SrmSampleEvaluationProjectDO project = validateProject(id);
        Long usedCount = sampleEvaluationMapper.selectCount(new LambdaQueryWrapperX<SrmSampleEvaluationDO>()
                .eq(SrmSampleEvaluationDO::getProjectId, project.getId()));
        if (usedCount != null && usedCount > 0) {
            throw exception(SRM_SAMPLE_EVALUATION_PROJECT_IN_USE);
        }
        projectUserMapper.deleteByProjectId(project.getId());
        projectMapper.deleteById(project.getId());
    }

    @Override
    public SrmSampleEvaluationProjectRespVO getProject(Long id) {
        return buildProjectResp(validateProject(id), true);
    }

    @Override
    public PageResult<SrmSampleEvaluationProjectRespVO> getProjectPage(SrmSampleEvaluationProjectPageReqVO reqVO) {
        PageResult<SrmSampleEvaluationProjectDO> page = projectMapper.selectPage(reqVO);
        return new PageResult<>(BeanUtils.toBean(page.getList(), SrmSampleEvaluationProjectRespVO.class),
                page.getTotal());
    }

    @Override
    public List<SrmSampleEvaluationProjectRespVO> getEnabledProjectList() {
        return BeanUtils.toBean(projectMapper.selectEnabledList(), SrmSampleEvaluationProjectRespVO.class);
    }

    private void saveProjectUsers(SrmSampleEvaluationProjectDO project,
                                  List<SrmSampleEvaluationProjectSaveReqVO.UserConfig> reqUsers) {
        List<SrmSampleEvaluationProjectUserDO> configs = buildUserConfigs(project.getId(), reqUsers);
        if (STATUS_ENABLED.equals(project.getStatus())) {
            if (CollUtil.isEmpty(configs)) {
                throw exception(SRM_SAMPLE_EVALUATION_PROJECT_USER_REQUIRED);
            }
            boolean hasInitiator = configs.stream().anyMatch(item -> Boolean.TRUE.equals(item.getCanInitiate()));
            if (!hasInitiator) {
                throw exception(SRM_SAMPLE_EVALUATION_PROJECT_INITIATOR_REQUIRED);
            }
        }
        projectUserMapper.deleteByProjectId(project.getId());
        if (CollUtil.isNotEmpty(configs)) {
            projectUserMapper.insertBatch(configs);
        }
    }

    private List<SrmSampleEvaluationProjectUserDO> buildUserConfigs(Long projectId,
                                                                    List<SrmSampleEvaluationProjectSaveReqVO.UserConfig> reqUsers) {
        if (CollUtil.isEmpty(reqUsers)) {
            return List.of();
        }
        List<SrmSampleEvaluationProjectSaveReqVO.UserConfig> effectiveUsers = reqUsers.stream()
                .filter(item -> item != null && item.getUserId() != null && StrUtil.isNotBlank(item.getDeptName()))
                .toList();
        if (CollUtil.isEmpty(effectiveUsers)) {
            return List.of();
        }
        Set<Long> userIds = effectiveUsers.stream().map(SrmSampleEvaluationProjectSaveReqVO.UserConfig::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        adminUserApi.validateUserList(userIds);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<String> uniqueKeys = new LinkedHashSet<>();
        List<SrmSampleEvaluationProjectUserDO> result = new ArrayList<>();
        for (SrmSampleEvaluationProjectSaveReqVO.UserConfig reqUser : effectiveUsers) {
            String deptName = StrUtil.trim(reqUser.getDeptName());
            String key = deptName + "#" + reqUser.getUserId();
            if (!uniqueKeys.add(key)) {
                continue;
            }
            AdminUserRespDTO user = userMap.get(reqUser.getUserId());
            SrmSampleEvaluationProjectUserDO config = new SrmSampleEvaluationProjectUserDO();
            config.setProjectId(projectId);
            config.setDeptCode(StrUtil.trim(reqUser.getDeptCode()));
            config.setDeptName(deptName);
            config.setUserId(reqUser.getUserId());
            config.setUserName(userName(user, reqUser.getUserName()));
            config.setCanInitiate(Boolean.TRUE.equals(reqUser.getCanInitiate()));
            config.setCanAssign(Boolean.TRUE.equals(reqUser.getCanAssign()));
            config.setSortNo(reqUser.getSortNo() == null ? (result.size() + 1) * 10 : reqUser.getSortNo());
            config.setRemark(StrUtil.trim(reqUser.getRemark()));
            result.add(config);
        }
        return result;
    }

    private SrmSampleEvaluationProjectRespVO buildProjectResp(SrmSampleEvaluationProjectDO project,
                                                              boolean includeUsers) {
        SrmSampleEvaluationProjectRespVO resp = BeanUtils.toBean(project, SrmSampleEvaluationProjectRespVO.class);
        if (includeUsers) {
            resp.setUsers(projectUserMapper.selectListByProjectId(project.getId()).stream()
                    .map(item -> BeanUtils.toBean(item, SrmSampleEvaluationProjectRespVO.UserConfig.class))
                    .toList());
        }
        return resp;
    }

    private void copyProjectFields(SrmSampleEvaluationProjectSaveReqVO reqVO,
                                   SrmSampleEvaluationProjectDO project) {
        project.setProjectCode(StrUtil.trim(reqVO.getProjectCode()));
        project.setProjectName(StrUtil.trim(reqVO.getProjectName()));
        String status = StrUtil.blankToDefault(reqVO.getStatus(), STATUS_ENABLED);
        project.setStatus(STATUS_DISABLED.equals(status) ? STATUS_DISABLED : STATUS_ENABLED);
        project.setRemark(StrUtil.trim(reqVO.getRemark()));
    }

    private SrmSampleEvaluationProjectDO validateProject(Long id) {
        SrmSampleEvaluationProjectDO project = id == null ? null : projectMapper.selectById(id);
        if (project == null) {
            throw exception(SRM_SAMPLE_EVALUATION_PROJECT_NOT_CONFIGURED);
        }
        return project;
    }

    private boolean isEvaluationAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        if (permissionApi.hasAnyRoles(userId, SUPER_ADMIN_ROLE, EVALUATION_ADMIN_ROLE)) {
            return true;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user != null && (HC_ADMIN_USERNAME.equalsIgnoreCase(String.valueOf(user.getUsername()))
                || HC_ADMIN_NICKNAME.equals(user.getNickname()));
    }

    private void assertEvaluationAdmin() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (!isEvaluationAdmin(userId)) {
            throw exception(SRM_EVALUATION_TEMPLATE_ADMIN_REQUIRED);
        }
    }

    private String userName(AdminUserRespDTO user, String fallback) {
        if (user == null) {
            return StrUtil.trim(fallback);
        }
        return StrUtil.blankToDefault(user.getNickname(), user.getUsername());
    }

}
