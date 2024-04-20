package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.yaml.snakeyaml.events.Event;

import javax.annotation.Resource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    ShopServiceImpl shopService;

    @Resource
    CacheClient cacheClient;

    @Resource
    RedisIdWorker redisIdWorker;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 创建500个线程池
     */
    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void testsaveshop() {
        Shop shop = shopService.getById(1L);

        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
    }


    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

    @Test
    void testRedisson() throws InterruptedException {
        //获取锁（可重入）
        RLock lock = redissonClient.getLock("anyLock");
        //尝试获取锁(第一个参数：获取锁的等时间，第二个参数：过期时间)
        boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
        //判断锁是否获取成功
        if (isLock){
            try {
                System.out.println("执行业务");
            }finally {
                //释放锁
                lock.unlock();
            }
        }
    }
}
