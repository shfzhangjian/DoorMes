// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.hr.HrSkillMatrixServiceImpl.java
package cn.iocoder.yudao.module.mes.service.hr;

import cn.iocoder.yudao.module.mes.dal.dataobject.hr.HrSkillMatrixDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hr.HrSkillMatrixMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.time.LocalDate;

@Service
public class HrSkillMatrixServiceImpl {
    @Resource
    private HrSkillMatrixMapper hrSkillMatrixMapper;

    public void validateOperatorSkill(Long userId, Long processId) {
        HrSkillMatrixDO matrix = hrSkillMatrixMapper.selectOne(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<HrSkillMatrixDO>()
                        .eq(HrSkillMatrixDO::getUserId, userId)
                        .eq(HrSkillMatrixDO::getProcessId, processId)
        );

        // 🚨 架构师红线：LocalDate 精确日期防呆校验
        if (matrix == null || matrix.getExpireDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("当前操作员缺乏该工序资质或资质已过期！");
        }
    }
}
