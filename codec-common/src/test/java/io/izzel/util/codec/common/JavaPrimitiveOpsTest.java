package io.izzel.util.codec.common;

import com.mojang.serialization.Codec;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaPrimitiveOpsTest {

    @Test
    void testPrimitives() {
        var intResult = Codec.INT.parse(JavaPrimitiveOps.dynamic(42));
        assertTrue(intResult.result().isPresent());
        assertEquals(42, intResult.result().get());
        var longResult = Codec.LONG.parse(JavaPrimitiveOps.dynamic(42L));
        assertTrue(longResult.result().isPresent());
        assertEquals(42L, longResult.result().get());
        var floatResult = Codec.FLOAT.parse(JavaPrimitiveOps.dynamic(42F));
        assertTrue(floatResult.result().isPresent());
        assertEquals(42F, floatResult.result().get());
        var doubleResult = Codec.DOUBLE.parse(JavaPrimitiveOps.dynamic(42D));
        assertTrue(doubleResult.result().isPresent());
        assertEquals(42D, doubleResult.result().get());
        var byteResult = Codec.BYTE.parse(JavaPrimitiveOps.dynamic((byte) 42));
        assertTrue(byteResult.result().isPresent());
        assertEquals((byte) 42, byteResult.result().get());
        var shortResult = Codec.SHORT.parse(JavaPrimitiveOps.dynamic((short) 42));
        assertTrue(shortResult.result().isPresent());
        assertEquals((short) 42, shortResult.result().get());
        var booleanResult = Codec.BOOL.parse(JavaPrimitiveOps.dynamic(true));
        assertTrue(booleanResult.result().isPresent());
        assertEquals(true, booleanResult.result().get());
        var stringResult = Codec.STRING.parse(JavaPrimitiveOps.dynamic("42"));
        assertTrue(stringResult.result().isPresent());
        assertEquals("42", stringResult.result().get());
    }

    @Test
    void testDfuPrimitiveArray() {
        var ints = new int[]{42};
        var result = Codec.INT_STREAM.encodeStart(JavaPrimitiveOps.INSTANCE, IntStream.of(ints));
        assertTrue(result.result().isPresent());
        assertArrayEquals(ints, (int[]) result.result().get());
    }

    @Test
    void testNonDfuPrimitiveArray() {
        var shorts = new short[]{42};
        var result = Codec.SHORT.listOf().parse(JavaPrimitiveOps.INSTANCE, shorts);
        assertTrue(result.error().isPresent());
    }

    @Test
    void testPrimitiveList() {
        var shorts = List.of((short) 42);
        var shortResult = Codec.SHORT.listOf().encodeStart(JavaPrimitiveOps.INSTANCE, shorts);
        assertTrue(shortResult.result().isPresent());
        assertEquals(shorts, shortResult.result().get());
        var ints = List.of(42);
        var intResult = Codec.INT.listOf().encodeStart(JavaPrimitiveOps.INSTANCE, ints);
        assertTrue(intResult.result().isPresent());
        assertEquals(ints, intResult.result().get());
    }

    @Test
    void testComplexConvert() {
        var basicDataTypes = Map.ofEntries(
            Map.entry("int", 42),
            Map.entry("long", 42L),
            Map.entry("float", 42F),
            Map.entry("double", 42D),
            Map.entry("byte", (byte) 42),
            Map.entry("short", (short) 42),
            Map.entry("boolean", true),
            Map.entry("string", "42")
        );
        var listMap = Collections.nCopies(42, basicDataTypes);
        var mapMap = Map.of("map", basicDataTypes);
        var map = Map.ofEntries(
            Map.entry("list", listMap),
            Map.entry("map", mapMap),
            Map.entry("basic", basicDataTypes)
        );
        var convert = JavaPrimitiveOps.INSTANCE.convertTo(JavaPrimitiveOps.INSTANCE, map);
        assertEquals(map, convert);
    }
}
