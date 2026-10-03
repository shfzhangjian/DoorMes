package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 仅解析业务姓名，不修改来源记录或按 creator 猜测记录人。 */
@Component
public class HcProductionRecorderNameResolver {
    @Resource
    private AdminUserMapper adminUserMapper;

    /** 保留 Mapper 的租户和逻辑删除约束，包含停用账号以兼容历史记录。 */
    public Names loadNames() {
        Map<String, String> aliases = new LinkedHashMap<>();
        for (AdminUserDO user : adminUserMapper.selectList(new LambdaQueryWrapperX<AdminUserDO>()
                .select(AdminUserDO::getUsername, AdminUserDO::getNickname))) {
            if (StrUtil.isNotBlank(user.getUsername()) && StrUtil.isNotBlank(user.getNickname())) {
                aliases.put(user.getUsername().trim(), user.getNickname().trim());
            }
        }
        return new Names(aliases);
    }

    public String resolveForSave(String name) {
        if (StrUtil.isBlank(name)) {
            return name;
        }
        AdminUserDO user = adminUserMapper.selectByUsername(name.trim());
        return user != null && StrUtil.isNotBlank(user.getNickname()) ? user.getNickname().trim() : name.trim();
    }

    public static final class Names {
        private final Map<String, String> aliases;

        Names(Map<String, String> aliases) {
            this.aliases = aliases;
        }

        public String normalize(String names) {
            if (StrUtil.isBlank(names)) {
                return names;
            }
            return Arrays.stream(names.split("[；;]"))
                    .map(String::trim).filter(StrUtil::isNotBlank)
                    .map(name -> aliases.getOrDefault(name, name))
                    .collect(Collectors.toCollection(LinkedHashSet::new)).stream()
                    .collect(Collectors.joining("；"));
        }

        public boolean matches(String names, String query) {
            if (StrUtil.isBlank(query)) {
                return true;
            }
            String normalized = names;
            if (StrUtil.containsIgnoreCase(normalized, query.trim())) {
                return true;
            }
            return aliases.entrySet().stream().anyMatch(entry ->
                    StrUtil.containsIgnoreCase(entry.getKey(), query.trim())
                    && normalized != null
                    && Arrays.asList(normalized.split("；")).contains(entry.getValue()));
        }
    }
}
