package dev.traveler.core.common.api;

public interface TravelerRegistry<K, V> {
    V resolve(K key);
}
