package dev.xxwon.ticket.scheduler;

import dev.xxwon.ticket.application.TicketFacade;
import dev.xxwon.ticket.domain.TicketRepository;
import dev.xxwon.ticket.service.RedisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class StockRecoveryScheduler {

    private final TicketFacade ticketFacade;
    private final RedisStockService redisStockService;
    private final TicketRepository ticketRepository;

    @Scheduled(fixedDelay = 10000)
    public void checkRedisAndRecover(){
        if(ticketFacade.isRedisDown()){
            log.info("Redis is down. Attempting recovery...");

            if(redisStockService.isAlive()){
                log.info("Redis is back! Recovering stock data...");
                ticketRepository.findAll().forEach(ticket -> {
                    redisStockService.warmUpStock(ticket.getId());
                });

                ticketFacade.setRedisDown(false);
                log.info("system recovered from Redis downtime.");
            }
        }
    }
}
