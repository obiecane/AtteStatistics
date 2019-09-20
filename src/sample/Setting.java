package sample;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Zhu Kaixiao
 * @version 1.0
 * @date 2019/9/20 14:04
 * @copyright 江西金磊科技发展有限公司 All rights reserved. Notice
 * 仅限于授权后使用，禁止非授权传阅以及私自用于商业目的。
 */
public class Setting {

    private Setting(){}

    private static class SingletonHandler {
        private static Setting singleton = new Setting();
    }

    public static Setting getSetting() {
        return SingletonHandler.singleton;
    }

    /** 上班时间 */
    private String standardWorkingTimeStr = "08:30";
    /** 下班时间 */
    private String standardOffWorkTimeStr = "18:00";
    /** 加班起始时间 */
    private String standardOvertimeStr = "18:00";


    public String getStandardWorkingTimeStr() {
        return standardWorkingTimeStr;
    }

    public void setStandardWorkingTimeStr(String standardWorkingTimeStr) {
        this.standardWorkingTimeStr = standardWorkingTimeStr;
    }

    public String getStandardOffWorkTimeStr() {
        return standardOffWorkTimeStr;
    }

    public void setStandardOffWorkTimeStr(String standardOffWorkTimeStr) {
        this.standardOffWorkTimeStr = standardOffWorkTimeStr;
    }

    public String getStandardOvertimeStr() {
        return standardOvertimeStr;
    }

    public void setStandardOvertimeStr(String standardOvertimeStr) {
        this.standardOvertimeStr = standardOvertimeStr;
    }


    //
    private static final DateTimeFormatter HH_mm_format = DateTimeFormatter.ofPattern("HH:mm");


    private static Map<String, LocalTime> workingTimeCache = new ConcurrentHashMap<>();
    private static Map<String, LocalTime> cache = new ConcurrentHashMap<>();

    public synchronized LocalTime getStandardWorkingTime() {
        LocalTime time = workingTimeCache.get(standardWorkingTimeStr);
        if (time == null) {
            String s = "0" + standardWorkingTimeStr;
            time = LocalTime.parse(s.substring(s.length() - 5), HH_mm_format);
            time = time.plusSeconds(59);
            workingTimeCache.put(standardWorkingTimeStr, time);
        }
        return time;
    }

    public synchronized LocalTime getStandardOffWorkTime() {
        return getCache(standardOffWorkTimeStr);
    }

    public LocalTime getStandardOvertime() {
        return getCache(standardOvertimeStr);
    }

    private static LocalTime getCache(String timeStr) {
        LocalTime time = cache.get(timeStr);
        if (time == null) {
            String s = "0" + timeStr;
            time = LocalTime.parse(s.substring(s.length() - 5), HH_mm_format);
            cache.put(timeStr, time);
        }
        return time;
    }
}
