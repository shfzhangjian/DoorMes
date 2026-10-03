package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNoCounterMapper;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class QmsNoGeneratorServiceImpl implements QmsNoGeneratorService {

    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_DAILY_SEQ = 999;
    private static final long DEFAULT_TENANT_ID = 0L;

    @Resource
    private QmsNoCounterMapper qmsNoCounterMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateNo(String bizType) {
        String normalizedBizType = normalizeBizType(bizType);
        LocalDate bizDate = LocalDate.now(BIZ_ZONE);
        Long tenantId = resolveTenantId();

        qmsNoCounterMapper.insertIgnore(tenantId, normalizedBizType, normalizedBizType, bizDate);
        int updated = qmsNoCounterMapper.incrementDailySeq(tenantId, normalizedBizType, normalizedBizType, bizDate);
        if (updated <= 0) {
            throw invalidParamException("检验单号计数器更新失败，请稍后重试");
        }
        Integer seq = qmsNoCounterMapper.selectLastIncrementSeq();
        if (seq == null || seq <= 0) {
            throw invalidParamException("检验单号计数器返回异常，请稍后重试");
        }
        if (seq > MAX_DAILY_SEQ) {
            throw invalidParamException("当天" + normalizedBizType + "检验单号流水已用尽");
        }

        String orderNo = normalizedBizType + "-" + bizDate.format(DATE_FORMATTER) + "-" + String.format("%03d", seq);
        qmsNoCounterMapper.updateLastOrderNo(tenantId, normalizedBizType, bizDate, orderNo);
        return orderNo;
    }

    private String normalizeBizType(String bizType) {
        if (!StringUtils.hasText(bizType)) {
            throw invalidParamException("检验单号业务类型不能为空");
        }
        return bizType.trim().toUpperCase(Locale.ROOT);
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }
}
