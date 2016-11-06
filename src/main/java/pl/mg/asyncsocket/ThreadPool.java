package pl.mg.asyncsocket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ThreadPool {

    private final static int THREAD_COUNT = 4;
    private final ExecutorService executor;

    ThreadPool(){
        this.executor = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    void execute(Runnable r){
        executor.execute(r);
    }

    void shutdown(){
        executor.shutdown();
    }

}
