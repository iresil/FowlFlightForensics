package FowlFlightForensics.util;

import FowlFlightForensics.domain.IncidentRanked;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CommandUtils {
    @FunctionalInterface
    public interface AddingFunction<T, R> {
        void add(T target, R value);
    }

    @FunctionalInterface
    public interface PuttingFunction<T, K, V> {
        void put(T target, K key, V value);
    }

    @FunctionalInterface
    public interface SettingFunction<T, K, V> {
        void set(T target, K index, V value);
    }

    public static java.util.function.Predicate<? super Object> distinctByKey(Function<IncidentRanked, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply((IncidentRanked) t));
    }
}
