package com.tea;

import com.tea.dao.DaoException;
import com.tea.entity.Admin;
import com.tea.entity.Drink;
import com.tea.entity.Member;
import com.tea.entity.OrderItem;
import com.tea.entity.TeaOrder;
import com.tea.service.AdminService;
import com.tea.service.BusinessException;
import com.tea.service.DrinkService;
import com.tea.service.MemberService;
import com.tea.service.OrderService;
import com.tea.service.StatService;
import com.tea.util.DbUtil;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.tea.util.ConsoleUtil.confirm;
import static com.tea.util.ConsoleUtil.padRight;
import static com.tea.util.ConsoleUtil.pause;
import static com.tea.util.ConsoleUtil.readId;
import static com.tea.util.ConsoleUtil.readInt;
import static com.tea.util.ConsoleUtil.readLine;
import static com.tea.util.ConsoleUtil.readMoney;
import static com.tea.util.ConsoleUtil.readNonBlank;

/**
 * 奶茶物语 · 点单系统 —— 控制台入口（表现层）。
 * 只负责菜单、输入输出和异常提示，业务规则全部在 Service 层。
 */
public class Main {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final AdminService ADMIN_SVC = new AdminService();
    private static final DrinkService DRINK_SVC = new DrinkService();
    private static final MemberService MEMBER_SVC = new MemberService();
    private static final OrderService ORDER_SVC = new OrderService();
    private static final StatService STAT_SVC = new StatService();

    /** 当前登录的店员，null 表示未登录。 */
    private static Admin currentAdmin;

    public static void main(String[] args) {
        printWelcome();
        String err = DbUtil.checkConnection();
        if (err != null) {
            System.out.println("✗ 无法连接数据库：" + err);
            System.out.println("  请确认 MySQL 已启动、已执行 sql/init.sql，并检查 db.properties 中的账号密码。");
            return;
        }
        while (true) {
            try {
                if (currentAdmin == null) {
                    if (!loginMenu()) {
                        System.out.println("👋 再见！欢迎下次再来。");
                        return;
                    }
                } else {
                    mainMenu();
                }
            } catch (BusinessException e) {
                System.out.println("✗ " + e.getMessage());
            } catch (DaoException e) {
                System.out.println("✗ 数据库操作失败，请稍后重试（" + e.getMessage() + "）");
            }
        }
    }

    // ===================== 欢迎页 / 登录 =====================

    private static void printWelcome() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║                                              ║");
        System.out.println("║       🧋  奶茶物语 · 点单系统  v1.0          ║");
        System.out.println("║                                              ║");
        System.out.println("║         欢迎使用，请先登录                   ║");
        System.out.println("║                                              ║");
        System.out.println("╚══════════════════════════════════════════════╝");
    }

    /** @return false 表示用户选择退出系统 */
    private static boolean loginMenu() {
        System.out.println();
        System.out.println("   1. 登录");
        System.out.println("   2. 注册新账号");
        System.out.println("   0. 退出系统");
        switch (readLine("请选择 > ")) {
            case "1" -> doLogin();
            case "2" -> doRegister();
            case "0" -> {
                return false;
            }
            default -> System.out.println("✗ 无效选项");
        }
        return true;
    }

    private static void doLogin() {
        System.out.println("┌──────────── 登录 ────────────┐");
        String u = readLine("请输入用户名 > ");
        String p = readLine("请输入密码   > ");
        System.out.println("└──────────────────────────────┘");
        Admin a = ADMIN_SVC.login(u, p);
        if (a == null) {
            System.out.println("✗ 用户名或密码错误，请重试。");
            pause();
            return;
        }
        currentAdmin = a;
        System.out.println("✓ 登录成功，欢迎回来，" + a.getUsername() + "！");
        readLine("按回车进入主菜单...");
    }

    private static void doRegister() {
        System.out.println("┌────────── 注册新账号 ──────────┐");
        String u = readNonBlank("用户名        > ", 32);
        String p = readLine("密码（≥6位） > ");
        String p2 = readLine("再次输入密码 > ");
        System.out.println("└────────────────────────────────┘");
        if (!p.equals(p2)) {
            System.out.println("✗ 两次输入的密码不一致");
            return;
        }
        ADMIN_SVC.register(u, p);
        System.out.println("✓ 注册成功，请登录。");
    }

    // ===================== 主菜单 =====================

    private static void mainMenu() {
        System.out.println();
        System.out.println("╔════════════ 奶茶物语 · 主菜单 [" + currentAdmin.getUsername() + "] ════════════╗");
        System.out.println("   1. 商品管理（奶茶 CRUD）");
        System.out.println("   2. 会员管理");
        System.out.println("   3. 下单结算  ←  最常用");
        System.out.println("   4. 数据统计");
        System.out.println();
        System.out.println("   0. 退出登录");
        System.out.println("╚════════════════════════════════════════════════════╝");
        switch (readLine("请选择 > ")) {
            case "1" -> drinkMenu();
            case "2" -> memberMenu();
            case "3" -> orderMenu();
            case "4" -> statMenu();
            case "0" -> {
                if (confirm("是否退出登录？(y/n) > ")) {
                    System.out.println("👋 再见，" + currentAdmin.getUsername() + "！");
                    currentAdmin = null;
                }
            }
            default -> System.out.println("✗ 无效选项");
        }
    }

    /** 子菜单通用循环：每个操作单独兜底，一个操作出错不会把人踢回主菜单。 */
    private static void subMenu(String title, String[] options, Runnable[] actions) {
        while (true) {
            System.out.println();
            System.out.println("┌────────── " + title + " ──────────┐");
            for (int i = 0; i < options.length; i++) {
                System.out.println("│  " + (i + 1) + ". " + options[i]);
            }
            System.out.println("│  0. 返回主菜单");
            System.out.println("└──────────────────────────────┘");
            String op = readLine("请选择 > ");
            if ("0".equals(op)) {
                return;
            }
            int idx;
            try {
                idx = Integer.parseInt(op) - 1;
            } catch (NumberFormatException e) {
                idx = -1;
            }
            if (idx < 0 || idx >= actions.length) {
                System.out.println("✗ 无效选项");
                continue;
            }
            try {
                actions[idx].run();
            } catch (BusinessException e) {
                System.out.println("✗ " + e.getMessage());
            } catch (DaoException e) {
                System.out.println("✗ 数据库操作失败：" + e.getMessage());
            }
        }
    }

    // ===================== 1. 商品管理 =====================

    private static void drinkMenu() {
        subMenu("商品管理",
                new String[]{"查看全部奶茶", "新增奶茶", "修改奶茶", "下架奶茶", "库存预警查询"},
                new Runnable[]{() -> {
                    printDrinks(DRINK_SVC.listAllActive());
                    pause();
                }, Main::addDrink, Main::updateDrink, Main::deactivateDrink, Main::printLowStock});
    }

    /** 按分类分组打印在售奶茶，低库存高亮。 */
    private static void printDrinks(List<Drink> list) {
        System.out.println();
        System.out.println("========= 在售奶茶清单 =========");
        if (list.isEmpty()) {
            System.out.println("  （暂无在售奶茶）");
        }
        String currentCat = null;
        for (Drink d : list) {
            if (!d.getCategory().equals(currentCat)) {
                currentCat = d.getCategory();
                System.out.println("【" + currentCat + "】");
            }
            System.out.printf("  [%d] %s ¥%-7s 库存 %3d%s%n", d.getId(), padRight(d.getName(), 14),
                    d.getPrice().toPlainString(), d.getStock(),
                    DrinkService.isLowStock(d) ? "  ⚠ 库存预警" : "");
        }
        System.out.println("================================");
    }

    private static void addDrink() {
        System.out.println("┌────────── 新增奶茶 ──────────┐");
        String name = readNonBlank("名称                   > ", 64);
        String category;
        while (true) {
            category = readNonBlank("分类（奶茶/果茶/特调） > ", 16);
            if (DrinkService.CATEGORIES.contains(category)) {
                break;
            }
            System.out.println("✗ 分类只能是：奶茶 / 果茶 / 特调");
        }
        BigDecimal price = readMoney("单价（元）             > ", DrinkService.MAX_PRICE);
        int stock = readInt("初始库存               > ", 0, DrinkService.MAX_STOCK);
        System.out.println("└──────────────────────────────┘");
        Drink d = DRINK_SVC.addDrink(name, category, price, stock);
        System.out.println("✓ 已新增「" + d.getName() + "」，ID = " + d.getId());
        pause();
    }

    private static void updateDrink() {
        long id = readId("要修改的奶茶 ID > ");
        Drink d = DRINK_SVC.findById(id);
        if (d == null || !d.isOnSale()) {
            System.out.println("✗ 奶茶不存在或已下架");
            return;
        }
        System.out.println("当前：" + d);
        BigDecimal price = readMoney("新单价（元） > ", DrinkService.MAX_PRICE);
        int stock = readInt("新库存       > ", 0, DrinkService.MAX_STOCK);
        DRINK_SVC.updateDrink(id, price, stock);
        System.out.println("✓ 已更新「" + d.getName() + "」：¥" + price.toPlainString() + "，库存 " + stock);
        pause();
    }

    private static void deactivateDrink() {
        long id = readId("要下架的奶茶 ID > ");
        Drink d = DRINK_SVC.findById(id);
        if (d == null || !d.isOnSale()) {
            System.out.println("✗ 奶茶不存在或已下架");
            return;
        }
        if (!confirm("确认下架「" + d.getName() + "」？(y/n) > ")) {
            System.out.println("已取消");
            return;
        }
        DRINK_SVC.deactivate(id);
        System.out.println("✓ 已下架「" + d.getName() + "」（软删除，历史订单仍可查到）");
        pause();
    }

    private static void printLowStock() {
        List<Drink> list = STAT_SVC.lowStock();
        System.out.println();
        System.out.println("========= 库存预警（< " + DrinkService.STOCK_WARN_THRESHOLD + " 杯）=========");
        if (list.isEmpty()) {
            System.out.println("  ✓ 暂无库存预警，安心营业");
        }
        for (Drink d : list) {
            System.out.printf("  [%d] %s 库存 %d%n", d.getId(), padRight(d.getName(), 14), d.getStock());
        }
        System.out.println("=====================================");
        if (!list.isEmpty()) {
            System.out.print("建议尽快补货。");
        }
        pause();
    }

    // ===================== 2. 会员管理 =====================

    private static void memberMenu() {
        subMenu("会员管理",
                new String[]{"按手机号查会员", "新增会员", "修改会员信息", "查看全部会员", "会员历史订单"},
                new Runnable[]{Main::findMember, Main::addMember, Main::updateMember,
                        Main::listMembers, Main::memberOrders});
    }

    private static String readPhone(String prompt) {
        while (true) {
            String phone = readLine(prompt);
            if (MemberService.isValidPhone(phone)) {
                return phone;
            }
            System.out.println("✗ 手机号必须是 1 开头的 11 位数字");
        }
    }

    private static void printMember(Member m) {
        System.out.println("============= 会员详情 =============");
        System.out.println("  ID    : " + m.getId());
        System.out.println("  姓名  : " + m.getName());
        System.out.println("  手机  : " + m.getPhone());
        System.out.println("  积分  : " + m.getPoints());
        System.out.println("  入会  : " + (m.getCreatedAt() == null ? "-" : m.getCreatedAt().format(DATE_FMT)));
        System.out.println("====================================");
    }

    private static void findMember() {
        Member m = MEMBER_SVC.findByPhone(readPhone("请输入手机号 > "));
        if (m == null) {
            System.out.println("✗ 未找到该会员");
            return;
        }
        printMember(m);
        pause();
    }

    private static void addMember() {
        String name = readNonBlank("姓名   > ", 32);
        String phone = readPhone("手机号 > ");
        Member m = MEMBER_SVC.addMember(name, phone);
        System.out.println("✓ 新会员入会成功：" + m.getName() + "（ID = " + m.getId() + "，初始积分 0）");
        pause();
    }

    private static void updateMember() {
        Member m = MEMBER_SVC.findByPhone(readPhone("要修改的会员手机号 > "));
        if (m == null) {
            System.out.println("✗ 未找到该会员");
            return;
        }
        printMember(m);
        String name = readLine("新姓名（直接回车保持不变）> ");
        String pointsStr = readLine("新积分（直接回车保持不变）> ");
        int points = m.getPoints();
        if (!pointsStr.isEmpty()) {
            try {
                points = Integer.parseInt(pointsStr);
            } catch (NumberFormatException e) {
                System.out.println("✗ 请输入数字");
                return;
            }
        }
        MEMBER_SVC.updateMember(m.getId(), name.isEmpty() ? m.getName() : name, points);
        System.out.println("✓ 会员信息已更新");
        pause();
    }

    private static void listMembers() {
        List<Member> list = MEMBER_SVC.listAll();
        System.out.println();
        System.out.println("============= 全部会员（" + list.size() + " 人）=============");
        for (Member m : list) {
            System.out.printf("  [%d] %s %s  积分 %d%n", m.getId(), padRight(m.getName(), 8), m.getPhone(), m.getPoints());
        }
        System.out.println("============================================");
        pause();
    }

    private static void memberOrders() {
        Member m = MEMBER_SVC.findByPhone(readPhone("会员手机号 > "));
        if (m == null) {
            System.out.println("✗ 未找到该会员");
            return;
        }
        List<TeaOrder> orders = MEMBER_SVC.listOrders(m.getId());
        System.out.println();
        System.out.println("======= " + m.getName() + " 的历史订单（" + orders.size() + " 笔）=======");
        for (TeaOrder o : orders) {
            System.out.printf("  #%-4d %s  ¥%-8s %s%n", o.getId(), o.getCreatedAt().format(TIME_FMT),
                    o.getTotalAmount().toPlainString(), statusText(o.getStatus()));
        }
        System.out.println("==========================================");
        pause();
    }

    // ===================== 3. 下单结算（核心） =====================

    private static void orderMenu() {
        subMenu("下单结算", new String[]{"新建订单", "查询订单明细"},
                new Runnable[]{Main::placeOrder, Main::queryOrder});
    }

    private static void placeOrder() {
        System.out.println("┌────────── 新建订单 ──────────┐");

        // A. 识别会员 / 散客
        Member member = null;
        while (true) {
            String phone = readLine("请输入会员手机号（输入 0 表示散客）> ");
            if ("0".equals(phone)) {
                break;
            }
            if (!MemberService.isValidPhone(phone)) {
                System.out.println("✗ 手机号必须是 1 开头的 11 位数字");
                continue;
            }
            member = MEMBER_SVC.findByPhone(phone);
            if (member == null && confirm("该手机号还不是会员，是否帮他注册？(y/n) > ")) {
                member = MEMBER_SVC.addMember(readNonBlank("会员姓名 > ", 32), phone);
                System.out.println("✓ 已注册新会员：" + member.getName());
            }
            break;
        }
        if (member != null) {
            System.out.println("✓ 识别到会员：" + member.getName() + "（积分 " + member.getPoints() + "）");
        } else {
            System.out.println("✓ 本单按散客处理（不累计积分）");
        }

        // B. 列出在售奶茶
        List<Drink> onSale = DRINK_SVC.listAllActive();
        if (onSale.isEmpty()) {
            System.out.println("✗ 当前没有在售奶茶，无法下单");
            return;
        }
        printDrinks(onSale);

        // C. 循环加杯（输入 0 完成）
        List<OrderItem> cart = new ArrayList<>();
        while (true) {
            String idStr = readLine("请输入奶茶ID（输入 0 表示完成下单）> ");
            if ("0".equals(idStr)) {
                break;
            }
            long drinkId;
            try {
                drinkId = Long.parseLong(idStr);
            } catch (NumberFormatException e) {
                System.out.println("✗ 请输入数字");
                continue;
            }
            Drink d = onSale.stream().filter(x -> x.getId() == drinkId).findFirst().orElse(null);
            if (d == null) {
                System.out.println("✗ 奶茶不存在或已下架，请重新选择");
                continue;
            }
            int qty = readInt("杯数 > ", 1, DrinkService.MAX_STOCK);
            int inCart = cart.stream().filter(x -> x.getDrinkId() == drinkId).mapToInt(OrderItem::getQuantity).sum();
            if (qty + inCart > d.getStock()) {
                System.out.printf("✗ 库存不足！%s 仅剩 %d 杯%s，请重新选择。%n", d.getName(), d.getStock(),
                        inCart > 0 ? "（购物车里已有 " + inCart + " 杯）" : "");
                continue;
            }
            OrderItem it = new OrderItem(d.getId(), d.getName(), qty, d.getPrice());
            cart.add(it);
            System.out.printf("✓ 已加入：%s × %d = ¥%s（当前合计 ¥%s）%n", d.getName(), qty,
                    it.getSubtotal().toPlainString(), OrderService.sum(cart).toPlainString());
        }
        if (cart.isEmpty()) {
            System.out.println("订单已取消（未选任何奶茶）");
            return;
        }

        // D. 订单确认
        List<OrderItem> items = OrderService.mergeItems(cart);
        BigDecimal total = OrderService.sum(items);
        System.out.println();
        System.out.println("========= 订单清单确认 =========");
        System.out.println("  会员：" + (member == null ? "散客" : member.getName() + "（" + member.getPhone() + "）"));
        System.out.println("  ─────────────────────────────");
        for (OrderItem it : items) {
            System.out.printf("  %s × %-3d = ¥%s%n", padRight(it.getDrinkName(), 12), it.getQuantity(),
                    it.getSubtotal().toPlainString());
        }
        System.out.println("  ─────────────────────────────");
        System.out.println("  合计           ¥" + total.toPlainString());
        if (member != null) {
            System.out.println("  预计积分 +" + OrderService.pointsFor(total));
        }
        System.out.println("================================");
        if (!confirm("确认下单？(y/n) > ")) {
            System.out.println("订单已取消");
            return;
        }

        // E. 事务下单
        OrderService.PlaceOrderResult r = ORDER_SVC.placeOrder(member == null ? null : member.getId(), items);
        System.out.println();
        System.out.println("✓ 订单创建成功！");
        System.out.println("  订单号 : " + r.orderId());
        System.out.println("  总金额 : ¥" + r.totalAmount().toPlainString());
        if (member != null && r.pointsAfter() != null) {
            System.out.printf("  会员「%s」积分 %d → %d%n", member.getName(), r.pointsBefore(), r.pointsAfter());
        }
        System.out.println("  库存已扣减，谢谢惠顾 🧋");
        pause();
    }

    private static void queryOrder() {
        long id = readId("请输入订单号 > ");
        OrderService.OrderDetail detail = ORDER_SVC.findOrderDetail(id);
        if (detail == null) {
            System.out.println("✗ 订单不存在");
            return;
        }
        TeaOrder o = detail.order();
        Member m = detail.member();
        System.out.println();
        System.out.printf("========= 订单 #%d 详情 =========%n", o.getId());
        System.out.println("  会员    : " + (m == null ? "散客" : m.getName() + "（" + m.getPhone() + "）"));
        System.out.println("  下单时间: " + o.getCreatedAt().format(TIME_FMT));
        System.out.println("  状态    : " + statusText(o.getStatus()));
        System.out.println("  ──────────────────────────────");
        for (OrderItem it : detail.items()) {
            System.out.printf("  %s × %-3d ¥%-6s = ¥%s%n", padRight(it.getDrinkName(), 12), it.getQuantity(),
                    it.getUnitPrice().toPlainString(), it.getSubtotal().toPlainString());
        }
        System.out.println("  ──────────────────────────────");
        System.out.println("  合计                       ¥" + o.getTotalAmount().toPlainString());
        System.out.println("================================");
        pause();
    }

    private static String statusText(String status) {
        return TeaOrder.STATUS_PAID.equals(status) ? "已支付" : "已取消";
    }

    // ===================== 4. 数据统计 =====================

    private static void statMenu() {
        subMenu("数据统计", new String[]{"本月销量榜 TOP 3", "本月营业额", "库存预警清单"},
                new Runnable[]{Main::topSales, Main::monthlyAmount, Main::printLowStock});
    }

    private static void topSales() {
        List<StatService.SaleRank> list = STAT_SVC.topSalesThisMonth(3);
        String[] medals = {"🥇", "🥈", "🥉"};
        System.out.println();
        System.out.println("=========  本月销量榜 TOP 3  =========");
        if (list.isEmpty()) {
            System.out.println("  本月暂无销售数据");
        }
        for (int i = 0; i < list.size(); i++) {
            StatService.SaleRank r = list.get(i);
            System.out.printf("  %s  %s 售出 %d 杯   ¥%s%n", medals[i], padRight(r.drinkName(), 12),
                    r.totalQty(), r.totalAmount().toPlainString());
        }
        System.out.println("======================================");
        pause();
    }

    private static void monthlyAmount() {
        StatService.MonthlySummary s = STAT_SVC.monthlySummary();
        System.out.println();
        System.out.println("============= 本月营业额 =============");
        System.out.println("  统计周期 : " + s.period());
        System.out.println("  订单总数 : " + s.orderCount());
        System.out.println("  营业总额 : ¥" + s.totalAmount().toPlainString());
        System.out.println("  客单价   : ¥" + s.avgAmount().toPlainString());
        System.out.println("======================================");
        pause();
    }
}
