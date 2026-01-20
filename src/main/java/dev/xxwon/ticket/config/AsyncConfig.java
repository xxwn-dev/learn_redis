package dev.xxwon.ticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);   //기본적으로 유지할 스레드 개수
        executor.setMaxPoolSize(50);    //최대 생성 가능한 스레드 개수
        executor.setQueueCapacity(100); //작업 대기 큐의 크기
        executor.setThreadNamePrefix("TicketAsync-");   //스레드 이름 접두사 설정
        executor.initialize();
        return executor;
    }
}
