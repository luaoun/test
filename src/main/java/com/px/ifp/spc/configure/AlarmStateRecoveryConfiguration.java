package com.px.ifp.spc.configure;


import com.px.ifp.spc.service.impl.AlarmStateRecoveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * SPC报警状态恢复配置 - 系统启动时自动恢复Redis状态
 */
@Slf4j
@Component
public class AlarmStateRecoveryConfiguration implements ApplicationRunner {

    @Autowired
    private AlarmStateRecoveryService recoveryService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("应用启动完成，开始恢复报警状态...");

        try {
            // 恢复所有指标的报警状态
            recoveryService.recoverAllAlarmStates();

            log.info("报警状态恢复完成");

        } catch (Exception e) {
            log.error("启动时状态恢复失败，系统将继续运行但可能存在状态不一致问题", e);
            // 不抛出异常，避免影响系统启动
        }
    }
}
