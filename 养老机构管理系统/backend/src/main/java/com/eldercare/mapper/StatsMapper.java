package com.eldercare.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 统计查询 Mapper：看板数据直接使用聚合 SQL
 */
public interface StatsMapper {

    /** 老人总数（含已退住） */
    @Select("SELECT COUNT(*) FROM elder_info")
    long countElderTotal();

    /** 在住老人数 */
    @Select("SELECT COUNT(*) FROM elder_info WHERE status = 1")
    long countInHouse();

    /** 在住老人占用的房间数（房间号去重） */
    @Select("SELECT COUNT(DISTINCT room_no) FROM elder_info WHERE status = 1 AND room_no IS NOT NULL")
    long countUsedRooms();

    /** 今日护理次数 */
    @Select("SELECT COUNT(*) FROM care_record WHERE care_time >= CURDATE() AND care_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY)")
    long countTodayCare();

    /** 今日探访次数 */
    @Select("SELECT COUNT(*) FROM visit_appointment WHERE visit_date = CURDATE()")
    long countTodayVisit();

    /** 逾期未执行用药任务数（状态仍为待执行且日期早于今天） */
    @Select("SELECT COUNT(*) FROM medicine_plan WHERE plan_date < CURDATE() AND status = 0")
    long countOverdueTask();

    /** 老人年龄分布（按年龄段分组） */
    @Select("SELECT CASE " +
            "WHEN TIMESTAMPDIFF(YEAR, birthday, CURDATE()) <= 69 THEN '60-69' " +
            "WHEN TIMESTAMPDIFF(YEAR, birthday, CURDATE()) <= 79 THEN '70-79' " +
            "WHEN TIMESTAMPDIFF(YEAR, birthday, CURDATE()) <= 89 THEN '80-89' " +
            "ELSE '90+' END AS ageGroup, COUNT(*) AS cnt " +
            "FROM elder_info WHERE birthday IS NOT NULL GROUP BY ageGroup")
    List<Map<String, Object>> selectAgeDistribution();

    /** 近 N 天护理次数（按天分组） */
    @Select("SELECT DATE_FORMAT(care_time, '%Y-%m-%d') AS day, COUNT(*) AS cnt " +
            "FROM care_record " +
            "WHERE care_time >= DATE_SUB(CURDATE(), INTERVAL (#{days} - 1) DAY) " +
            "GROUP BY DATE_FORMAT(care_time, '%Y-%m-%d')")
    List<Map<String, Object>> selectCareTrend(@Param("days") int days);

    /** 近 N 天探访次数（按天分组） */
    @Select("SELECT DATE_FORMAT(visit_date, '%Y-%m-%d') AS day, COUNT(*) AS cnt " +
            "FROM visit_appointment " +
            "WHERE visit_date >= DATE_SUB(CURDATE(), INTERVAL (#{days} - 1) DAY) " +
            "GROUP BY DATE_FORMAT(visit_date, '%Y-%m-%d')")
    List<Map<String, Object>> selectVisitTrend(@Param("days") int days);

    /** 某老人近 N 天体征记录（按时间升序） */
    @Select("SELECT DATE_FORMAT(record_time, '%Y-%m-%d') AS day, blood_pressure, heart_rate, temperature, blood_sugar " +
            "FROM health_record " +
            "WHERE elder_id = #{elderId} AND record_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "ORDER BY record_time ASC")
    List<Map<String, Object>> selectHealthTrend(@Param("elderId") Long elderId, @Param("days") int days);
}