package FowlFlightForensics.util;

public class Consts {
    /**
     * Configures how strict the applied validations should be (set to {@code true} for strict, {@code false} for lenient).
     */
    public static final boolean INVALID_INCIDENTS_FILTER_STRICT = true;
    /**
     * The minimum allowed percentage of raw incidents to be considered invalid after applying the validations, per rule.
     * This will be used as a threshold to check whether the rule will be applied or not, if {@code INVALID_INCIDENTS_FILTER_STRICT}
     * is set to {@code false}. Setting this to anything lower than its current value will result to very few validations
     * being applied, and a lot of invalid data being contained in the final CSV.
     */
    public static final float INVALID_INCIDENTS_PERCENTAGE_LIMIT_MIN = 0.03f;
    /**
     * The maximum allowed percentage of raw incidents to be considered invalid after applying the validations, per rule.
     * This will be used as a threshold to check whether the rule will be applied or not, if {@code INVALID_INCIDENTS_FILTER_STRICT}
     * is set to {@code true}. Setting this to anything higher than its current value can result to that many validations
     * being applied, that not much data is left to transform and aggregate.
     */
    public static final float INVALID_INCIDENTS_PERCENTAGE_LIMIT_MAX = 0.48f;

    /**
     * The number of messages that can be sent to {@code Kafka} in the same batch, by the {@code Producer}.
     */
    public static final int NUM_OF_MESSAGES_PER_BATCH = 1000;
    /**
     * A default value to use for the field that will eventually contain the timestamp of the last message that was sent.
     * This had to be a value that wouldn't make sense in the context of milliseconds.
     */
    public static final long LAST_MESSAGE_TIME_MILLIS_DEFAULT_VALUE = -1;
}
