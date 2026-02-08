package com.ticket.model;

/**
 * 车次信息及余票情况
 */
public class TrainInfo {

    /** 车次内部编号（如 5l0000G10100） */
    private String trainNo;

    /** 车次代码（如 G101） */
    private String stationTrainCode;

    /** 始发站电报码 */
    private String startStationCode;

    /** 终到站电报码 */
    private String endStationCode;

    /** 始发站名 */
    private String startStationName;

    /** 终到站名 */
    private String endStationName;

    /** 出发站名（查询的出发站） */
    private String fromStationName;

    /** 到达站名（查询的到达站） */
    private String toStationName;

    /** 出发站电报码 */
    private String fromStationCode;

    /** 到达站电报码 */
    private String toStationCode;

    /** 出发时间 */
    private String startTime;

    /** 到达时间 */
    private String arriveTime;

    /** 历时 */
    private String duration;

    /** 商务座/特等座 */
    private String businessSeat;

    /** 一等座 */
    private String firstClassSeat;

    /** 二等座/二等包座 */
    private String secondClassSeat;

    /** 高级软卧 */
    private String advancedSoftSleeper;

    /** 软卧/一等卧 */
    private String softSleeper;

    /** 动卧 */
    private String moveSleeper;

    /** 硬卧/二等卧 */
    private String hardSleeper;

    /** 软座 */
    private String softSeat;

    /** 硬座 */
    private String hardSeat;

    /** 无座 */
    private String noSeat;

    /** 是否可预订 */
    private boolean canBook;

    /** 原始数据字符串 */
    private String rawData;

    public String getTrainNo() { return trainNo; }
    public void setTrainNo(String trainNo) { this.trainNo = trainNo; }

    public String getStationTrainCode() { return stationTrainCode; }
    public void setStationTrainCode(String stationTrainCode) { this.stationTrainCode = stationTrainCode; }

    public String getStartStationCode() { return startStationCode; }
    public void setStartStationCode(String startStationCode) { this.startStationCode = startStationCode; }

    public String getEndStationCode() { return endStationCode; }
    public void setEndStationCode(String endStationCode) { this.endStationCode = endStationCode; }

    public String getStartStationName() { return startStationName; }
    public void setStartStationName(String startStationName) { this.startStationName = startStationName; }

    public String getEndStationName() { return endStationName; }
    public void setEndStationName(String endStationName) { this.endStationName = endStationName; }

    public String getFromStationName() { return fromStationName; }
    public void setFromStationName(String fromStationName) { this.fromStationName = fromStationName; }

    public String getToStationName() { return toStationName; }
    public void setToStationName(String toStationName) { this.toStationName = toStationName; }

    public String getFromStationCode() { return fromStationCode; }
    public void setFromStationCode(String fromStationCode) { this.fromStationCode = fromStationCode; }

    public String getToStationCode() { return toStationCode; }
    public void setToStationCode(String toStationCode) { this.toStationCode = toStationCode; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getArriveTime() { return arriveTime; }
    public void setArriveTime(String arriveTime) { this.arriveTime = arriveTime; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getBusinessSeat() { return businessSeat; }
    public void setBusinessSeat(String businessSeat) { this.businessSeat = businessSeat; }

    public String getFirstClassSeat() { return firstClassSeat; }
    public void setFirstClassSeat(String firstClassSeat) { this.firstClassSeat = firstClassSeat; }

    public String getSecondClassSeat() { return secondClassSeat; }
    public void setSecondClassSeat(String secondClassSeat) { this.secondClassSeat = secondClassSeat; }

    public String getAdvancedSoftSleeper() { return advancedSoftSleeper; }
    public void setAdvancedSoftSleeper(String advancedSoftSleeper) { this.advancedSoftSleeper = advancedSoftSleeper; }

    public String getSoftSleeper() { return softSleeper; }
    public void setSoftSleeper(String softSleeper) { this.softSleeper = softSleeper; }

    public String getMoveSleeper() { return moveSleeper; }
    public void setMoveSleeper(String moveSleeper) { this.moveSleeper = moveSleeper; }

    public String getHardSleeper() { return hardSleeper; }
    public void setHardSleeper(String hardSleeper) { this.hardSleeper = hardSleeper; }

    public String getSoftSeat() { return softSeat; }
    public void setSoftSeat(String softSeat) { this.softSeat = softSeat; }

    public String getHardSeat() { return hardSeat; }
    public void setHardSeat(String hardSeat) { this.hardSeat = hardSeat; }

    public String getNoSeat() { return noSeat; }
    public void setNoSeat(String noSeat) { this.noSeat = noSeat; }

    public boolean isCanBook() { return canBook; }
    public void setCanBook(boolean canBook) { this.canBook = canBook; }

    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }

    /**
     * 获取余票摘要信息
     */
    public String getTicketSummary() {
        StringBuilder sb = new StringBuilder();
        appendSeat(sb, "商务座", businessSeat);
        appendSeat(sb, "一等座", firstClassSeat);
        appendSeat(sb, "二等座", secondClassSeat);
        appendSeat(sb, "软卧", softSleeper);
        appendSeat(sb, "硬卧", hardSleeper);
        appendSeat(sb, "硬座", hardSeat);
        appendSeat(sb, "无座", noSeat);
        return sb.length() > 0 ? sb.toString() : "无票";
    }

    /**
     * 是否有任何可用座位
     */
    public boolean hasAvailableTicket() {
        return isAvailable(businessSeat) || isAvailable(firstClassSeat) || isAvailable(secondClassSeat)
                || isAvailable(softSleeper) || isAvailable(hardSleeper)
                || isAvailable(hardSeat) || isAvailable(noSeat)
                || isAvailable(advancedSoftSleeper) || isAvailable(softSeat)
                || isAvailable(moveSleeper);
    }

    private boolean isAvailable(String seatInfo) {
        return seatInfo != null && !seatInfo.isEmpty()
                && !"无".equals(seatInfo) && !"--".equals(seatInfo) && !"*".equals(seatInfo);
    }

    private void appendSeat(StringBuilder sb, String seatType, String count) {
        if (isAvailable(count)) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(seatType).append(":").append(count);
        }
    }
}
