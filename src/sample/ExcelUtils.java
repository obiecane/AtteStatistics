package sample;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.IntPredicate;

import static sample.MainFrame.DTF_Y_M_D;
import static sample.MainFrame.DTF_Y_M_D_H_M_S;

/**
 * @author Zhu Kaixiao
 * @version 1.0
 * @date 2019/6/4 8:35
 * @copyright 江西金磊科技发展有限公司 All rights reserved. Notice
 * 仅限于授权后使用，禁止非授权传阅以及私自用于商业目的。
 */
public class ExcelUtils {

    private ExcelUtils() {

    }

    public static File genAndWriteExcel(List<MainFrame.WorkRecord> valueList)
            throws IOException {
        //第一步创建workbook
        HSSFWorkbook wb = new HSSFWorkbook();

        //第二步创建sheet
        HSSFSheet sheet = wb.createSheet("考勤记录");
        sheet.setColumnWidth(0, 10 * 256);
        sheet.setColumnWidth(1, 15 * 256);
        sheet.setColumnWidth(2, 25 * 256);
        sheet.setColumnWidth(3, 25 * 256);
        sheet.setColumnWidth(4, 25 * 256);
        sheet.setColumnWidth(5, 100 * 256);

        //第三步创建行row:添加表头0行
        HSSFRow row = sheet.createRow(0);
        HSSFCellStyle style = wb.createCellStyle();
        HSSFCellStyle redStyle = wb.createCellStyle();
        redStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);


        HSSFCellStyle lightGreenStyle = wb.createCellStyle();
        lightGreenStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        lightGreenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        //居中
//        style.setAlignment(HorizontalAlignment.CENTER);

        //第四步创建单元格
        HSSFCell cell = row.createCell(0);
        cell.setCellValue("工号");
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("日期");
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("上班打卡时间");
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("下班打卡时间");
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("加班时长");
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("备注");
        cell.setCellStyle(style);

        //第五步插入数据
        int rowIndex = 1;
        int perBegin = 0;
        for (int i = 0; i < valueList.size(); i++) {
            MainFrame.WorkRecord record = valueList.get(i);
            if (i > 0) {
                MainFrame.WorkRecord prevRecord = valueList.get(i - 1);
                if (!record.getWorkNum().equals(prevRecord.getWorkNum())) {
                    String countDesc = count(valueList.subList(perBegin, i));
                    row = sheet.createRow(rowIndex);
                    HSSFCell cell1 = row.createCell(0);
                    cell1.setCellValue(countDesc);
                    // 这里是合并excel中的单元格
                    sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 5));
                    rowIndex += 2;
                    perBegin = i;
                }
            }

            row = sheet.createRow(rowIndex++);
            HSSFCell cell1 = row.createCell(0);
            cell1.setCellValue(record.getWorkNum());

            HSSFCell cell2 = row.createCell(1);
            cell2.setCellValue(record.getDate().format(DTF_Y_M_D));

            HSSFCell cell3 = row.createCell(2);
            cell3.setCellValue(Optional.ofNullable(record.getGoWork()).map(d -> d.format(DTF_Y_M_D_H_M_S)).orElse("缺卡"));

            HSSFCell cell4 = row.createCell(3);
            cell4.setCellValue(Optional.ofNullable(record.getOffWork()).map(d -> d.format(DTF_Y_M_D_H_M_S)).orElse("缺卡"));

            HSSFCell cell5 = row.createCell(4);
            cell5.setCellValue(Optional.ofNullable(record.getOvertimeDuration()).map(od -> dressMinute((int) Math.ceil (od / 60000.0))).orElse(""));

            if (record.isWorkday()) {
                String desc = getDesc(record);
                row.createCell(5).setCellValue(desc);

                if (desc.contains("迟到") || desc.contains("未打上班卡")) {
                    cell3.setCellStyle(redStyle);
                }
                if (desc.contains("早退") || desc.contains("未打下班卡")) {
                    cell4.setCellStyle(redStyle);
                }
                if (desc.contains("旷工")) {
                    cell3.setCellStyle(redStyle);
                    cell4.setCellStyle(redStyle);
                }
            } else {
                cell1.setCellStyle(lightGreenStyle);
                cell2.setCellStyle(lightGreenStyle);
                cell3.setCellStyle(lightGreenStyle);
                cell4.setCellStyle(lightGreenStyle);
                cell5.setCellStyle(lightGreenStyle);
            }
        }
        String countDesc = count(valueList.subList(perBegin, valueList.size()));
        row = sheet.createRow(rowIndex);
        HSSFCell cell1 = row.createCell(0);
        cell1.setCellValue(countDesc);
        // 这里是合并excel中的单元格
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 5));


        File file = new File("./考勤分析.xls");
        for (int i = 1; file.exists(); i++) {
            file = new File(String.format("./考勤分析 (%d).xls", i));
        }

        wb.write(file);

        return file;
    }


    private static String count(List<MainFrame.WorkRecord> valueList) {
        long sum = valueList.stream().mapToLong(rec ->
                        Optional.of(rec).map(MainFrame.WorkRecord::getOvertimeDuration).orElse(0L)
        ).sum();

        int[] lateArr = valueList.stream().filter(rec -> rec.getLateDuration() != null)
                .mapToInt(MainFrame.WorkRecord::getLateDuration)
                .toArray();

        int a1 = 0, a2 = 0, a3 = 0, a4 = 0;
        int b1 = 0, b2 = 0, b3 = 0, b4 = 0;
        for (int i : lateArr) {
            if (i < 10) {
                ++a1;
                b1 += i;
            } else if (i < 20) {
                ++a2;
                b1 += 10;
                b2 += i - 10;
            } else if (i < 30){
                ++a3;
                b1 += 10;
                b2 += 10;
                b3 += i - 20;
            } else {
                ++a4;
                b1 += 10;
                b2 += 10;
                b3 += 10;
                b4 += i - 30;
            }
        }

        IntSummaryStatistics statistics = valueList.stream().filter(rec -> rec.getCompletion() != null)
                .mapToInt(MainFrame.WorkRecord::getCompletion).summaryStatistics();


        IntSummaryStatistics statistics1 = valueList.stream().filter(rec -> rec.getEarlyDuration() != null)
                .mapToInt(MainFrame.WorkRecord::getEarlyDuration)
                .summaryStatistics();

        StringBuilder sb = new StringBuilder();
        sb.append("统计: \n").append("累计加班时长: ")
                .append(dressMinute((int) Math.ceil(sum / 60000.0))).append("\n");
        if (lateArr.length > 0) {
            sb.append("累计迟到时长: ").append(dressMinute(Arrays.stream(lateArr).sum())).append("  共")
                    .append(lateArr.length).append("次  ").append("其中迟到10分钟及以下共").append(a1)
                    .append("次, 迟到10-20分钟共").append(a2).append("次, 迟到20-30分钟共").append(a3)
                    .append("次, ").append("迟到30分钟以上共").append(a4).append("次;\n").append("迟到时段1-10分钟累计")
                    .append(dressMinute(b1)).append(", 迟到时段10-20分钟累计").append(dressMinute(b2))
                    .append(", 迟到时段20-30分钟累计").append(dressMinute(b3)).append(", 迟到时段30分钟以上累计")
                    .append(dressMinute(b4)).append("\n");
        }
        if (statistics.getCount() > 0) {
            sb.append("累计旷工时长: ").append(statistics.getSum() / 2.0).append("天, 共旷工")
                    .append(statistics.getCount()).append("次");
        }
        if (statistics1.getCount() > 0) {
            sb.append("累计早退时长:").append(dressMinute((int) statistics1.getSum())).append("  共")
                    .append(statistics1.getCount()).append("次");
        }
        return sb.toString();
    }

    private static String getDesc(MainFrame.WorkRecord record) {
        Setting setting = Setting.getSetting();
        // 规定上班时间
        LocalTime workingTime = setting.getStandardWorkingTime();
        // 规定下班时间
        LocalTime offWorkTime = setting.getStandardWorkingTime();

        LocalDate date = record.getDate();
        LocalDateTime goWork = record.getGoWork();
        LocalDateTime offWork = record.getOffWork();

        StringBuilder sb = new StringBuilder();

        // 未打上班卡, 也未打下班卡, 旷工
        if (goWork == null && offWork == null) {
            sb.append(DTF_Y_M_D.format(date)).append("旷工");
            record.setCompletion(2);
            return sb.toString();
        }

        // 未打上班卡但却打了下班卡视为旷工半天(1次)
        if (goWork == null) {
            record.setCompletion(1);
            sb.append(DTF_Y_M_D.format(date)).append("旷工半天(因未打上班卡),");
        }

        // 上班时间晚于规定时间
        if (goWork != null && goWork.toLocalTime().isAfter(workingTime)) {
            int minuteDiff = (goWork.toLocalTime().toSecondOfDay() - workingTime.toSecondOfDay() + 59) / 60;
            // 8:31:02 > 8:30:59, 但是因为不足60秒, 所以会为0, 这种情况不满1分钟也按1分钟计
            minuteDiff = minuteDiff == 0 ? 1 : minuteDiff;
            // 迟到超过30分钟, 视为旷工半天(1次)

            record.setLateDuration(minuteDiff);
            if (minuteDiff > 30) {
                sb.append(DTF_Y_M_D.format(date)).append("旷工半天(因迟到").append(dressMinute(minuteDiff)).append("),");
            } else {
                sb.append(DTF_Y_M_D.format(date)).append("迟到").append(dressMinute(minuteDiff)).append(",");
            }
        }

        // 打了上班卡但是却没打下班卡
        if (offWork == null) {
            sb.append(DTF_Y_M_D.format(date)).append("未打下班卡,");
        }

        // 下班时间早于规定时间
        if (offWork != null && offWork.toLocalTime().isBefore(offWorkTime)) {
            int minuteDiff = (offWorkTime.toSecondOfDay() - offWork.toLocalTime().toSecondOfDay()) / 60;
            record.setEarlyDuration(minuteDiff);
            sb.append(DTF_Y_M_D.format(date)).append("早退").append(dressMinute(minuteDiff)).append(",");
        }

        return sb.length() > 1 ? sb.substring(0, sb.length() - 1) : "";
    }

    /**
     * 把分钟转为x小时y分钟的格式
     *
     * @param minutes 分钟
     * @return java.lang.String
     * @author Zhu Kaixiao
     * @date 2019/6/18 9:24
     **/
    private static String dressMinute(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return h > 0
                ? h + "小时" + m + "分钟"
                : m + "分钟";
    }
}
