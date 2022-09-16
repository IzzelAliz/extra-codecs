package io.izzel.util.codec.common;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeCodecTest {

    @Test
    void testPrimitiveTypes() {
        Class<?>[] types = {boolean.class, byte.class, short.class, int.class, long.class, float.class, double.class, String.class};
        Object[] values = {true, (byte) 42, (short) 42, 42, 42L, 42F, 42D, "42"};
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            Object value = values[i];
            DataResult<Object> result = TypeCodec.<Object>of(type).encodeStart(JavaPrimitiveOps.INSTANCE, value);
            assertTrue(result.result().isPresent(), "Result is " + result.error());
            assertEquals(result.result().get(), value);
        }
    }

    @Test
    void testPrimitiveArray() {
        int[] ints = {42};
        DataResult<int[]> intsResult = TypeCodec.of(int[].class).parse(JavaPrimitiveOps.dynamic(ints));
        assertTrue(intsResult.result().isPresent(), "Result is " + intsResult.error());
        assertArrayEquals(ints, intsResult.result().get());
        long[] longs = {42L};
        DataResult<long[]> longsResult = TypeCodec.of(long[].class).parse(JavaPrimitiveOps.dynamic(longs));
        assertTrue(longsResult.result().isPresent(), "Result is " + longsResult.error());
        assertArrayEquals(longs, longsResult.result().get());
        byte[] bytes = {42};
        DataResult<byte[]> bytesResult = TypeCodec.of(byte[].class).parse(JavaPrimitiveOps.dynamic(bytes));
        assertTrue(bytesResult.result().isPresent(), "Result is " + bytesResult.error());
        assertArrayEquals(bytes, bytesResult.result().get());
    }

    @Test
    void testNonDfuPrimitiveArray() {
        assertThrows(IllegalArgumentException.class, () -> TypeCodec.of(short[].class));
        assertThrows(IllegalArgumentException.class, () -> TypeCodec.of(float[].class));
        assertThrows(IllegalArgumentException.class, () -> TypeCodec.of(double[].class));
        assertThrows(IllegalArgumentException.class, () -> TypeCodec.of(boolean[].class));
    }

    @Test
    void testPrimitiveList() {
        List<Integer> ints = List.of(42);
        DataResult<List<Integer>> intsResult = TypeCodec.of(int.class).listOf().parse(JavaPrimitiveOps.dynamic(ints));
        assertTrue(intsResult.result().isPresent(), "Result is " + intsResult.error());
        assertEquals(ints, intsResult.result().get());
        List<Long> longs = List.of(42L);
        DataResult<List<Long>> longsResult = TypeCodec.of(long.class).listOf().parse(JavaPrimitiveOps.dynamic(longs));
        assertTrue(longsResult.result().isPresent(), "Result is " + longsResult.error());
        assertEquals(longs, longsResult.result().get());
        List<Float> floats = List.of(42F);
        DataResult<List<Float>> floatsResult = TypeCodec.of(float.class).listOf().parse(JavaPrimitiveOps.dynamic(floats));
        assertTrue(floatsResult.result().isPresent(), "Result is " + floatsResult.error());
        assertEquals(floats, floatsResult.result().get());
    }

    public record SimpleRecord(int intValue, String stringValue) {}

    @Test
    void testRecordToJson() {
        var record = new SimpleRecord(42, "42");
        var result = TypeCodec.of(SimpleRecord.class).encodeStart(JsonOps.INSTANCE, record);
        assertTrue(result.result().isPresent(), "Result is " + result.error());
        assertEquals(result.result().get().toString(), """
            {"intValue":42,"stringValue":"42"}""");
    }

    @Test
    void testRecordFromJson() {
        var record = new SimpleRecord(42, "42");
        var result = TypeCodec.of(SimpleRecord.class).parse(JsonOps.INSTANCE, new JsonParser().parse("""
            {"intValue":42,"stringValue":"42"}"""));
        assertTrue(result.result().isPresent(), "Result is " + result.error());
        assertEquals(result.result().get(), record);
    }

    @Test
    void testGenericList() {
        var type = new TypeToken<List<SimpleRecord>>() {}.getType();
        var codec = TypeCodec.<List<SimpleRecord>>of(type);
        var records = List.of(new SimpleRecord(42, "42"));
        var result = codec.encodeStart(JsonOps.INSTANCE, records);
        assertTrue(result.result().isPresent(), "Result is " + result.error());
        assertEquals(result.result().get().toString(), """
            [{"intValue":42,"stringValue":"42"}]""");
        var parse = codec.parse(JsonOps.INSTANCE, result.result().get());
        assertTrue(parse.result().isPresent(), "Result is " + parse.error());
        assertEquals(parse.result().get(), records);
    }

    @Test
    void testRecordArray() {
        var codec = TypeCodec.of(SimpleRecord[].class);
        var records = new SimpleRecord[]{new SimpleRecord(42, "42")};
        var result = codec.encodeStart(JsonOps.INSTANCE, records);
        assertTrue(result.result().isPresent(), "Result is " + result.error());
        assertEquals(result.result().get().toString(), """
            [{"intValue":42,"stringValue":"42"}]""");
    }

    @Test
    void testGenericMap() {
        var type = new TypeToken<Map<String, SimpleRecord>>() {}.getType();
        var codec = TypeCodec.<Map<String, SimpleRecord>>of(type);
        var records = Map.of("record", new SimpleRecord(42, "42"));
        var result = codec.encodeStart(JsonOps.INSTANCE, records);
        assertTrue(result.result().isPresent(), "Result is " + result.error());
        assertEquals(result.result().get().toString(), """
            {"record":{"intValue":42,"stringValue":"42"}}""");
        var parse = codec.parse(JsonOps.INSTANCE, result.result().get());
        assertTrue(parse.result().isPresent(), "Result is " + parse.error());
        assertEquals(parse.result().get(), records);
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class SimpleImmutablePojo {

        private final int intValue;
        private final String stringValue;

        public SimpleImmutablePojo(int intValue, String stringValue) {
            this.intValue = intValue;
            this.stringValue = stringValue;
        }

        public int getIntValue() {
            return intValue;
        }

        public String getStringValue() {
            return stringValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SimpleImmutablePojo that = (SimpleImmutablePojo) o;

            if (intValue != that.intValue) return false;
            return Objects.equals(stringValue, that.stringValue);
        }

        @Override
        public int hashCode() {
            int result = intValue;
            result = 31 * result + (stringValue != null ? stringValue.hashCode() : 0);
            return result;
        }
    }

    @Test
    void testImmutablePojo() {
        var pojo = new SimpleImmutablePojo(42, "42");
        var result = TypeCodec.of(SimpleImmutablePojo.class).encodeStart(JsonOps.INSTANCE, pojo);
        assertTrue(result.result().isPresent(), "Result is " + result.error());
        assertEquals(result.result().get().toString(), """
            {"intValue":42,"stringValue":"42"}""");
        var parse = TypeCodec.of(SimpleImmutablePojo.class).parse(JsonOps.INSTANCE, result.result().get());
        assertTrue(parse.result().isPresent(), "Result is " + parse.error());
        assertEquals(pojo, parse.result().get());
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class SimpleAccessor {

        private final int intValue;
        private final String stringValue;

        public SimpleAccessor(int intValue, String stringValue) {
            this.intValue = intValue;
            this.stringValue = stringValue;
        }

        public int intValue() {
            return intValue;
        }

        public String stringValue() {
            return stringValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SimpleAccessor that = (SimpleAccessor) o;

            if (intValue != that.intValue) return false;
            return Objects.equals(stringValue, that.stringValue);
        }

        @Override
        public int hashCode() {
            int result = intValue;
            result = 31 * result + (stringValue != null ? stringValue.hashCode() : 0);
            return result;
        }
    }

    @Test
    void testAccessor() {
        var pojo = new SimpleAccessor(42, "42");
        var result = TypeCodec.of(SimpleAccessor.class).encodeStart(JsonOps.INSTANCE, pojo);
        assertTrue(result.result().isPresent(), "Result is " + result.error());
        assertEquals(result.result().get().toString(), """
            {"intValue":42,"stringValue":"42"}""");
        var parse = TypeCodec.of(SimpleAccessor.class).parse(JsonOps.INSTANCE, result.result().get());
        assertTrue(parse.result().isPresent(), "Result is " + parse.error());
        assertEquals(pojo, parse.result().get());
    }

    public record GenericComponent(List<String> strings) {}

    @Test
    void testGenericComponent() {
        var component = new GenericComponent(List.of("a", "b", "c"));
        var result = TypeCodec.of(GenericComponent.class).encodeStart(JsonOps.INSTANCE, component);
        assertTrue(result.result().isPresent(), "Result is " + result.error());
        assertEquals(result.result().get().toString(), """
            {"strings":["a","b","c"]}""");
        var parse = TypeCodec.of(GenericComponent.class).parse(JsonOps.INSTANCE, result.result().get());
        assertTrue(parse.result().isPresent(), "Result is " + parse.error());
        assertEquals(component, parse.result().get());
    }
}
