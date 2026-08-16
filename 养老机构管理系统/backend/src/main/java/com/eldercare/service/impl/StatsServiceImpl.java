package com.eldercare.service.impl;

import com.eldercare.common.BusinessException;
import com.eldercare.entity.ElderInfo;
import com.eldercare.mapper.ElderInfoMapper;
import com.eldercare.mapper.StatsMapper;
import com.eldercare.security.UserContext;
import com.eldercare.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计看板业务实现：年龄分布、近 30 天趋势、体征趋势
 */
@Service
public class StatsServiceImpl implements StatsService {

    @Autowired
    private StatsMapper statsMapper;

    @Autowired
    private ElderInfoMapper elderInfoMapper;

    /** 体征趋势可选的指标 */
    private static final List<String> SUPPORTED_METRICS =
            List.of("bloodPressure", "heartRate", "temperature", "bloodSugar");

    @Override
    public Map<String, Object> getOverview() {
        long elderTotal = statsMapper.countElderTotal();
        long inHouse = statsMapper.countInHouse();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("elderTotal", elderTotal);
        map.put("inHouse", inHouse);
        // 房间数用"在住老人占用的去重房间数"表示（系统未单独建房间表）
        map.put("roomTotal", statsMapper.countUsedRooms());
        // 入住率 = 在住数 / 老人总数
        double rate = elderTotal == 0 ? 0 : BigDecimal.valueOf(inHouse * 100.0 / elderTotal)
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
        map.put("checkInRate", rate);
        map.put("todayCareCount", statsMapper.countTodayCare());
        map.put("todayVisitCount", statsMapper.countTodayVisit());
        map.put("overdueTaskCount", statsMapper.countOverdueTask());
        return map;
    }

    @Override
    public Map<String, Object> getAgeDistribution() {
        // 固定年龄段顺序，数据库没查到某段的补 0
        List<String> categories = List.of("60-69", "70-79", "80-89", "90+");
        Map<String, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : statsMapper.selectAgeDistribution()) {
            countMap.put(String.valueOf(row.get("ageGroup")),
                    ((Number) row.get("cnt")).longValue());
        }
        List<Long> counts = new ArrayList<>();
        for (String category : categories) {
            counts.add(countMap.getOrDefault(category, 0L));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", categories);
        result.put("counts", counts);
        return result;
    }

    @Override
    public Map<String, Object> getActivityTrend(int days) {
        if (days <= 0 || days > 90) {
            throw new BusinessException(400, "天数需在 1~90 之间");
        }
        // 生成从 (今天-days+1) 到 今天的日期序列
        List<String> dates = new ArrayList<>();
        Map<String, Long> careMap = new HashMap<>();
        Map<String, Long> visitMap = new HashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            dates.add(LocalDate.now().minusDays(i).toString());
        }
        for (Map<String, Object> row : statsMapper.selectCareTrend(days)) {
            careMap.put(String.valueOf(row.get("day")), ((Number) row.get("cnt")).longValue());
        }
        for (Map<String, Object> row : statsMapper.selectVisitTrend(days)) {
            visitMap.put(String.valueOf(row.get("day")), ((Number) row.get("cnt")).longValue());
        }
        // 无数据的日期补 0
        List<Long> careCounts = new ArrayList<>();
        List<Long> visitCounts = new ArrayList<>();
        for (String date : dates) {
            careCounts.add(careMap.getOrDefault(date, 0L));
            visitCounts.add(visitMap.getOrDefault(date, 0L));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("careCounts", careCounts);
        result.put("visitCounts", visitCounts);
        return result;
    }

    @Override
    public Map<String, Object> getHealthTrend(Long elderId, int days, String metric) {
        if (!SUPPORTED_METRICS.contains(metric)) {
            throw new BusinessException(400, "不支持的体征指标");
        }
        if (days <= 0 || days > 90) {
            throw new BusinessException(400, "天数需在 1~90 之间");
        }
        // 家属只能看关联老人的趋势
        if ("family".equals(UserContext.getRole())) {
            ElderInfo elder = elderInfoMapper.selectById(elderId);
            if (elder == null || elder.getFamilyId() == null
                    || !elder.getFamilyId().equals(UserContext.getUserId())) {
                throw new BusinessException(403, "无权访问该老人的数据");
            }
        }
        List<String> dates = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map<String, Object> row : statsMapper.selectHealthTrend(elderId, days)) {
            dates.add(String.valueOf(row.get("day")));
            Object value = switch (metric) {
                case "heartRate" -> row.get("heart_rate");
                case "temperature" -> row.get("temperature");
                case "bloodSugar" -> row.get("blood_sugar");
                default -> row.get("blood_pressure"); // bloodPressure 为 "128/82" 字符串
            };
            values.add(value == null ? "" : value);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("values", values);
        return result;
    }

    @Override
    public List<String> getSupportedMetrics() {
        return SUPPORTED_METRICS;
    }
}