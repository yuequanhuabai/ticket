package com.ticket.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ticket.model.Station;
import com.ticket.model.TrainInfo;
import com.ticket.util.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 余票查询服务
 */
public class TicketQueryService {

    private static final String BASE_URL = "https://kyfw.12306.cn/otn/leftTicket/";

    /** 已知的 endpoint 后缀列表，12306 会不定期更换 */
    private static final String[] KNOWN_ENDPOINTS = {
            "query", "queryZ", "queryX", "queryA", "queryG", "queryT",
            "queryO", "queryE", "queryD", "queryY"
    };

    private String queryEndpoint = null;
    private final StationService stationService;
    private boolean sessionInitialized = false;

    /** 连续限流计数器 */
    private int rateLimitCount = 0;

    /** 总请求计数器 */
    private int totalRequests = 0;

    public TicketQueryService(StationService stationService) {
        this.stationService = stationService;
    }

    /**
     * 初始化：建立会话并检测正确的 API endpoint
     */
    public void init() throws IOException {
        if (!sessionInitialized) {
            System.out.println("正在初始化12306会话...");
            HttpUtil.initSession();
            sessionInitialized = true;
        }

        if (queryEndpoint == null) {
            detectEndpoint();
        }
    }

    /**
     * 从 init 页面的 JS 中检测当前有效的查询 endpoint
     */
    private void detectEndpoint() {
        try {
            System.out.println("正在检测余票查询接口...");
            String html = HttpUtil.getHtml("https://kyfw.12306.cn/otn/leftTicket/init");

            Pattern pattern = Pattern.compile("var\\s+CLeftTicketUrl\\s*=\\s*'([^']+)'");
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                String url = matcher.group(1);
                if (url.contains("/")) {
                    queryEndpoint = url.substring(url.lastIndexOf("/") + 1);
                } else {
                    queryEndpoint = url;
                }
                System.out.println("检测到查询接口: " + queryEndpoint);
                return;
            }
        } catch (Exception e) {
            System.err.println("检测接口失败: " + e.getMessage());
        }

        queryEndpoint = "query";
        System.out.println("使用默认查询接口: " + queryEndpoint);
    }

    /**
     * 重新初始化会话（被限流后调用）
     */
    private void refreshSession() {
        try {
            System.out.println("    [刷新会话中...]");
            HttpUtil.initSession();
            sessionInitialized = true;
            detectEndpoint();
            rateLimitCount = 0;
        } catch (Exception e) {
            // 刷新失败，继续用旧的
        }
    }

    /**
     * 查询两站之间的余票
     */
    public List<TrainInfo> queryTickets(String fromStationCode, String toStationCode, String date) throws IOException {
        if (!sessionInitialized) {
            init();
        }

        totalRequests++;

        String url = BASE_URL + queryEndpoint
                + "?leftTicketDTO.train_date=" + date
                + "&leftTicketDTO.from_station=" + fromStationCode
                + "&leftTicketDTO.to_station=" + toStationCode
                + "&purpose_codes=ADULT";

        String response;
        try {
            response = HttpUtil.get(url);
        } catch (IOException e) {
            // 网络错误，可能是限流导致连接被拒
            return handleRateLimit(fromStationCode, toStationCode, date);
        }

        String trimmed = response.trim();

        // 空响应
        if (trimmed.isEmpty()) {
            return handleRateLimit(fromStationCode, toStationCode, date);
        }

        // 非 JSON 响应 → 大概率是被限流了（返回了 HTML 页面）
        if (!trimmed.startsWith("{")) {
            return handleRateLimit(fromStationCode, toStationCode, date);
        }

        // 成功拿到 JSON，重置限流计数
        rateLimitCount = 0;

        List<TrainInfo> result = parseResponse(response);

        // 检查是否需要更新 endpoint（c_url）
        if (result.isEmpty() && response.contains("c_url")) {
            try {
                JsonObject root = JsonParser.parseString(response).getAsJsonObject();
                if (root.has("c_url")) {
                    String newUrl = root.get("c_url").getAsString();
                    if (newUrl != null && !newUrl.isEmpty()) {
                        if (newUrl.contains("/")) {
                            queryEndpoint = newUrl.substring(newUrl.lastIndexOf("/") + 1);
                        } else {
                            queryEndpoint = newUrl;
                        }
                        System.out.println("    [接口已更新: " + queryEndpoint + "]");
                        return queryTickets(fromStationCode, toStationCode, date);
                    }
                }
            } catch (Exception ignored) {}
        }

        return result;
    }

    /**
     * 处理限流：等待后重试，而不是盲目切换 endpoint
     */
    private List<TrainInfo> handleRateLimit(String fromStationCode, String toStationCode, String date) {
        rateLimitCount++;

        if (rateLimitCount <= 3) {
            // 前3次：递增等待后重试同一 endpoint
            long waitSeconds = rateLimitCount * 3L; // 3秒, 6秒, 9秒
            System.out.println("    [被限流，等待" + waitSeconds + "秒后重试...]");
            try {
                Thread.sleep(waitSeconds * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 重试
            try {
                String url = BASE_URL + queryEndpoint
                        + "?leftTicketDTO.train_date=" + date
                        + "&leftTicketDTO.from_station=" + fromStationCode
                        + "&leftTicketDTO.to_station=" + toStationCode
                        + "&purpose_codes=ADULT";
                String response = HttpUtil.get(url);
                String trimmed = response.trim();
                if (!trimmed.isEmpty() && trimmed.startsWith("{")) {
                    rateLimitCount = 0;
                    return parseResponse(response);
                }
            } catch (Exception e) {
                // 重试也失败了
            }

            // 重试失败，递归继续
            return handleRateLimit(fromStationCode, toStationCode, date);
        }

        if (rateLimitCount == 4) {
            // 第4次：刷新会话后重试
            refreshSession();
            try {
                String url = BASE_URL + queryEndpoint
                        + "?leftTicketDTO.train_date=" + date
                        + "&leftTicketDTO.from_station=" + fromStationCode
                        + "&leftTicketDTO.to_station=" + toStationCode
                        + "&purpose_codes=ADULT";
                String response = HttpUtil.get(url);
                String trimmed = response.trim();
                if (!trimmed.isEmpty() && trimmed.startsWith("{")) {
                    rateLimitCount = 0;
                    return parseResponse(response);
                }
            } catch (Exception e) {
                // 刷新后重试也失败
            }
        }

        // 超过重试次数，放弃本次查询
        rateLimitCount = 0;
        return new ArrayList<>();
    }

    /**
     * 解析 12306 余票查询响应
     */
    private List<TrainInfo> parseResponse(String response) {
        List<TrainInfo> trains = new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();

            if (root.has("status") && !root.get("status").getAsBoolean()) {
                if (root.has("messages") && root.getAsJsonArray("messages").size() > 0) {
                    System.err.println("12306 返回错误: " + root.getAsJsonArray("messages"));
                }
                return trains;
            }

            if (!root.has("data")) return trains;

            JsonElement dataElement = root.get("data");
            if (!dataElement.isJsonObject()) return trains;

            JsonObject data = dataElement.getAsJsonObject();
            if (!data.has("result")) return trains;

            JsonObject stationMap = data.has("map") ? data.getAsJsonObject("map") : null;

            JsonArray results = data.getAsJsonArray("result");
            for (JsonElement element : results) {
                String rawData = element.getAsString();
                TrainInfo train = parseTrainData(rawData, stationMap);
                if (train != null) {
                    trains.add(train);
                }
            }
        } catch (Exception e) {
            System.err.println("解析余票数据失败: " + e.getMessage());
            if (response.length() > 200) {
                System.err.println("响应内容(前200字符): " + response.substring(0, 200));
            } else {
                System.err.println("响应内容: " + response);
            }
        }

        return trains;
    }

    /**
     * 解析单条车次数据
     */
    private TrainInfo parseTrainData(String rawData, JsonObject stationMap) {
        try {
            String[] fields = rawData.split("\\|");
            if (fields.length < 35) return null;

            TrainInfo train = new TrainInfo();
            train.setRawData(rawData);
            train.setTrainNo(fields[2]);
            train.setStationTrainCode(fields[3]);
            train.setFromStationCode(fields[6]);
            train.setToStationCode(fields[7]);
            train.setStartTime(fields[8]);
            train.setArriveTime(fields[9]);
            train.setDuration(fields[10]);

            if (stationMap != null) {
                if (stationMap.has(fields[6])) {
                    train.setFromStationName(stationMap.get(fields[6]).getAsString());
                }
                if (stationMap.has(fields[7])) {
                    train.setToStationName(stationMap.get(fields[7]).getAsString());
                }
            }

            // fields[4] 和 fields[5] 是始发站和终到站的电报码
            train.setStartStationCode(fields[4]);
            train.setEndStationCode(fields[5]);

            Station startStation = stationService.getByCode(fields[4]);
            train.setStartStationName(startStation != null ? startStation.getName() : fields[4]);
            Station endStation = stationService.getByCode(fields[5]);
            train.setEndStationName(endStation != null ? endStation.getName() : fields[5]);

            if (train.getFromStationName() == null) {
                Station s = stationService.getByCode(fields[6]);
                train.setFromStationName(s != null ? s.getName() : fields[6]);
            }
            if (train.getToStationName() == null) {
                Station s = stationService.getByCode(fields[7]);
                train.setToStationName(s != null ? s.getName() : fields[7]);
            }

            train.setBusinessSeat(getField(fields, 32));
            train.setFirstClassSeat(getField(fields, 31));
            train.setSecondClassSeat(getField(fields, 30));
            train.setAdvancedSoftSleeper(getField(fields, 21));
            train.setSoftSleeper(getField(fields, 23));
            train.setMoveSleeper(getField(fields, 33));
            train.setHardSleeper(getField(fields, 28));
            train.setSoftSeat(getField(fields, 24));
            train.setHardSeat(getField(fields, 29));
            train.setNoSeat(getField(fields, 26));
            train.setCanBook("Y".equals(fields[11]));

            return train;
        } catch (Exception e) {
            return null;
        }
    }

    private String getField(String[] fields, int index) {
        if (index < fields.length && fields[index] != null && !fields[index].isEmpty()) {
            return fields[index];
        }
        return "--";
    }
}
