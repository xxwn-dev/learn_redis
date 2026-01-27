package dev.xxwon.ticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableRetry
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);   //기본적으로 유지할 스레드 개수
        executor.setMaxPoolSize(50);    //최대 생성 가능한 스레드 개수
        executor.setQueueCapacity(100); //작업 대기 큐의 크기
        executor.setThreadNamePrefix("TicketAsync-");   //스레드 이름 접두사 설정

        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(0);

        executor.setThreadFactory(r ->{
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        executor.initialize();
        return executor;
    }

    @Bean(name = "orderRelayExecutor")
    public Executor orderRelayExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Relay-Async-");

        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(0);

        executor.setThreadFactory(r ->{
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        executor.initialize();
        return executor;
    }
}
