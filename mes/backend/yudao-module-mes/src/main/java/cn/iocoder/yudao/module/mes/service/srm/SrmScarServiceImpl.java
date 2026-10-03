package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmScarPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmScarRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmScarSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmScarDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmScarMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SCAR_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SCAR_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_VERSION_CONFLICT;

@Service
@Validated
public class SrmScarServiceImpl implements SrmScarService {

    private static final String STATUS_WAIT_SUPPLIER = "WAIT_SUPPLIER";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String SCAR_NO_PREFIX = "SCAR-";
    private static final DateTimeFormatter SCAR_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyyMMdd");
    private static final int SCAR_NO_DAILY_SEQUENCE_LIMIT = 999;
    private static final int INITIAL_VERSION = 0;

    @Resource
    private SrmScarMapper scarMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createScar(SrmScarSaveReqVO reqVO) {
        String scarNo = generateScarNo();
        validateScarNoUnique(null, scarNo);
        SrmScarDO scar = BeanUtils.toBean(reqVO, SrmScarDO.class);
        scar.setId(null);
        scar.setScarNo(scarNo);
        scar.setStatus(resolveStatus(reqVO));
        scar.setApplyTime(LocalDateTime.now());
        fillInitiatorDefaults(scar, null);
        scar.setVersion(INITIAL_VERSION);
        scarMapper.insert(scar);
        return scar.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScar(SrmScarSaveReqVO reqVO) {
        SrmScarDO old = validateScarExists(reqVO.getId());
        SrmScarDO updateObj = BeanUtils.toBean(reqVO, SrmScarDO.class);
        updateObj.setScarNo(old.getScarNo());
        updateObj.setStatus(resolveStatus(reqVO));
        fillInitiatorDefaults(updateObj, old);
        updateObj.setApplyTime(old.getApplyTime());
        updateObj.setVersion(reqVO.getVersion());
        if (scarMapper.updateById(updateObj) == 0) {
            throw exception(SRM_VERSION_CONFLICT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScar(Long id) {
        validateScarExists(id);
        scarMapper.deleteById(id);
    }

    @Override
    public SrmScarRespVO getScar(Long id) {
        return BeanUtils.toBean(validateScarExists(id), SrmScarRespVO.class);
    }

    @Override
    public PageResult<SrmScarRespVO> getScarPage(SrmScarPageReqVO reqVO) {
        return BeanUtils.toBean(scarMapper.selectPage(reqVO), SrmScarRespVO.class);
    }

    /**
     * 状态自动管理：已填写异常回复（回复日期且回复说明非空）视为已整改关闭，否则待供方回复。
     */
    private String resolveStatus(SrmScarSaveReqVO reqVO) {
        if (reqVO.getReplyDate() != null && StrUtil.isNotBlank(reqVO.getReplyDesc())) {
            return STATUS_CLOSED;
        }
        return STATUS_WAIT_SUPPLIER;
    }

    private SrmScarDO validateScarExists(Long id) {
        SrmScarDO scar = scarMapper.selectById(id);
        if (scar == null) {
            throw exception(SRM_SCAR_NOT_EXISTS);
        }
        return scar;
    }

    private void validateScarNoUnique(Long id, String scarNo) {
        if (StrUtil.isBlank(scarNo)) {
            return;
        }
        SrmScarDO scar = scarMapper.selectByScarNo(scarNo);
        if (scar != null && !scar.getId().equals(id)) {
            throw exception(SRM_SCAR_NO_EXISTS);
        }
    }

    private String generateScarNo() {
        String dailyPrefix = SCAR_NO_PREFIX + LocalDate.now().format(SCAR_NO_DATE_FORMATTER);
        for (int sequence = 1; sequence <= SCAR_NO_DAILY_SEQUENCE_LIMIT; sequence++) {
            String scarNo = dailyPrefix + String.format(Locale.ROOT, "%03d", sequence);
            if (scarMapper.selectByScarNo(scarNo) == null) {
                return scarNo;
            }
        }
        throw exception(SRM_SCAR_NO_EXISTS);
    }

    private void fillInitiatorDefaults(SrmScarDO scar, SrmScarDO old) {
        Initiator initiator = getCurrentInitiator();
        scar.setApplicantId(old == null
                ? defaultLong(initiator.userId(), scar.getApplicantId())
                : defaultLong(old.getApplicantId(), defaultLong(initiator.userId(), scar.getApplicantId())));
        scar.setApplicantName(old == null
                ? defaultString(initiator.userName(), scar.getApplicantName())
                : defaultString(old.getApplicantName(), defaultString(initiator.userName(), scar.getApplicantName())));
    }

    private Initiator getCurrentInitiator() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userName = SecurityFrameworkUtils.getLoginUserNickname();
        Long deptId = SecurityFrameworkUtils.getLoginUserDeptId();
        if (userId != null && (StrUtil.isBlank(userName) || deptId == null)) {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            if (user != null) {
                userName = defaultString(userName, user.getNickname());
                deptId = deptId == null ? user.getDeptId() : deptId;
            }
        }
        String deptName = null;
        if (deptId != null) {
            DeptRespDTO dept = deptApi.getDept(deptId);
            deptName = dept == null ? null : dept.getName();
        }
        return new Initiator(userId, userName, deptName);
    }

    private record Initiator(Long userId, String userName, String deptName) {
    }

    private static String defaultString(String value, String defaultValue) {
        return StrUtil.isBlank(value) ? defaultValue : value;
    }

    private static Long defaultLong(Long value, Long defaultValue) {
        return value == null ? defaultValue : value;
    }

}