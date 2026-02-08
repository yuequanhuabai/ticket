package com.ticket.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ticket.model.TrainStop;
import com.ticket.util.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 车次路线查询服务 - 查询车次的完整经停站信息
 */
public class TrainRouteService {

    private static final String ROUTE_URL = "https://kyfw.12306.cn/otn/czxx/queryByTrainNo";

    /**
     * 查询车次的完整经停路线
     *
     * @param trainNo           车次内部编号（如 5l0000G10100）
     * @param fromStationCode   出发站电报码
     * @param toStationCode     到达站电报码
     * @param date              出发日期 (yyyy-MM-dd)
     * @return 经停站列表（按站序排列）
     */
    public List<TrainStop> queryRoute(String trainNo, String fromStationCode, String toStationCode, String date) throws IOException {
        String url = ROUTE_URL
                + "?train_no=" + trainNo
                + "&from_station_telecode=" + fromStationCode
                + "&to_station_telecode=" + toStationCode
                + "&depart_date=" + date;

        String response = HttpUtil.get(url);
        return parseResponse(response);
    }

    /**
     * 解析经停站响应数据
     */
    private List<TrainStop> parseResponse(String response) {
        List<TrainStop> stops = new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();

            if (!root.has("data")) {
                return stops;
            }

            JsonObject data = root.getAsJsonObject("data");
            if (!data.has("data")) {
                return stops;
            }

            JsonArray stopsArray = data.getAsJsonArray("data");
            for (JsonElement element : stopsArray) {
                JsonObject obj = element.getAsJsonObject();
                TrainStop stop = new TrainStop();

                stop.setStationNo(getInt(obj, "station_no"));
                stop.setStationName(getString(obj, "station_name"));
                stop.setArriveTime(getString(obj, "arrive_time"));
                stop.setStartTime(getString(obj, "start_time"));
                stop.setStopoverTime(getString(obj, "stopover_time"));

                // 提取站点电报码（如果有的话）
                if (obj.has("station_telecode")) {
                    stop.setStationCode(getString(obj, "station_telecode"));
                }

                stop.setStart(stop.getStationNo() == 1);
                stop.setEnd("----".equals(stop.getStartTime()) || stop.getStartTime() == null);

                stops.add(stop);
            }
        } catch (Exception e) {
            System.err.println("解析经停站数据失败: " + e.getMessage());
        }

        return stops;
    }

    /**
     * 在经停站列表中找到指定站点之后的所有站点
     * 支持按站名或站点编码匹配
     *
     * @param stops       完整经停站列表
     * @param stationName 目标站名
     * @param stationCode 目标站编码（电报码），可为 null
     * @return 目标站之后的站点列表（不含目标站本身）
     */
    public List<TrainStop> getStopsAfter(List<TrainStop> stops, String stationName, String stationCode) {
        List<TrainStop> result = new ArrayList<>();
        boolean found = false;
        for (TrainStop stop : stops) {
            if (found) {
                result.add(stop);
            }
            // 先按编码匹配，再按站名匹配（包含匹配，如"衡阳"匹配"衡阳东"）
            if (!found) {
                if (stationCode != null && stationCode.equals(stop.getStationCode())) {
                    found = true;
                } else if (stop.getStationName().equals(stationName)
                        || stop.getStationName().startsWith(stationName)
                        || stationName.startsWith(stop.getStationName())) {
                    found = true;
                }
            }
        }
        return result;
    }

    private String getString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }

    private int getInt(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return Integer.parseInt(obj.get(key).getAsString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}
