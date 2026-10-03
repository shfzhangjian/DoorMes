// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/andon/AndonRecordDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.andon;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.Map;

/**
 * 现场Andon异常响应与呼叫 DO
 *
 * @author 资深后端架构智能体
 */
@TableName(value = "mes_andon_record", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AndonRecordDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 安灯呼叫单号
     */
    private String andonNo;

    /**
     * 类别(MATERIAL, QUALITY, EQUIPMENT, QTIME)
     */
    private String exceptionType;

    /**
     * 等级(LOW, MEDIUM, HIGH, CRITICAL)
     */
    private String severityLevel;

    /**
     * 故障机台ID
     */
    private Long equipmentId;

    /**
     * 关联派工细单ID
     */
    private Long subOrderId;

    /**
     * 状态(UNPROCESSED, PROCESSING, RECOVERED)
     */
    private String status;

    /**
     * 响应处理人
     */
    private String handlerUser;

    /**
     * AI智能辅助归因与排故建议预留
     * 🚨 架构师红线：强制使用 JacksonTypeHandler 映射 JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> aiAnalysisResult;

}
