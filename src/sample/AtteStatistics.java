package sample;

import java.util.Date;

/**
 * @author Zhu Kaixiao
 * @version 1.0
 * @date 2019/6/19 14:56
 * @copyright 江西金磊科技发展有限公司 All rights reserved. Notice
 * 仅限于授权后使用，禁止非授权传阅以及私自用于商业目的。
 */
public class AtteStatistics {

    /**
     * 用户id
     */
    private Integer workNum;

    /**
     * 请假次数
     */
    private Integer leaveCount;

    /**
     * 迟到次数
     */
    private Integer lateCount;

    /**
     * 早退次数
     */
    private Integer leaveEarlyCount;

    /**
     * 旷工次数
     */
    private Integer absenceCount;

    /**
     * 年月
     */
    private Date yearMonth;

    /**
     * 备注
     */
    private String remark;

    /**
     *忘记打下班卡次数
     */
    private Integer forgetOffWorkCount;

    /** 加班时长 */
    private Long overtimeDuration;


    public Integer getWorkNum() {
        return workNum;
    }

    public void setWorkNum(Integer workNum) {
        this.workNum = workNum;
    }

    public Integer getLeaveCount() {
        return leaveCount;
    }

    public void setLeaveCount(Integer leaveCount) {
        this.leaveCount = leaveCount;
    }

    public Integer getLateCount() {
        return lateCount;
    }

    public void setLateCount(Integer lateCount) {
        this.lateCount = lateCount;
    }

    public Integer getLeaveEarlyCount() {
        return leaveEarlyCount;
    }

    public void setLeaveEarlyCount(Integer leaveEarlyCount) {
        this.leaveEarlyCount = leaveEarlyCount;
    }

    public Integer getAbsenceCount() {
        return absenceCount;
    }

    public void setAbsenceCount(Integer absenceCount) {
        this.absenceCount = absenceCount;
    }

    public Date getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(Date yearMonth) {
        this.yearMonth = yearMonth;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getForgetOffWorkCount() {
        return forgetOffWorkCount;
    }

    public void setForgetOffWorkCount(Integer forgetOffWorkCount) {
        this.forgetOffWorkCount = forgetOffWorkCount;
    }

    public Long getOvertimeDuration() {
        return overtimeDuration;
    }

    public void setOvertimeDuration(Long overtimeDuration) {
        this.overtimeDuration = overtimeDuration;
    }
}
