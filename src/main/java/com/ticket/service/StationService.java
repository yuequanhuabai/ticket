package com.ticket.service;

import com.ticket.model.Station;
import com.ticket.util.HttpUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 车站数据服务 - 加载和查询车站编码信息
 */
public class StationService {

    private static final String STATION_URL = "https://kyfw.12306.cn/otn/resources/js/framework/station_name.js";
    private static final String CACHE_FILE = "stations.dat";

    /** 所有车站列表 */
    private List<Station> allStations = new ArrayList<>();

    /** 车站名称 -> Station 映射 */
    private Map<String, Station> nameMap = new HashMap<>();

    /** 车站编码 -> Station 映射 */
    private Map<String, Station> codeMap = new HashMap<>();

    /**
     * 初始化车站数据（优先从缓存加载，否则从12306下载）
     */
    public void init() throws IOException {
        // 尝试从缓存加载
        Path cachePath = Paths.get(CACHE_FILE);
        if (Files.exists(cachePath)) {
            long ageHours = (System.currentTimeMillis() - Files.getLastModifiedTime(cachePath).toMillis()) / (1000 * 60 * 60);
            if (ageHours < 24 * 7) { // 缓存7天有效
                System.out.println("从本地缓存加载车站数据...");
                loadFromCache(cachePath);
                if (!allStations.isEmpty()) {
                    System.out.println("已加载 " + allStations.size() + " 个车站");
                    return;
                }
            }
        }

        // 从12306下载
        System.out.println("正在从12306下载车站数据...");
        String jsContent = HttpUtil.get(STATION_URL);
        parseStationData(jsContent);
        saveToCache(cachePath, jsContent);
        System.out.println("已加载 " + allStations.size() + " 个车站");
    }

    /**
     * 解析 station_name.js 的内容
     * 格式: var station_names ='@bjb|北京北|VAP|beijingbei|bjb|0@...'
     */
    private void parseStationData(String jsContent) {
        allStations.clear();
        nameMap.clear();
        codeMap.clear();

        // 提取引号内的数据
        int start = jsContent.indexOf("'");
        int end = jsContent.lastIndexOf("'");
        if (start < 0 || end <= start) {
            System.err.println("车站数据格式异常，无法解析");
            return;
        }
        String data = jsContent.substring(start + 1, end);

        // 按 @ 分割每个车站
        String[] entries = data.split("@");
        for (String entry : entries) {
            if (entry.isEmpty()) continue;
            String[] parts = entry.split("\\|");
            if (parts.length >= 6) {
                Station station = new Station(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
                allStations.add(station);
                nameMap.put(parts[1], station);
                codeMap.put(parts[2], station);
            }
        }
    }

    /**
     * 根据中文名精确查找车站
     */
    public Station getByName(String name) {
        return nameMap.get(name);
    }

    /**
     * 根据电报码查找车站
     */
    public Station getByCode(String code) {
        return codeMap.get(code);
    }

    /**
     * 模糊搜索车站（支持中文名、拼音、拼音首字母）
     */
    public List<Station> search(String keyword) {
        String kw = keyword.toLowerCase().trim();
        return allStations.stream()
                .filter(s -> s.getName().contains(kw)
                        || s.getPinyin().contains(kw)
                        || s.getInitial().contains(kw)
                        || s.getAbbr().contains(kw))
                .collect(Collectors.toList());
    }

    /**
     * 保存到本地缓存
     */
    private void saveToCache(Path path, String content) {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("缓存保存失败: " + e.getMessage());
        }
    }

    /**
     * 从本地缓存加载
     */
    private void loadFromCache(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            parseStationData(content);
        } catch (IOException e) {
            System.err.println("缓存读取失败: " + e.getMessage());
        }
    }
}
