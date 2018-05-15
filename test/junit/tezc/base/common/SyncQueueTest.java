package junit.tezc.base.common;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncQueueTest
{
    private SyncQueue<String> queue;
    private AtomicInteger count;

    public SyncQueueTest()
    {
        queue = new SyncQueue<>();
        count = new AtomicInteger(0);
    }

    @Test
    public void run()
    {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            Set<Callable<Integer>> callables = new HashSet<Callable<Integer>>();
            for (int i = 0; i < 10000; i++) {
                callables.add(new Callable<Integer>()
                {
                    public Integer call() throws Exception
                    {
                        queue.addEvent(UUID.randomUUID().toString());
                        return 0;
                    }
                });
            }

            List<Future<Integer>> futures = executorService.invokeAll(callables);
            for (Future<Integer> future : futures) {
                future.get();
            }
            executorService.shutdown();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        Assert.assertEquals(queue.getItems().size(), 10000);
    }
}
