package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNoCounterDO;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface QmsNoCounterMapper extends BaseMapperX<QmsNoCounterDO> {

    @Insert("""
            INSERT IGNORE INTO mes_qms_no_counter (
                biz_type, prefix, biz_date, current_seq, last_order_no,
                creator, create_time, updater, update_time, deleted, tenant_id
            ) VALUES (
                #{bizType}, #{prefix}, #{bizDate}, 0, NULL,
                '', NOW(), '', NOW(), b'0', #{tenantId}
            )
            """)
    int insertIgnore(@Param("tenantId") Long tenantId,
                     @Param("bizType") String bizType,
                     @Param("prefix") String prefix,
                     @Param("bizDate") LocalDate bizDate);

    @Update("""
            UPDATE mes_qms_no_counter
            SET current_seq = LAST_INSERT_ID(current_seq + 1),
                prefix = #{prefix},
                updater = '',
                update_time = NOW()
            WHERE tenant_id = #{tenantId}
              AND biz_type = #{bizType}
              AND biz_date = #{bizDate}
              AND deleted = b'0'
            """)
    int incrementDailySeq(@Param("tenantId") Long tenantId,
                          @Param("bizType") String bizType,
                          @Param("prefix") String prefix,
                          @Param("bizDate") LocalDate bizDate);

    @Select("SELECT LAST_INSERT_ID()")
    Integer selectLastIncrementSeq();

    @Update("""
            UPDATE mes_qms_no_counter
            SET last_order_no = #{lastOrderNo},
                updater = '',
                update_time = NOW()
            WHERE tenant_id = #{tenantId}
              AND biz_type = #{bizType}
              AND biz_date = #{bizDate}
              AND deleted = b'0'
            """)
    int updateLastOrderNo(@Param("tenantId") Long tenantId,
                          @Param("bizType") String bizType,
                          @Param("bizDate") LocalDate bizDate,
                          @Param("lastOrderNo") String lastOrderNo);
}
