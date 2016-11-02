package com.huatu.ztk.arena;

import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by shaojieyue
 * Created time 2016-07-06 16:04
 */
public class AATest {
    private static final Logger logger = LoggerFactory.getLogger(AATest.class);

    public static void main(String[] args) {
        final AA aa = new AATest().newAA();
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    aa.a();
                    try {
                        Thread.sleep(org.apache.commons.lang3.RandomUtils.nextInt(10,1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        final Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    aa.b();
                    try {
                        Thread.sleep(org.apache.commons.lang3.RandomUtils.nextInt(10,1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        final Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    aa.c();
                }
            }
        });
        thread.start();
        thread1.start();
        thread2.start();
    }

    public AA newAA(){
        return new AA();
    }

    public class AA{
        public synchronized void a(){
            System.out.println("a->"+System.nanoTime());
        }

        public synchronized void b(){
            System.out.println("b->"+System.nanoTime());
        }

        public  void c(){
            System.out.println("c->"+System.nanoTime());
        }
    }
}
