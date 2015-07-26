package org.wf.dp.dniprorada.base.dao;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.map.DefaultedMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Random;

/**
 * Utility class which creates random value of given type.
 */
public final class RandomUtils {
    private static final int RANDOM_STRING_SIZE = 10;
    private static final int DATE_WINDOW_DAYS = 30;

    private static final Random RANDOM = new Random();

    private RandomUtils() {
    }

    private interface Randomizer<T> {
        T random(Class<T> givenType);
    }

    private static final Randomizer<Byte> BYTE_RANDOMIZER = new Randomizer<Byte>() {
        public Byte random(Class<Byte> givenType) {
            return (byte) RANDOM.nextInt();
        }
    };

    private static final Randomizer<Short> SHORT_RANDOMIZER = new Randomizer<Short>() {
        public Short random(Class<Short> givenType) {
            return (short) RANDOM.nextInt();
        }
    };

    private static final Randomizer<Integer> INTEGER_RANDOMIZER = new Randomizer<Integer>() {
        public Integer random(Class<Integer> givenType) {
            return RANDOM.nextInt();
        }
    };

    private static final Randomizer<Long> LONG_RANDOMIZER = new Randomizer<Long>() {
        public Long random(Class<Long> givenType) {
            return RANDOM.nextLong();
        }
    };

    private static final Randomizer<Float> FLOAT_RANDOMIZER = new Randomizer<Float>() {
        public Float random(Class<Float> givenType) {
            return RANDOM.nextFloat();
        }
    };

    private static final Randomizer<Double> DOUBLE_RANDOMIZER = new Randomizer<Double>() {
        public Double random(Class<Double> givenType) {
            return RANDOM.nextDouble();
        }
    };

    private static final Randomizer<Boolean> BOOLEAN_RANDOMIZER = new Randomizer<Boolean>() {
        public Boolean random(Class<Boolean> givenType) {
            return RANDOM.nextBoolean();
        }
    };

    private static final Randomizer<Character> CHARACTER_RANDOMIZER = new Randomizer<Character>() {
        public Character random(Class<Character> givenType) {
            return (char) RANDOM.nextInt();
        }
    };

    private static final Randomizer<String> STRING_RANDOMIZER = new Randomizer<String>() {
        public String random(Class<String> givenType) {
            return RandomStringUtils.random(RANDOM_STRING_SIZE, true, false);
        }
    };

    private static final Randomizer<DateTime> DATE_TIME_RANDOMIZER = new Randomizer<DateTime>() {
        public DateTime random(Class<DateTime> givenType) {
            return DateTime.now().minusDays(RANDOM.nextInt(DATE_WINDOW_DAYS) - RANDOM.nextInt(DATE_WINDOW_DAYS));
        }
    };

    private static final Randomizer<Enum> ENUM_RANDOMIZER = new Randomizer<Enum>() {
        public Enum random(Class<Enum> givenType) {
            Enum[] enumValues = givenType.getEnumConstants();
            return enumValues[RANDOM.nextInt(enumValues.length)];
        }
    };

    private static final Randomizer<Object> DEFAULT_RANDOMIZER = new Randomizer<Object>() {
        public Object random(Class<Object> givenType) {
            return null;
        }
    };

    private static final ImmutableMap<Class<?>, Randomizer> RANDOMIZERS = ImmutableMap.<Class<?>, Randomizer>builder()
            .put(byte.class, BYTE_RANDOMIZER)
            .put(Byte.class, BYTE_RANDOMIZER)
            .put(short.class, SHORT_RANDOMIZER)
            .put(Short.class, SHORT_RANDOMIZER)
            .put(int.class, INTEGER_RANDOMIZER)
            .put(Integer.class, INTEGER_RANDOMIZER)
            .put(long.class, LONG_RANDOMIZER)
            .put(Long.class, LONG_RANDOMIZER)
            .put(float.class, FLOAT_RANDOMIZER)
            .put(Float.class, FLOAT_RANDOMIZER)
            .put(double.class, DOUBLE_RANDOMIZER)
            .put(Double.class, DOUBLE_RANDOMIZER)
            .put(boolean.class, BOOLEAN_RANDOMIZER)
            .put(Boolean.class, BOOLEAN_RANDOMIZER)
            .put(char.class, CHARACTER_RANDOMIZER)
            .put(Character.class, CHARACTER_RANDOMIZER)
            .put(String.class, STRING_RANDOMIZER)
            .put(DateTime.class, DATE_TIME_RANDOMIZER)
            .put(Enum.class, ENUM_RANDOMIZER)
            .build();

    /**
     * Creates random value of given type
     * @see RandomUtils#RANDOMIZERS - supported types
     *
     * @param type creates random value of given type
     * @return random value or default value see {@link RandomUtils#DEFAULT_RANDOMIZER} for details
     */
    public static Object getRandomValueOf(Class<?> type) {
        Map<Class<?>, Randomizer> randomizers = DefaultedMap.decorate(RANDOMIZERS, DEFAULT_RANDOMIZER);

        //TODO: find out better solution for enums
        Randomizer randomizer = randomizers.get(type.isEnum() ? Enum.class : type);

        return randomizer.random(type);
    }

}
