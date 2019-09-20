package sample;

import sample.utils.StringUtils;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.lang.reflect.Field;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FontUIResource;


/**
 * 主界面类
 *
 * @author zak
 */
public class MainFrame extends JFrame {

    private static final String VERSION = "v1.3.0";

    private JTextField fileTextField;
    private JTextField datesTextField;
    private JTextArea notifyArea;
    private List<LocalDate> datesList = Collections.emptyList();
    private JPanel settingPanel;
    private JPanel aboutPanel;
    private Setting setting = Setting.getSetting();

    public MainFrame() {
        String msg = null;
        try {
            readSetting();
            initGlobalFont();
            initMenuPanel();
            initMenu();
        } catch (Exception e) {
            msg = e.getMessage();
        }

        Box mainBox = Box.createVerticalBox();
        Box fBox = Box.createHorizontalBox();
        Box dBox = Box.createHorizontalBox();
        JPanel p1 = new JPanel();
        JButton dateBtn = new JButton("工作日期");
        dateBtn.setFocusPainted(false);
        DateChooser dateChooser1 = DateChooser.getInstance();
        dateChooser1.register(dateBtn);

        dateChooser1.addListener(new DateChooser.DateChoosedListener() {
            @Override
            public void dateChoosed(DateChooser dateChooser) {
                datesTextField.setText(dateChooser.getStrDate());
                datesList = new ArrayList<>(dateChooser.getSelectedDates());
            }

            @Override
            public void dateCleared(DateChooser dateChooser) {
                datesTextField.setText("");
                datesList.clear();
            }
        });

        p1.add(dateBtn);
        fileTextField = new JTextField(30);
        fBox.add(fileTextField);
        JButton fileBtn = new JButton("选择文件");
        fileBtn.setFocusPainted(false);

        fileBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFileChooser jfc = new JFileChooser();
                jfc.setFileFilter(new FileFilter() {
                    private java.util.List<String> fileTypes = Arrays.asList("txt");

                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        String name = f.getName();
                        int p = name.lastIndexOf(".");
                        if (p == -1) {
                            return false;
                        }
                        String suffix = name.substring(p + 1).toLowerCase();
                        return fileTypes.contains(suffix);
                    }

                    @Override
                    public String getDescription() {
                        List<String> tmp = fileTypes.stream().map(s -> "*." + s).collect(Collectors.toList());
                        StringBuilder sb = new StringBuilder();
                        for (String s : tmp) {
                            sb.append(s).append("; ");
                        }
                        return sb.substring(0, sb.length() - 2);
                    }

                });
                jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                jfc.showDialog(new JLabel(), "选择");
                File file = jfc.getSelectedFile();
                if (file != null && file.isFile()) {
                    fileTextField.setText(file.getAbsolutePath());
                }
            }

            @Override
            public void mouseEntered(MouseEvent me) {
                if (fileBtn.isEnabled()) {
                    fileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    fileBtn.setForeground(Color.RED);
                }
            }

            @Override
            public void mouseExited(MouseEvent me) {
                if (fileBtn.isEnabled()) {
                    fileBtn.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    fileBtn.setForeground(Color.BLACK);
                }
            }


            @Override
            public void mousePressed(MouseEvent me) {
                if (fileBtn.isEnabled()) {
                    fileBtn.setForeground(Color.CYAN);
                }
            }


            @Override
            public void mouseReleased(MouseEvent me) {
                if (fileBtn.isEnabled()) {
                    fileBtn.setForeground(Color.BLACK);
                }
            }

        });

        JPanel p2 = new JPanel();
        p2.add(fileBtn);
        fBox.add(p2);
        datesTextField = new JTextField(30);
        datesTextField.setEditable(false);
        datesTextField.setBackground(Color.WHITE);
        datesTextField.setBorder(BorderFactory.createLineBorder(new Color(122,138,153),1));

        dBox.add(datesTextField);
        dBox.add(p1);
        mainBox.add(fBox);
        mainBox.add(Box.createVerticalStrut(10));
        mainBox.add(dBox);
        mainBox.add(Box.createVerticalStrut(10));
        Box b = Box.createHorizontalBox();
        JButton button = new JButton("开始分析");


        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String filename = fileTextField.getText();
                File file = new File(filename);

                if (filename.trim().length() == 0) {
                    notifyArea.append(String.format("[%s] 请选择文件!\n", LocalDateTime.now().format(DTF_Y_M_D_H_M_S)));
                    return;
                }

                if (datesList.isEmpty()) {
                    notifyArea.append(String.format("[%s] 请选择工作日期!\n", LocalDateTime.now().format(DTF_Y_M_D_H_M_S)));
                    return;
                }

                if (!file.exists()) {
                    notifyArea.append(String.format("[%s] 文件[%s]不存在!\n", LocalDateTime.now().format(DTF_Y_M_D_H_M_S), filename));
                    return;
                }

                notifyArea.append(String.format("[%s] 正在分析打卡数据, 请稍候...\n", LocalDateTime.now().format(DTF_Y_M_D_H_M_S)));


                try {
                    // 指纹打卡记录
                    Map<Integer, List<LocalDateTime>> checkInMap = fetchCheckIn(file);
                    // 上下班打卡记录
                    Map<Integer, List<WorkRecord>> workRecordMap = resolveWorkLog(checkInMap, datesList);
//                    // 考勤统计
//                    Map<Integer, AtteStatistics> statisticsMap = analysisAtteStatisticsMap(workRecordMap);

                    List<WorkRecord> list = new LinkedList<>();
                    workRecordMap.values().forEach(list::addAll);
                    list = list.stream()
                            .sorted((o1, o2) -> {
                                int c1 = o1.getWorkNum() - o2.getWorkNum();
                                int c2 = o1.getDate().compareTo(o2.getDate());
                                return c1 * 1000000 + c2;
                            })
                            .collect(Collectors.toList());

                    notifyArea.append(String.format("[%s] 正在生成Excel, 请稍候...\n", LocalDateTime.now().format(DTF_Y_M_D_H_M_S)));
                    File excel = ExcelUtils.genAndWriteExcel(list);
                    notifyArea.append(String.format("[%s] Excel生成完成, 文件位置:[%s]\n", LocalDateTime.now().format(DTF_Y_M_D_H_M_S), excel.getCanonicalPath()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    notifyArea.append(String.format("[%s] %s",LocalDateTime.now().format(DTF_Y_M_D_H_M_S), ex.getMessage()));
                }
            }

            @Override
            public void mouseEntered(MouseEvent me) {
                if (button.isEnabled()) {
                    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    button.setForeground(Color.RED);
                }
            }

            @Override
            public void mouseExited(MouseEvent me) {
                if (button.isEnabled()) {
                    button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    button.setForeground(Color.BLACK);
                }
            }

            @Override
            public void mousePressed(MouseEvent me) {
                if (button.isEnabled()) {
                    button.setForeground(Color.CYAN);
                }
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                if (button.isEnabled()) {
                    button.setForeground(Color.BLACK);
                }
            }

        });

        JPanel p3 = new JPanel();
        p3.add(button);
        b.add(Box.createHorizontalGlue());
        b.add(button);
        b.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        button.setFocusPainted(false);
        mainBox.add(b);

        mainBox.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        Box labelBox = Box.createVerticalBox();
        labelBox.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        notifyArea = new JTextArea();
        notifyArea.setLineWrap(true);
        notifyArea.setEditable(false);
        notifyArea.setFont(new Font("宋体", Font.PLAIN, 15));
        if (msg != null) {
            notifyArea.append(String.format("[%s] %s",LocalDateTime.now().format(DTF_Y_M_D_H_M_S), msg));
        }
        labelBox.add(new JScrollPane(notifyArea));

        JPanel pp = new JPanel();
        pp.setLayout(new BorderLayout());
        pp.add(mainBox, BorderLayout.NORTH);
        pp.add(labelBox, BorderLayout.CENTER);
        this.add(pp, BorderLayout.CENTER);

        this.setBounds(0, 0, 700, 500);
        this.setLocationRelativeTo(null);
        this.setTitle("考勤统计 " + VERSION);
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }


    private static void initGlobalFont(){
        FontUIResource fontUIResource = new FontUIResource(new Font("宋体",Font.PLAIN, 13));
        for (Enumeration keys = UIManager.getDefaults().keys(); keys.hasMoreElements();) {
            Object key = keys.nextElement();
            Object value= UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontUIResource);
            }
        }
    }

    private void initMenuPanel() {
        aboutPanel = new JPanel();
        Box aboutBox = Box.createVerticalBox();
        aboutBox.add(new JLabel("作者: zak"));
        aboutBox.add(new JLabel("版本: " + VERSION));
        aboutBox.add(Box.createVerticalStrut(5000));
        aboutPanel.add(aboutBox);

        settingPanel = new JPanel();
        settingPanel.setLayout(new BorderLayout());
        Box settingBox = Box.createVerticalBox();
        Box box1 = Box.createHorizontalBox();
        box1.add(new JLabel("上班时间:　　"));
        JTextField t1 = new JTextField(10);
        t1.setText(setting.getStandardWorkingTimeStr());
        box1.add(t1);

        Box box2 = Box.createHorizontalBox();
        box2.add(new JLabel("下班时间:　　"));
        JTextField t2 = new JTextField(10);
        t2.setText(setting.getStandardOffWorkTimeStr());
        box2.add(t2);

        Box box3 = Box.createHorizontalBox();
        box3.add(new JLabel("加班起始时间:"));
        JTextField t3 = new JTextField(10);
        t3.setText(setting.getStandardOvertimeStr());
        box3.add(t3);

        settingBox.add(box1);
        settingBox.add(box2);
        settingBox.add(box3);
        settingBox.add(Box.createVerticalStrut(5000));
        settingPanel.add(settingBox, BorderLayout.CENTER);
        JButton saveSettingButton = new JButton("保存");
        saveSettingButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setting.setStandardWorkingTimeStr(t1.getText());
                setting.setStandardOffWorkTimeStr(t2.getText());
                setting.setStandardOvertimeStr(t3.getText());
                saveSetting();

                Container par = settingPanel;
                do {
                    par = par.getParent();
                } while (par != null && !(par instanceof DDialog));

                if (par != null) {
                    DDialog d = (DDialog) par;
                    d.dispose();
                }
            }
        });
        settingPanel.add(saveSettingButton, BorderLayout.SOUTH);
    }

    private void initMenu() {
        //创建菜单栏
        JMenuBar menuBar = new JMenuBar();
        //创建菜单
        JMenu setting=new JMenu("设置");
        setting.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // show dialog window
                DDialog.showDialog("设置", MainFrame.this, settingPanel);
            }
        });

        JMenu about=new JMenu("关于");
        about.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                DDialog.showDialog("关于", MainFrame.this, aboutPanel);
            }
        });
        menuBar.add(setting);
        menuBar.add(about);
        this.add(menuBar,BorderLayout.NORTH);
    }


    private void readSetting() {
        File baseDir = new File(System.getProperty("user.home"));
        File settingFile = new File(baseDir, ".jeecms-attendance-analysis.conf");
        if (!settingFile.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(settingFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int si = line.indexOf("=");
                if (si < 1 || si == line.length() - 1) {
                    continue;
                }
                String key = line.substring(0, si);
                String value = line.substring(si + 1);

                try {
                    Field field = Setting.class.getDeclaredField(key);
                    field.setAccessible(true);
                    field.set(setting, value);
                } catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("读取设置失败");
        }
    }

    private void saveSetting() {
        File baseDir = new File(System.getProperty("user.home"));
        File settingFile = new File(baseDir, ".jeecms-attendance-analysis.conf");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(settingFile))) {
            Field[] fields = Setting.class.getDeclaredFields();
            Field.setAccessible(fields, true);
            for (Field field : fields) {
                writer.write(field.getName() + "=" + field.get(setting) + "\n");
            }
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("保存设置失败");
        }
    }

    /**
     * 从文件中提取打卡信息
     *
     * @param file      文件
     * @return java.util.Map<java.lang.Integer, java.util.List < java.time.LocalDateTime>>
     * @author Zhu Kaixiao
     * @date 2019/6/18 9:09
     **/
    private Map<Integer, List<LocalDateTime>> fetchCheckIn(File file) throws Exception {
        Map<Integer, List<LocalDateTime>> ret = new HashMap<>(64);
        Set<Integer> workNumSet = new HashSet<>(32);

        String line;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            while ((line = r.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                Matcher matcher = PATTERN_CK_IN.matcher(line);
                if (matcher.find()) {
                    LocalDateTime datetime = LocalDateTime.parse(matcher.group(2), DTF_CK_IN);
                    int workNum = Integer.parseInt(matcher.group(1));
                    workNumSet.add(workNum);
                    ret.putIfAbsent(workNum, new LinkedList<>());
                    List<LocalDateTime> dates = ret.computeIfAbsent(workNum, integer -> new LinkedList<>());
                    dates.add(datetime);

                } else {
                    throw new Exception("正则表达式匹配失败!");
                }
            }
        }

        if (ret.isEmpty()) {
            workNumSet.stream()
                    .sorted()
                    .forEach(wn -> ret.put(wn, Collections.emptyList()));
        }

        return ret;
    }


    /**
     * 解析员工每天的上班打卡时间和下班打卡时间
     * 6:00(含) - 18:00(不含) 取最早的一次打卡记录为上班卡
     * 8:30(不含) - 次日6:00(不含)  最晚的一次打卡(且晚于上班打卡时间)为下班卡
     *
     * @param map map
     * @return java.util.Map<java.lang.Integer, java.util.List < Scratch.WorkRecord>>
     * @author Zhu Kaixiao
     * @date 2019/6/17 11:46
     **/
    private Map<Integer, List<WorkRecord>> resolveWorkLog(Map<Integer, List<LocalDateTime>> map, List<LocalDate> workTimes) {
        Map<Integer, List<WorkRecord>> ret = new HashMap<>(32);

        Map<Integer, List<LocalDate>> workDayMap = workTimes.stream()
                .collect(Collectors.groupingBy(LocalDate::getDayOfYear));

        for (Map.Entry<Integer, List<LocalDateTime>> entry : map.entrySet()) {
            Integer workNum = entry.getKey();
            List<LocalDateTime> value = entry.getValue();

            // 按天分组
            Map<Integer, List<LocalDateTime>> punchDaily = value.stream().
                    // 往前偏移6小时, 使得跨天的日期都成为不跨天, 方便按天分组
                            map(l -> l.plusHours(-6)).
                            collect(Collectors.groupingBy(LocalDateTime::getDayOfYear));
            Set<Integer> punchDailyKeySet = punchDaily.keySet();
            List<WorkRecord> workRecords = ret.computeIfAbsent(workNum, i -> new ArrayList<>(punchDaily.size()));

            Set<Integer> allDaySet = new HashSet<>(punchDailyKeySet);
            allDaySet.addAll(workDayMap.keySet());

            for (Integer dayOfYear : allDaySet) {
                WorkRecord workRecord = new WorkRecord();
                if (workDayMap.containsKey(dayOfYear)) {
                    // 迭代规定上班时间
                    LocalDate workDate = workDayMap.get(dayOfYear).get(0);
                    workRecord.setWorkday(true);
                    workRecord.setWorkNum(workNum);
                    workRecord.setDate(workDate);
                    workRecords.add(workRecord);

                    if (punchDailyKeySet.contains(dayOfYear)) {
                        List<LocalDateTime> punchTime = punchDaily.get(dayOfYear);
                        // 当天最早打卡
                        Optional<LocalDateTime> min = punchTime.stream().min(Comparator.naturalOrder());
                        // 当天最晚打卡
                        Optional<LocalDateTime> max = punchTime.stream().max(Comparator.naturalOrder());
                        if (min.isPresent()) {
                            LocalDateTime goWork = min.get();
                            // 只有在18点之前的打卡记录才被认为是打上班卡
                            if (goWork.getHour() < 12) {
                                workRecord.setGoWork(goWork.plusHours(6));
                            }
                        }
                        if (max.isPresent()) {
                            LocalDateTime offWork = max.get();
                            // 不等于上班卡时间, 且晚于8:30:59,
                            // 而且最少比上班卡时间晚5分钟(如果迟到了的时候, 打卡按住不放(重复打上班卡),
                            // 又刚好这天忘记打下班卡, 那么就会把重复的上班卡记录的最后一次误判为下班卡;
                            // 最少比上班卡时间晚5分钟就是为了避免这种情况)
                            // 则被认为打的是下班卡
                            // 之所以是 > 9059, 是因为8:30:59往前偏移了6小时, 则是2:30:59
                            // 2:30:59从当天0:00起为 9059秒
                            int offSecs = offWork.toLocalTime().toSecondOfDay();
                            int goSecs = min.map(ld -> ld.toLocalTime().toSecondOfDay()).orElse(-300);
                            if (!offWork.equals(min.orElse(null))
                                    && offSecs > 9059
                                    && offSecs - goSecs > 300) {
                                workRecord.setOffWork(offWork.plusHours(6));

                                // TODO  如果下班时间大于加班基础时间, 则计算加班时长, 要注意加班到凌晨的情况
                                LocalDateTime todayBaseOvertime = LocalDateTime.of(offWork.toLocalDate(),
                                        setting.getStandardOvertime().minusHours(6));
                                if (offWork.isAfter(todayBaseOvertime)) {
                                    // TODO 计算
                                    long diffMill = offWork.toInstant(ZoneOffset.of("+8")).toEpochMilli()
                                            - todayBaseOvertime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
                                    workRecord.setOvertimeDuration(diffMill);
                                }
                            }
                        }
                    } else {
                        // 该天缺卡
                        // do nothing
                    }
                } else {
                    // 剩下的就是非工作日来加班的
                    workRecord.setWorkday(false);
                    workRecord.setWorkNum(workNum);
                    workRecord.setDate(punchDaily.get(dayOfYear).get(0).toLocalDate());
                    workRecords.add(workRecord);

                    List<LocalDateTime> punchTime = punchDaily.get(dayOfYear);
                    // 当天最早打卡
                    Optional<LocalDateTime> min = punchTime.stream().min(Comparator.naturalOrder());
                    // 当天最晚打卡
                    Optional<LocalDateTime> max = punchTime.stream().max(Comparator.naturalOrder());

                    if (min.isPresent()) {
                        LocalDateTime goWork = min.get();
                        workRecord.setGoWork(goWork.plusHours(6));
                    }
                    if (max.isPresent()) {
                        LocalDateTime offWork = max.get();
                        if (!offWork.equals(min.orElse(null))) {
                            workRecord.setOffWork(offWork.plusHours(6));
                        }
                    }

                    if (min.isPresent() && max.isPresent()) {
                        long diffMill = max.get().toInstant(ZoneOffset.of("+8")).toEpochMilli()
                                - min.get().toInstant(ZoneOffset.of("+8")).toEpochMilli();
                        workRecord.setOvertimeDuration(diffMill);
                    }
                }
            }
        }

        return ret;
    }

    /**
     * 分析考勤统计
     *
     * @param workLogMap 上下班打卡记录
     * @return java.util.Map<java.lang.Integer, com.jlkj.auth.service.impl.Run.AtteStatistics>
     * @author Zhu Kaixiao
     * @date 2019/6/17 15:54
     **/
    private Map<Integer, AtteStatistics> analysisAtteStatisticsMap(Map<Integer, List<WorkRecord>> workLogMap) {
        // 规定上班时间
        LocalTime standardWorkingTime = setting.getStandardWorkingTime();
        // 规定下班时间
        LocalTime standardOffWorkTime = setting.getStandardOffWorkTime();
        Map<Integer, AtteStatistics> statisticsMap = new HashMap<>(workLogMap.size());

        for (Map.Entry<Integer, List<WorkRecord>> entry : workLogMap.entrySet()) {
            List<WorkRecord> workRecords = entry.getValue();
            StringBuilder sb = new StringBuilder();
            // 迟到次数
            int lateCount = 0;
            // 早退次数
            int leaveEarlyCount = 0;
            // 旷工
            int absenceCount = 0;
            // 忘记打下班卡
            int forgetOffWorkCount = 0;

            for (WorkRecord workRecord : workRecords) {
                LocalDate date = workRecord.getDate();
                LocalDateTime goWork = workRecord.getGoWork();
                LocalDateTime offWork = workRecord.getOffWork();

                // TODO 查询请假记录, 如果有请假, 则根据请假信息修改workingTime 和 offWorkTime
                LocalTime workingTime = standardWorkingTime;
                LocalTime offWorkTime = standardOffWorkTime;

                // 未打上班卡, 也未打下班卡, 旷工
                if (goWork == null && offWork == null) {
                    ++absenceCount;
                    sb.append(DTF_Y_M_D.format(date)).append("旷工,");
                    continue;
                }

                // 未打上班卡但却打了下班卡视为旷工半天(1次)
                if (goWork == null) {
                    ++absenceCount;
                    sb.append(DTF_Y_M_D.format(date)).append("未打上班卡,");
                }

                // 打了上班卡但是却没打下班卡
                if (offWork == null) {
                    ++forgetOffWorkCount;
                    sb.append(DTF_Y_M_D.format(date)).append("未打下班卡,");
                }

                // 下班时间早于规定时间
                if (offWork != null && offWork.toLocalTime().isBefore(offWorkTime)) {
                    int minuteDiff = (offWorkTime.toSecondOfDay() - offWork.toLocalTime().toSecondOfDay()) / 60;
                    sb.append(DTF_Y_M_D.format(date)).append("早退").append(dressMinute(minuteDiff)).append(",");
                    ++leaveEarlyCount;
                }

                // 上班时间晚于规定时间
                if (goWork != null && goWork.toLocalTime().isAfter(workingTime)) {
                    int minuteDiff = (goWork.toLocalTime().toSecondOfDay() - workingTime.toSecondOfDay()) / 60;
                    // 8:31:02 > 8:30:59, 但是因为不足60秒, 所以会为0, 这种情况不满1分钟也按1分钟计
                    minuteDiff = minuteDiff == 0 ? 1 : minuteDiff;
                    sb.append(DTF_Y_M_D.format(date)).append("迟到").append(dressMinute(minuteDiff)).append(",");
                    // 迟到超过30分钟, 视为旷工半天(1次)
                    if (minuteDiff > 30) {
                        ++absenceCount;
                    } else {
                        ++lateCount;
                    }
                }
            }

            AtteStatistics atteStatistics = new AtteStatistics();
            // 员工id
            Integer workNum = entry.getKey();
            // 日期改为1号
            LocalDate ym = entry.getValue().get(0).date.withDayOfMonth(1);
            atteStatistics.setWorkNum(workNum);
            // java.time.LocalDate => java.util.Date
            atteStatistics.setYearMonth(Date.from(ym.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
            atteStatistics.setAbsenceCount(absenceCount);
            atteStatistics.setLateCount(lateCount);
            atteStatistics.setLeaveEarlyCount(leaveEarlyCount);
            atteStatistics.setForgetOffWorkCount(forgetOffWorkCount);
            atteStatistics.setLeaveCount(0);
            if (sb.length() > 1) {
                atteStatistics.setRemark(sb.substring(0, sb.length() - 1));
            }

            statisticsMap.put(workNum, atteStatistics);
        }

        return statisticsMap;
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


    public static class WorkRecord {
        /**
         * 是否工作日
         */
        boolean isWorkday;

        /**
         * 旷工, 1半天, 2全天
         */
        Integer completion;
        /**
         * 工号
         */
        Integer workNum;
        /**
         * 日期
         */
        LocalDate date;
        /**
         * 上班卡
         */
        LocalDateTime goWork;
        /**
         * 下班卡
         */
        LocalDateTime offWork;

        /** 加班时长 毫秒*/
        Long overtimeDuration;

        /** 迟到时长 分钟*/
        Integer lateDuration;

        /** 早退时长 分钟*/
        Integer earlyDuration;


        Integer getWorkNum() {
            return workNum;
        }

        void setWorkNum(Integer workNum) {
            this.workNum = workNum;
        }

        LocalDate getDate() {
            return date;
        }

        void setDate(LocalDate date) {
            this.date = date;
        }

        LocalDateTime getGoWork() {
            return goWork;
        }

        void setGoWork(LocalDateTime goWork) {
            this.goWork = goWork;
        }

        LocalDateTime getOffWork() {
            return offWork;
        }

        void setOffWork(LocalDateTime offWork) {
            this.offWork = offWork;
        }

        Long getOvertimeDuration() {
            return overtimeDuration;
        }

        void setOvertimeDuration(Long overtimeDuration) {
            this.overtimeDuration = overtimeDuration;
        }

        public boolean isWorkday() {
            return isWorkday;
        }

        public void setWorkday(boolean workday) {
            isWorkday = workday;
        }

        public Integer getLateDuration() {
            return lateDuration;
        }

        public void setLateDuration(Integer lateDuration) {
            this.lateDuration = lateDuration;
        }

        public Integer getEarlyDuration() {
            return earlyDuration;
        }

        public void setEarlyDuration(Integer earlyDuration) {
            this.earlyDuration = earlyDuration;
        }

        public Integer getCompletion() {
            return completion;
        }

        public void setCompletion(Integer completion) {
            this.completion = completion;
        }
    }


    // ----------------------------------------------------- static ----------------------------------------------------
    /**
     * yyyy-MM-dd 日期格式
     */
    public static final DateTimeFormatter DTF_Y_M_D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final DateTimeFormatter DTF_Y_M_D_H_M_S = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 打卡记录文件中的时间格式
     */
    public static final DateTimeFormatter DTF_CK_IN = DateTimeFormatter.ofPattern("yyyy/MM/dd  HH:mm:ss");

    /**
     * 匹配打卡记录文件中的每行文字的正则表达式
     */
    private static final Pattern PATTERN_CK_IN =
            Pattern.compile("\\d+\\W+(\\d+)\\W+\\d+\\W+\\d+\\W+(\\d{4}/\\d{2}/\\d{2}\\W+\\d{2}:\\d{2}:\\d{2})");


}