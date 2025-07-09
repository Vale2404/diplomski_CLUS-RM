package redescriptionmining;

import gnu.trove.map.hash.THashMap;

import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentTHashMap<K, V> extends THashMap<K, V> {
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public V put(K key, V value) {
        int index;
        V val;
        try {
            lock.lock();
            index = this.insertKey(key);
            val = this.doPut(value, index);
        } finally {
            lock.unlock();
        }
        return val;
    }

    private V doPut(V value, int index) {
        V previous = null;
        boolean isNewMapping = true;
        if (index < 0) {
            index = -index - 1;
            previous = this._values[index];
            isNewMapping = false;
        }

        this._values[index] = value;
        if (isNewMapping) {
            this.postInsertHook(this.consumeFreeSlot);
        }

        return previous;
    }
}
