package dev.xxwon.ticket;

import dev.xxwon.ticket.domain.Ticket;
import dev.xxwon.ticket.domain.TicketRepository;
import dev.xxwon.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("100명이 동시에 티켓 구매한다")
    void purchase_concurrency_test() throws InterruptedException {
        //given
        int numberOfThreads = 100;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        //멀티스레드환경만들어주는도구 try-with-resources구문으로 자동 종료
        try (ExecutorService executorService = Executors.newFixedThreadPool(32)) {

            //when
            for (int i = 0; i < numberOfThreads; i++) {
                executorService.submit(() -> {
                    try {
                        ticketService.purchase(1L); // Assuming ticket ID is 1
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();  //모든 구매 요청이 끝날 때까지 대기
        }
        Ticket ticket = ticketRepository.findById(1L).orElseThrow();
        System.out.println("Remaining tickets: " + ticket.getAvailableQuantity());
        assertThat(ticket.getAvailableQuantity()).isEqualTo(0);
    }
}
