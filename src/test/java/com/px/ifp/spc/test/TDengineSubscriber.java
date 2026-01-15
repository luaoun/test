package com.px.ifp.spc.test;

import com.taosdata.jdbc.tmq.ConsumerRecord;
import com.taosdata.jdbc.tmq.ConsumerRecords;
import com.taosdata.jdbc.tmq.TaosConsumer;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TDengine 3.3.6 原生 TMQ 订阅实现
 * 监听 measure_tsdb.sensor_data 超级表的 temperature 字段变化
 */
public class TDengineSubscriber {

    // 配置参数
    private static final String TDENGINE_HOST = "192.168.41.91";
    private static final int TDENGINE_PORT = 6030;
    private static final String TDENGINE_USER = "root";
    private static final String TDENGINE_PASSWORD = "taosdata";
    private static final String DATABASE = "measure_tsdb";
    private static final String TOPIC_NAME = "topic_meters";
    private static final String CONSUMER_GROUP = "sensor_data_monitor_group";

    // 运行状态标志
    private static final AtomicBoolean running = new AtomicBoolean(true);

    // 统计信息
    private static long messageCount = 0;
    private static long lastReportTime = System.currentTimeMillis();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void main(String[] args) {
        // 注册关闭钩子，实现优雅退出
        registerShutdownHook();

        System.out.println("========================================");
        System.out.println("TDengine TMQ 原生订阅 v3.3.6");
        System.out.println("========================================");

        // 检查服务端连接和版本
        if (!checkServerConnection()) {
            System.err.println("❌ 无法连接到 TDengine 服务器，程序退出");
            System.exit(1);
        }

        // 创建并启动订阅
        startSubscription();
    }

    /**
     * 检查服务端连接和版本兼容性
     */
    private static boolean checkServerConnection() {
        String jdbcUrl = String.format("jdbc:TAOS-RS://%s:6041/%s?charset=utf8mb4&user=%s&password=%s",
                TDENGINE_HOST, DATABASE, TDENGINE_USER, TDENGINE_PASSWORD);
        try  {
            Class.forName("com.taosdata.jdbc.rs.RestfulDriver");

            Connection conn = DriverManager.getConnection(jdbcUrl);
            Statement stmt = conn.createStatement();
            // 获取服务端版本
            ResultSet rs = stmt.executeQuery("SELECT server_version()");
            if (rs.next()) {
                String serverVersion = rs.getString(1);
                System.out.println("✓ TDengine 服务端版本: " + serverVersion);
            }

            // 检查数据库是否存在
            rs = stmt.executeQuery("SHOW DATABASES");
            boolean dbExists = false;
            while (rs.next()) {
                if (DATABASE.equals(rs.getString(1))) {
                    dbExists = true;
                    break;
                }
            }

            if (!dbExists) {
                System.err.println("⚠️  警告: 数据库 " + DATABASE + " 不存在");
                return false;
            }

            System.out.println("✓ 数据库 " + DATABASE + " 存在");

            // 检查主题是否存在
            rs = stmt.executeQuery("SHOW TOPICS");
            boolean topicExists = false;
            while (rs.next()) {
                if (TOPIC_NAME.equals(rs.getString(1))) {
                    topicExists = true;
                    break;
                }
            }

            if (!topicExists) {
                System.err.println("⚠️  警告: 主题 " + TOPIC_NAME + " 不存在");
                System.err.println("请先创建主题: CREATE TOPIC " + TOPIC_NAME +
                                 " AS SELECT * FROM " + DATABASE + ".sensor_data;");
                return false;
            }

            System.out.println("✓ 主题 " + TOPIC_NAME + " 已创建");
            return true;

        } catch (Exception e) {
            System.err.println("❌ 连接检查失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 启动 TMQ 订阅
     */
    private static void startSubscription() {
        // 配置 TMQ 消费者参数（针对 TDengine 3.3.6 优化）
        Properties config = new Properties();

        // 基本连接配置
        config.setProperty("bootstrap.servers", TDENGINE_HOST + ":" + TDENGINE_PORT);
        config.setProperty("td.connect.user", TDENGINE_USER);
        config.setProperty("td.connect.pass", TDENGINE_PASSWORD);

        // 消费者组配置
        config.setProperty("group.id", CONSUMER_GROUP);
        config.setProperty("client.id", CONSUMER_GROUP + "_client_" + System.currentTimeMillis());

        // 消息配置
        config.setProperty("msg.with.table.name", "true");  // 消息包含表名
        // 注意：3.3.6 版本使用默认反序列化器，不需要显式指定

        // 消费策略（3.3.6 推荐配置）
        config.setProperty("auto.offset.reset", "earliest");  // earliest 或 latest
        config.setProperty("enable.auto.commit", "true");
        config.setProperty("auto.commit.interval.ms", "5000");  // 5秒自动提交一次

        // 性能调优参数
        config.setProperty("session.timeout.ms", "30000");  // 会话超时 30 秒
        config.setProperty("max.poll.interval.ms", "300000");  // 最大轮询间隔 5 分钟
        config.setProperty("max.poll.records", "500");  // 每次拉取最多 500 条记录

        TaosConsumer<Map<String, Object>> consumer = null;

        try {
            // 创建消费者
            consumer = new TaosConsumer<>(config);
            System.out.println("✓ TMQ 消费者创建成功");

            // 订阅主题
            consumer.subscribe(Collections.singletonList(TOPIC_NAME));
            System.out.println("✓ 已订阅主题: " + TOPIC_NAME);

            System.out.println("========================================");
            System.out.println("开始监听 " + DATABASE + ".sensor_data");
            System.out.println("监听所有子表的 temperature 字段变化");
            System.out.println("按 Ctrl+C 停止订阅");
            System.out.println("========================================\n");

            // 持续消费数据
            consumeMessages(consumer);

        } catch (Exception e) {
            System.err.println("❌ 订阅失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理资源
            if (consumer != null) {
                try {
                    consumer.unsubscribe();
                    consumer.close();
                    System.out.println("\n✓ 订阅已关闭");
                } catch (Exception e) {
                    System.err.println("关闭消费者时出错: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 消费消息主循环
     */
    private static void consumeMessages(TaosConsumer<Map<String, Object>> consumer) {
        while (running.get()) {
            try {
                // 拉取消息（超时 1 秒）
                ConsumerRecords<Map<String, Object>> records = consumer.poll(Duration.ofMillis(1000));

                if (records == null || records.isEmpty()) {
                    continue;
                }

                // 处理每条消息
                for (ConsumerRecord<Map<String, Object>> record : records) {
                    processMessage(record);
                    messageCount++;
                }

                // 定期输出统计信息（每 10 秒）
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastReportTime > 10000) {
                    System.out.println(String.format("\n[统计] 已处理消息数: %d 条", messageCount));
                    lastReportTime = currentTime;
                }

            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("❌ 消费消息时出错: " + e.getMessage());
                    e.printStackTrace();

                    // 出错后短暂休眠，避免快速失败循环
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    /**
     * 处理单条消息
     */
    private static void processMessage(ConsumerRecord<Map<String, Object>> record) {
        try {
            Map<String, Object> data = record.value();

            if (data == null || data.isEmpty()) {
                return;
            }

            // 提取数据字段
            String tbname = (String) data.get("tbname");
            Timestamp ts = (Timestamp) data.get("ts");
            Object tempObj = data.get("temperature");

            if (tempObj != null) {
                // 解析温度值
                double temperature = parseTemperature(tempObj);

                // 格式化输出
                String timeStr = ts != null ? sdf.format(ts) : "N/A";
                System.out.printf("[%s] 子表: %-20s 时间: %s  温度: %.2f°C%n",
                        sdf.format(new Date()),
                        tbname != null ? tbname : "unknown",
                        timeStr,
                        temperature);

                // 业务逻辑处理
                handleTemperatureChange(tbname, temperature, ts);

            } else {
                // 如果没有 temperature 字段，输出原始数据用于调试
                System.out.println("[DEBUG] 接收到的数据: " + data);
            }

        } catch (Exception e) {
            System.err.println("处理消息时出错: " + e.getMessage());
        }
    }

    /**
     * 解析温度值（兼容多种数值类型）
     */
    private static double parseTemperature(Object tempObj) {
        if (tempObj instanceof Number) {
            return ((Number) tempObj).doubleValue();
        } else if (tempObj instanceof String) {
            return Double.parseDouble((String) tempObj);
        } else {
            throw new IllegalArgumentException("无法解析温度值: " + tempObj);
        }
    }

    /**
     * 处理温度变化的业务逻辑
     */
    private static void handleTemperatureChange(String tbname, double temperature, Timestamp ts) {
        // 温度阈值检查
        if (temperature > 30.0) {
            System.out.println(String.format("  ⚠️  警告: %s 温度过高 (%.2f°C)！", tbname, temperature));
        } else if (temperature < 10.0) {
            System.out.println(String.format("  ⚠️  警告: %s 温度过低 (%.2f°C)！", tbname, temperature));
        }

        // 这里可以添加更多业务逻辑：
        // 1. 存储到其他数据库
        // 2. 发送告警通知
        // 3. 触发自动化操作
        // 4. 数据统计和分析
    }

    /**
     * 注册关闭钩子，实现优雅退出
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\n接收到停止信号，正在优雅关闭...");
            running.set(false);

            // 等待消费循环结束
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("程序已退出");
        }));
    }
}
