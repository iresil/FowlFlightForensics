package FowlFlightForensics.util;

import FowlFlightForensics.domain.dto.IncidentRanked;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The {@code CommandUtils} class contains code that has been written with the Command pattern in mind.
 */
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

    /**
     * Generates a {@code Predicate} to be used within the {@code filter} method, which checks if an object containing the
     * same as the current value in the same field has been previously "seen" or not. The first appearance of each value
     * will be considered "seen", which will eventually lead to the object in question being the only one that is actually
     * kept after the {@code filter} operation.
     * @param keyExtractor A functional interface referring to the field whose value appearance will be checked.
     * @return A {@code Predicate} to be used within the {@code filter} method.
     */
    public static java.util.function.Predicate<? super Object> distinctByKey(Function<IncidentRanked, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply((IncidentRanked) t));
    }
}
