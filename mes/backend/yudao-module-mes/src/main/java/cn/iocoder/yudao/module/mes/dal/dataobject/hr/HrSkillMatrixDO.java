// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/hr/HrSkillMatrixDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.hr;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * 人员工艺技能资质矩阵 DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_hr_skill_matrix")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrSkillMatrixDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工系统ID
     */
    private Long userId;

    /**
     * 取得资质的标准工序ID
     */
    private Long processId;

    /**
     * 技能等级(TRAINEE, OPERATOR, EXPERT)
     */
    private String skillLevel;

    /**
     * 资质到期日
     * 🚨 架构师红线：数据库为 date 类型，精确映射为 LocalDate
     */
    private LocalDate expireDate;

}
