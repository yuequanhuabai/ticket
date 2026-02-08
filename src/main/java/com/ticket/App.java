package com.ticket;

import com.ticket.model.Station;
import com.ticket.model.TrainInfo;
import com.ticket.model.TrainStop;
import com.ticket.service.StationService;
import com.ticket.service.TicketQueryService;
import com.ticket.service.TrainRouteService;

import java.util.List;
import java.util.Scanner;

/**
 * 12306 "买长乘短" 火车票查询工具
 *
 * 功能：查询从出发地到目的地的车次余票，并分析"买长乘短"的购票机会。
 * 即：如果直达票无票，尝试购买同一车次到更远站点的车票，在目的地站下车。
 */
public class App {

    private final StationService stationService = new StationService();
    private final TicketQueryService ticketQueryService = new TicketQueryService(stationService);
    private final TrainRouteService trainRouteService = new TrainRouteService();

    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    public void run() {
        System.out.println("========================================");
        System.out.println("   12306 \"买长乘短\" 火车票查询工具");
        System.out.println("========================================");
        System.out.println();

        // 1. 初始化车站数据和12306会话
        try {
            stationService.init();
            ticketQueryService.init();
        } catch (Exception e) {
            System.err.println("初始化失败: " + e.getMessage());
            System.err.println("请检查网络连接后重试。");
            return;
        }

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println();
            System.out.println("----------------------------------------");

            // 2. 获取出发站
            Station fromStation = inputStation(scanner, "请输入出发城市（如：深圳）");
            if (fromStation == null) break;

            // 3. 获取目的站
            Station toStation = inputStation(scanner, "请输入目的地（如：衡阳）");
            if (toStation == null) break;

            // 4. 获取出行日期
            System.out.print("请输入出行日期（格式 yyyy-MM-dd，如 2026-02-10）: ");
            String date = scanner.nextLine().trim();
            if (date.isEmpty()) {
                System.out.println("日期不能为空");
                continue;
            }

            System.out.println();
            System.out.println("正在查询 " + fromStation.getName() + " -> " + toStation.getName() + " (" + date + ") ...");
            System.out.println();

            // 5. 执行查询
            try {
                queryAndAnalyze(fromStation, toStation, date);
            } catch (Exception e) {
                System.err.println("查询失败: " + e.getMessage());
                e.printStackTrace();
            }

            // 6. 是否继续
            System.out.println();
            System.out.print("是否继续查询？(y/n): ");
            String cont = scanner.nextLine().trim().toLowerCase();
            if (!"y".equals(cont) && !"yes".equals(cont)) {
                break;
            }
        }

        System.out.println("感谢使用，祝您购票顺利！");
    }

    /**
     * 输入并选择车站
     */
    private Station inputStation(Scanner scanner, String prompt) {
        System.out.print(prompt + "（输入 q 退出）: ");
        String input = scanner.nextLine().trim();

        if ("q".equalsIgnoreCase(input) || "quit".equalsIgnoreCase(input)) {
            return null;
        }

        // 精确匹配
        Station station = stationService.getByName(input);
        if (station != null) {
            System.out.println("  -> 已选择: " + station);
            return station;
        }

        // 模糊搜索
        List<Station> results = stationService.search(input);
        if (results.isEmpty()) {
            System.out.println("  未找到匹配的车站，请重新输入。");
            return inputStation(scanner, prompt);
        }

        if (results.size() == 1) {
            System.out.println("  -> 已选择: " + results.get(0));
            return results.get(0);
        }

        // 多个匹配，让用户选择
        System.out.println("  找到多个匹配车站，请选择：");
        int showCount = Math.min(results.size(), 20);
        for (int i = 0; i < showCount; i++) {
            System.out.printf("    [%d] %s%n", i + 1, results.get(i));
        }
        if (results.size() > 20) {
            System.out.println("    ... 共 " + results.size() + " 个结果，仅显示前20个");
        }

        System.out.print("  请输入编号: ");
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice >= 1 && choice <= showCount) {
                Station selected = results.get(choice - 1);
                System.out.println("  -> 已选择: " + selected);
                return selected;
            }
        } catch (NumberFormatException ignored) {}

        System.out.println("  输入无效，请重新选择。");
        return inputStation(scanner, prompt);
    }

    /**
     * 核心逻辑：查询余票并分析"买长乘短"机会
     */
    private void queryAndAnalyze(Station fromStation, Station toStation, String date) throws Exception {
        // Step 1: 查询直达余票
        System.out.println("【第1步】查询直达余票...");
        List<TrainInfo> directTrains = ticketQueryService.queryTickets(
                fromStation.getCode(), toStation.getCode(), date);

        if (directTrains.isEmpty()) {
            System.out.println("  未查询到从 " + fromStation.getName() + " 到 " + toStation.getName() + " 的车次。");
            return;
        }

        System.out.println("  共找到 " + directTrains.size() + " 个车次");
        System.out.println();

        // 先打印所有车次的余票概况
        printTableHeader();
        int noTicketCount = 0;
        int hasTicketCount = 0;
        int canExtendCount = 0;

        for (TrainInfo train : directTrains) {
            printTrainRow(train);
            if (train.hasAvailableTicket()) {
                hasTicketCount++;
            } else {
                noTicketCount++;
                // 判断该车次是否有延伸空间（到达站编码 != 终到站编码）
                if (!train.getToStationCode().equals(train.getEndStationCode())) {
                    canExtendCount++;
                }
            }
        }

        System.out.println();
        System.out.println("直达有票: " + hasTicketCount + " | 直达无票: " + noTicketCount
                + " | 可尝试买长: " + canExtendCount);

        if (canExtendCount == 0) {
            System.out.println("所有无票车次均以目的地为终到站，无法使用买长乘短策略。");
            printSummary(directTrains.size(), hasTicketCount, noTicketCount, 0);
            return;
        }

        // Step 2: 对可延伸的无票车次，分析"买长乘短"机会
        System.out.println();
        System.out.println("【第2步】分析买长乘短机会（共 " + canExtendCount + " 个车次需要查询）...");
        System.out.println();

        int buyLongOpportunities = 0;
        int analyzed = 0;

        for (TrainInfo train : directTrains) {
            // 只分析无票且有延伸空间的车次
            if (train.hasAvailableTicket()) continue;
            if (train.getToStationCode().equals(train.getEndStationCode())) continue;

            analyzed++;
            System.out.println("[" + analyzed + "/" + canExtendCount + "] "
                    + train.getStationTrainCode()
                    + " (" + train.getFromStationName() + "→" + train.getToStationName()
                    + ", 终到: " + train.getEndStationName() + ")");

            try {
                // 关键修复：用车次的实际始发站→终到站查完整路线
                List<TrainStop> route = trainRouteService.queryRoute(
                        train.getTrainNo(),
                        train.getStartStationCode(),
                        train.getEndStationCode(),
                        date);

                if (route.isEmpty()) {
                    System.out.println("    ↳ 未能获取经停站信息");
                    continue;
                }

                // 用到达站编码在完整路线中定位，找到之后的站点
                List<TrainStop> stopsAfter = trainRouteService.getStopsAfter(
                        route, toStation.getName(), train.getToStationCode());

                if (stopsAfter.isEmpty()) {
                    System.out.println("    ↳ 目的地之后无更多站点");
                    continue;
                }

                System.out.println("    ↳ 目的地之后还有 " + stopsAfter.size() + " 个站，正在查询余票...");

                // 优先查询终点站（买到终点概率最高），然后查中间几个大站
                // 限制最多查 5 个站点，避免请求过多被限流
                boolean foundOpportunity = false;

                // 先查终点站
                TrainStop lastStop = stopsAfter.get(stopsAfter.size() - 1);
                foundOpportunity = checkExtendedTicket(fromStation, train, lastStop, date);
                if (foundOpportunity) buyLongOpportunities++;

                // 再查中间站（均匀取样，最多再查4个）
                if (!foundOpportunity && stopsAfter.size() > 1) {
                    int step = Math.max(1, stopsAfter.size() / 4);
                    int checked = 0;
                    for (int i = 0; i < stopsAfter.size() - 1 && checked < 4; i += step) {
                        TrainStop extStop = stopsAfter.get(i);
                        if (checkExtendedTicket(fromStation, train, extStop, date)) {
                            buyLongOpportunities++;
                            foundOpportunity = true;
                            break; // 找到一个机会就够了
                        }
                        checked++;
                    }
                }

                if (!foundOpportunity) {
                    System.out.println("    ↳ 延伸站点也无余票");
                }

            } catch (Exception e) {
                System.out.println("    ↳ 查询失败: " + e.getMessage());
            }
        }

        printSummary(directTrains.size(), hasTicketCount, noTicketCount, buyLongOpportunities);
    }

    /**
     * 查询延伸站点的余票
     * @return 是否找到买长乘短机会
     */
    private boolean checkExtendedTicket(Station fromStation, TrainInfo train, TrainStop extStop, String date) {
        String extCode = extStop.getStationCode();
        if (extCode == null || extCode.isEmpty()) {
            Station extStation = stationService.getByName(extStop.getStationName());
            if (extStation != null) {
                extCode = extStation.getCode();
            } else {
                return false;
            }
        }

        try {
            List<TrainInfo> extTickets = ticketQueryService.queryTickets(
                    fromStation.getCode(), extCode, date);

            for (TrainInfo extTrain : extTickets) {
                if (extTrain.getStationTrainCode().equals(train.getStationTrainCode())) {
                    if (extTrain.hasAvailableTicket()) {
                        System.out.println("    ★ 买长乘短机会! "
                                + fromStation.getName() + " → " + extStop.getStationName()
                                + " (" + train.getStationTrainCode() + "): "
                                + extTrain.getTicketSummary());
                        return true;
                    }
                    return false;
                }
            }
        } catch (Exception e) {
            // 查询失败，跳过
        }
        return false;
    }

    private void printSummary(int total, int hasTicket, int noTicket, int buyLong) {
        System.out.println();
        System.out.println("========== 查询总结 ==========");
        System.out.println("车次总数: " + total);
        System.out.println("有票车次: " + hasTicket);
        System.out.println("无票车次: " + noTicket);
        System.out.println("买长乘短机会: " + buyLong);
        System.out.println("==============================");
    }

    /**
     * 打印表格头
     */
    private void printTableHeader() {
        System.out.println(String.format("%-8s %-8s %-8s %-8s %-6s %-6s %-6s %-6s %-6s %-6s %-6s %-6s",
                "车次", "出发站", "到达站", "终到站", "出发", "到达", "历时", "商务", "一等", "二等", "硬卧", "硬座"));
        System.out.println("-".repeat(100));
    }

    /**
     * 打印一行车次信息
     */
    private void printTrainRow(TrainInfo train) {
        String endMark = train.getToStationCode().equals(train.getEndStationCode()) ? "(终)" : train.getEndStationName();
        System.out.println(String.format("%-8s %-8s %-8s %-8s %-6s %-6s %-6s %-6s %-6s %-6s %-6s %-6s",
                train.getStationTrainCode(),
                train.getFromStationName(),
                train.getToStationName(),
                endMark,
                train.getStartTime(),
                train.getArriveTime(),
                train.getDuration(),
                formatSeat(train.getBusinessSeat()),
                formatSeat(train.getFirstClassSeat()),
                formatSeat(train.getSecondClassSeat()),
                formatSeat(train.getHardSleeper()),
                formatSeat(train.getHardSeat())));
    }

    private String formatSeat(String seat) {
        if (seat == null || seat.isEmpty() || "--".equals(seat)) return "--";
        if ("无".equals(seat)) return "无";
        if ("有".equals(seat)) return "有";
        return seat;
    }

    /**
     * 中文字符串右填充（考虑中文字符宽度）
     */
    private String padRight(String str, int width) {
        if (str == null) str = "";
        int chineseCount = 0;
        for (char c : str.toCharArray()) {
            if (c > 127) chineseCount++;
        }
        int padLen = width - str.length() - chineseCount;
        if (padLen <= 0) return str;
        return str + " ".repeat(padLen);
    }
}
