import java.util.concurrent.locks.ReentrantLock;
import java.util.LinkedList;

public class ThreadPool {
    private final ReentrantLock lock = new ReentrantLock();
    private final Thread[] workerThreads;
    private final LinkedList<Runnable> tasks = new LinkedList<>();
    private boolean isRunning = true;

    public ThreadPool(int nThreads) {
        workerThreads = new Thread[nThreads];
        for (int i = 0; i < workerThreads.length; ++i) {
            workerThreads[i] = new Thread(() -> {
                while (isRunning) {
                    Runnable task;
                    lock.lock();
                    try {
                        if (!tasks.isEmpty()) {
                            task = tasks.poll();
                        } else {
                            continue;
                        }
                    } finally {
                        lock.unlock();
                    }
                    if (task != null) {
                        try {
                            task.run(); // Run the task
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            workerThreads[i].start();
        }
    }

    public void execute(Runnable runnable) {
        lock.lock();
        try {
            tasks.offer(runnable);
        } finally {
            lock.unlock();
        }
    }

    // Method to stop the ThreadPool
    public void shutdown() {
        isRunning = false;
        for (Thread workerThread : workerThreads) {
            workerThread.interrupt();
        }
    }
}