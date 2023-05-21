import java.util.concurrent.locks.ReentrantLock;
import java.util.LinkedList;

public class CustomExecutors {

    public static ThreadPool newFixedThreadPool(int nThreads) {
        return new ThreadPool(nThreads);
    }
}

