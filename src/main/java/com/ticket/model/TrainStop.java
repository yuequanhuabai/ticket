package com.ticket.model;

/**
 * 车次经停站信息
 */
public class TrainStop {

    /** 站序（从1开始） */
    private int stationNo;

    /** 站名 */
    private String stationName;

    /** 站点电报码 */
    private String stationCode;

    /** 到达时间 */
    private String arriveTime;

    /** 出发时间 */
    private String startTime;

    /** 停留时间 */
    private String stopoverTime;

    /** 是否为始发站 */
    private boolean isStart;

    /** 是否为终到站 */
    private boolean isEnd;

    public int getStationNo() { return stationNo; }
    public void setStationNo(int stationNo) { this.stationNo = stationNo; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getStationCode() { return stationCode; }
    public void setStationCode(String stationCode) { this.stationCode = stationCode; }

    public String getArriveTime() { return arriveTime; }
    public void setArriveTime(String arriveTime) { this.arriveTime = arriveTime; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getStopoverTime() { return stopoverTime; }
    public void setStopoverTime(String stopoverTime) { this.stopoverTime = stopoverTime; }

    public boolean isStart() { return isStart; }
    public void setStart(boolean start) { isStart = start; }

    public boolean isEnd() { return isEnd; }
    public void setEnd(boolean end) { isEnd = end; }

    @Override
    public String toString() {
        return String.format("%-4d %-8s  到达:%-6s  出发:%-6s  停留:%s",
                stationNo, stationName, arriveTime, startTime, stopoverTime);
    }
}
