import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class CustomConcurrentHashMap<K, V> {
    private final Map<K, V> internalMap;
    private final ReentrantLock lock;

    public CustomConcurrentHashMap() {
        this.internalMap = new HashMap<>();
        this.lock = new ReentrantLock();
    }

    public V put(K key, V value) {
        lock.lock();
        try {
            return internalMap.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    public V get(K key) {
        lock.lock();
        try {
            return internalMap.get(key);
        } finally {
            lock.unlock();
        }
    }

    public V remove(K key) {
        lock.lock();
        try {
            return internalMap.remove(key);
        } finally {
            lock.unlock();
        }
    }
}
