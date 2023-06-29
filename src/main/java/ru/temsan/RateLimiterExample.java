package ru.temsan;

import com.google.common.util.concurrent.RateLimiter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

public class RateLimiterExample {
    public static void main(String[] args) {
        Queue<String> queue = new LinkedList<>();
        Semaphore semaphore = new Semaphore(0); // Семафор для синхронизации между продюсером и консьюмером
        int producerRate = 1000; // Ограничение скорости продюсера (количество элементов в секунду)
        int consumerDelay = 200; // Задержка консьюмера (в миллисекундах)
        int maxQueueSize = 100; // Максимальный размер очереди

        Producer producer = new Producer(queue, semaphore, producerRate, maxQueueSize);
        Consumer consumer = new Consumer(queue, semaphore, consumerDelay);

        producer.start();
        consumer.start();

        // Запускаем таймер, чтобы остановить продюсера и консьюмера через некоторое время
        TimerTask stopTask = new TimerTask() {
            public void run() {
                producer.stopProducing();
                consumer.stopConsuming();
            }
        };

        Timer timer = new Timer();
        timer.schedule(stopTask, 5000); // Остановить продюсера и консьюмера через 5 секунд

    }

    static class Producer extends Thread {
        private volatile boolean isProducing; // Флаг для контроля работы продюсера
        private Queue<String> queue;
        private Semaphore semaphore;
        private int rate;
        private final RateLimiter rateLimiter;
        private int maxQueueSize;

        public Producer(Queue<String> queue, Semaphore semaphore, int rate, int maxQueueSize) {
            this.queue = queue;
            this.semaphore = semaphore;
            this.rate = rate;
            this.maxQueueSize = maxQueueSize;
            this.isProducing = true;
            this.rateLimiter = RateLimiter.create(rate);
        }

        public void stopProducing() {
            isProducing = false;
        }

        public void run() {
            while (isProducing) {
                // Ожидаем, чтобы соответствовать ограничению скорости
                rateLimiter.acquire();

                // Добавляем элемент в очередь
                try {
                    semaphore.acquire();
                    String item = "Item " + System.currentTimeMillis();

                    synchronized (queue) {
                        if (queue.size() < maxQueueSize) {
                            queue.add(item);
                            System.out.println("Produced: " + item);
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    static class Consumer extends Thread {
        private volatile boolean isConsuming; // Флаг для контроля работы консьюмера
        private Queue<String> queue;
        private Semaphore semaphore;
        private int delay;

        public Consumer(Queue<String> queue, Semaphore semaphore, int delay) {
            this.queue = queue;
            this.semaphore = semaphore;
            this.delay = delay;
            this.isConsuming = true;
        }

        public void stopConsuming() {
            isConsuming = false;
        }

        public void run() {
            while (isConsuming) {
                try {
                    semaphore.release();
                    String item;

                    synchronized (queue) {
                        item = queue.poll();
                    }

                    if (item != null) {
                        System.out.println("Consumed: " + item);
                    }

                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}