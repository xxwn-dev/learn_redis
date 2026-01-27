package dev.xxwon.ticket.application.outbox;

import dev.xxwon.ticket.config.RabbitConfig;
import dev.xxwon.ticket.domain.Outbox;
import dev.xxwon.ticket.domain.OutboxRepository;
import dev.xxwon.ticket.domain.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processedOutbox(){

        List<Outbox> pendingOutboxes = outboxRepository.findAllByStatus(OutboxStatus.INIT, PageRequest.of(0,100));
        if(pendingOutboxes.isEmpty()) return;

        log.info("OutboxScheduler: Processing {} pending messages", pendingOutboxes.size());
        for(Outbox outbox : pendingOutboxes){
            try{
                rabbitTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, RabbitConfig.ORDER_ROUTING_KEY, outbox.getPayload());
                outbox.markProcessed();
            } catch (Exception e) {
                log.error("OutboxScheduler: Failed to process outbox id:{}", outbox.getId(), e);
            }
        }

    }

}
