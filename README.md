# 🧋 奶茶物语 · 奶茶店控制台点单系统

> Java 21 + JDBC + MySQL 8 的三层架构控制台项目。模拟一家大学城奶茶店的收银点单：店员登录、商品管理、会员积分、**一单多杯下单（JDBC 事务）**、销量榜 / 营业额 / 库存预警统计。

## 功能一览

| 模块 | 功能 |
| --- | --- |
| 店员账号 | 注册（用户名唯一、密码 ≥ 6 位）、登录（密码 SHA-256 摘要存储，失败统一提示"用户名或密码错误"） |
| 商品管理 | 按分类分组查看、新增、改价改库存、**软删除下架**（`is_active = 0`，历史订单仍可查） |
| 会员管理 | 新增（手机号唯一）、按手机号查询、修改姓名 / 积分、查看全部、会员历史订单 |
| 下单结算 | 会员 / 散客识别（未注册可当场开卡）→ 循环加杯 → 订单确认 → **事务内**写主表 + 批量写明细 + 扣库存 + 加积分 |
| 订单查询 | 按订单号查询，**多表 JOIN** 带出奶茶名、单价快照、小计 |
| 数据统计 | 本月销量榜 TOP 3（`SUM + GROUP BY + ORDER BY + LIMIT`）、本月营业额与客单价、库存预警（< 10 杯） |

## 技术亮点

1. **JDBC 事务保证下单原子性**：同一个 `Connection` 上 `setAutoCommit(false)`，四步全部成功才 `commit`，任意一步失败整单 `rollback`。
2. **条件更新防超卖**：`UPDATE drink SET stock = stock - ? WHERE id = ? AND stock >= ?`，影响行数不为 1 立即回滚，并发下单也不会把库存扣成负数。
3. **一对多订单模型 + 单价快照**：`tea_order` 1 : N `order_item`，明细保存下单时的 `unit_price`，之后改价不影响历史账目。
4. **分层清晰**：`entity`（实体）/ `dao`（只做 SQL）/ `service`（校验与业务规则）/ `Main`（只做交互），DAO 的事务方法接收外部 `Connection`，由 Service 控制事务边界。
5. **安全与健壮性**：全部 SQL 使用 `PreparedStatement` 防注入；密码只存 SHA-256 摘要；所有输入（空串、字母、负数、超长、越界）都有兜底，界面不会抛出 `SQLException` 堆栈；数据库唯一索引兜底并发注册。
6. **金额全程 `BigDecimal`**：DECIMAL ↔ BigDecimal，避免 `double` 精度误差。
7. **可演示的回滚开关**：启动时加 `-Dmilktea.rollbackDemo=true`，下单会在写完主表和明细后人为失败，可直接用 SQL 验证数据没有残留。

## 目录结构

```
milktea-saga/
├── README.md
├── .gitignore
├── sql/
│   └── init.sql                  # 建库建表 + 测试数据 + 自检 SQL
├── docs/
│   ├── 设计文档.md               # 需求、模块树、ER 图、下单时序、事务设计
│   ├── 测试报告.md               # T01 ~ T15 用例及实测结果
│   └── 面试问答.md               # 项目答辩 / 面试高频问题
├── backend/                      # Maven 工程
│   ├── pom.xml                   # 唯一依赖 mysql-connector-j
│   └── src/main/
│       ├── resources/db.properties.example
│       └── java/com/tea/
│           ├── Main.java         # 控制台菜单（表现层）
│           ├── entity/           # Admin / Drink / Member / TeaOrder / OrderItem
│           ├── dao/              # 5 个 DAO + DaoException
│           ├── service/          # Admin / Drink / Member / Order / Stat Service + BusinessException
│           └── util/             # DbUtil / PasswordUtil / ConsoleUtil
└── frontend/                     # 预留：后续做 Web 版时使用
```

## 快速开始

**环境**：JDK 21、Maven 3.8+、MySQL 8.0+

1. **初始化数据库**（会重建 `milktea_saga` 库）

   ```bash
   mysql -uroot -p < sql/init.sql
   ```

   末尾的自检应输出 `3  9  4  4  8`（店员 / 奶茶 / 会员 / 订单 / 明细条数）。

2. **配置数据库连接**（默认 `localhost:3306`，`root / 123456`，一致可跳过）

   ```bash
   cp backend/src/main/resources/db.properties.example backend/src/main/resources/db.properties
   # 修改 db.password 为本机 MySQL 密码；db.properties 已被 .gitignore 忽略
   ```

   也可以用环境变量 `MILKTEA_DB_URL` / `MILKTEA_DB_USER` / `MILKTEA_DB_PASSWORD` 覆盖。

3. **运行**

   ```bash
   cd backend
   mvn -q compile exec:java
   ```

   或在 IntelliJ IDEA 中打开 `backend/`，直接运行 `com.tea.Main`。

   > Windows 命令行若中文乱码，先执行 `chcp 65001`；在 IDEA 中运行不受影响。

**测试账号**：`xiaoli / 123456`、`xiaowang / 123456`、`linboss / 888888`
**测试会员**：`13800138001`（王同学，280 积分）

## 事务回滚演示

```bash
cd backend
mvn -q compile exec:java -Dmilktea.rollbackDemo=true
```

登录后下任意一单，会看到 `✗ 下单失败，已回滚：【回滚演示】...`。之后用以下 SQL 确认订单数、库存、积分都没有变化：

```sql
SELECT (SELECT COUNT(*) FROM tea_order)  AS orders,
       (SELECT COUNT(*) FROM order_item) AS items,
       (SELECT stock  FROM drink  WHERE id = 1) AS pearl_stock,
       (SELECT points FROM member WHERE id = 1) AS wang_points;
```

## 数据库设计

5 张表：`admin`（店员）、`drink`（奶茶）、`member`（会员）、`tea_order`（订单主表，避开保留字 `order`）、`order_item`（订单明细）。
`tea_order.member_id → member.id`（散客为 NULL），`order_item.order_id → tea_order.id`，`order_item.drink_id → drink.id`。详见 [docs/设计文档.md](docs/设计文档.md)。

## 测试

15 条验收用例（登录、注册、CRUD、软删除、会员下单与积分、散客下单、库存不足、JOIN 查询、统计、异常输入、事务回滚）全部在 MySQL 8 上实际运行通过，过程与输出见 [docs/测试报告.md](docs/测试报告.md)。
