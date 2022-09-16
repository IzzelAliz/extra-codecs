package io.izzel.util.codec.common;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class JavaPrimitiveOps implements DynamicOps<Object> {

    public static final JavaPrimitiveOps INSTANCE = new JavaPrimitiveOps();

    @SuppressWarnings("unchecked")
    public static <A> Dynamic<A> dynamic(A value) {
        return new Dynamic<>((DynamicOps<A>) INSTANCE, value);
    }

    protected JavaPrimitiveOps() {
    }

    @Override
    public Object empty() {
        return null;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, Object input) {
        if (input instanceof Map) {
            return convertMap(outOps, input);
        } else if (input instanceof Collection) {
            return convertList(outOps, input);
        } else if (input instanceof String) {
            return outOps.createString((String) input);
        } else if (input instanceof Boolean) {
            return outOps.createBoolean((Boolean) input);
        } else if (input instanceof Integer) {
            return outOps.createInt((Integer) input);
        } else if (input instanceof Short) {
            return outOps.createShort((Short) input);
        } else if (input instanceof Byte) {
            return outOps.createByte((Byte) input);
        } else if (input instanceof Long) {
            return outOps.createLong((Long) input);
        } else if (input instanceof Float) {
            return outOps.createFloat((Float) input);
        } else if (input instanceof Double) {
            return outOps.createDouble((Double) input);
        } else if (input instanceof int[]) {
            return outOps.createIntList(IntStream.of((int[]) input));
        } else if (input instanceof long[]) {
            return outOps.createLongList(LongStream.of((long[]) input));
        } else if (input instanceof byte[]) {
            return outOps.createByteList(ByteBuffer.wrap((byte[]) input));
        }
        throw new IllegalArgumentException("Cannot convert " + input + " to " + outOps);
    }

    @Override
    public Object emptyList() {
        return Collections.emptyList();
    }

    @Override
    public Object emptyMap() {
        return Collections.emptyMap();
    }

    @Override
    public DataResult<Boolean> getBooleanValue(Object input) {
        return input instanceof Boolean ? DataResult.success((Boolean) input) : DataResult.error("Not a boolean: " + input);
    }

    @Override
    public Object createBoolean(boolean value) {
        return value;
    }

    @Override
    public DataResult<Number> getNumberValue(Object input) {
        return input instanceof Number ? DataResult.success((Number) input) : DataResult.error("Not a number: " + input);
    }

    @Override
    public Object createNumeric(Number i) {
        return i;
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(Object input) {
        return input instanceof byte[] ? DataResult.success(ByteBuffer.wrap((byte[]) input)) : DataResult.error("Not a byte array: " + input);
    }

    @Override
    public Object createByteList(ByteBuffer input) {
        byte[] arr = new byte[input.remaining()];
        input.get(arr);
        return arr;
    }

    @Override
    public Object createIntList(IntStream input) {
        return input.toArray();
    }

    @Override
    public Object createLongList(LongStream input) {
        return input.toArray();
    }

    @Override
    public DataResult<String> getStringValue(Object input) {
        return input instanceof String ? DataResult.success((String) input) : DataResult.error("Not a string: " + input);
    }

    @Override
    public Object createString(String value) {
        return value;
    }

    @Override
    public DataResult<Object> mergeToList(Object list, Object value) {
        if (list != null && !(list instanceof Collection)) {
            return DataResult.error("Not a list: " + list);
        }
        ArrayList<Object> result = new ArrayList<>();
        if (list != null) {
            result.addAll((Collection<?>) list);
        }
        result.add(value);
        return DataResult.success(result);
    }

    @Override
    public DataResult<Object> mergeToList(Object list, List<Object> values) {
        if (list != null && !(list instanceof Collection)) {
            return DataResult.error("Not a list: " + list);
        }
        ArrayList<Object> result = new ArrayList<>();
        if (list != null) {
            result.addAll((Collection<?>) list);
        }
        result.addAll(values);
        return DataResult.success(result);
    }

    @Override
    public Object createMap(Map<Object, Object> map) {
        return map;
    }

    @Override
    public DataResult<Object> mergeToMap(Object map, Object key, Object value) {
        if (map != null && !(map instanceof Map)) {
            return DataResult.error("Not a map: " + map);
        }
        if (key == null) {
            return DataResult.error("Key is null");
        }
        LinkedHashMap<Object, Object> result = new LinkedHashMap<>();
        if (map != null) {
            result.putAll((Map<?, ?>) map);
        }
        result.put(key, value);
        return DataResult.success(result);
    }

    @Override
    public DataResult<Object> mergeToMap(Object map, MapLike<Object> values) {
        if (map != null && !(map instanceof Map)) {
            return DataResult.error("Not a map: " + map);
        }
        LinkedHashMap<Object, Object> result = new LinkedHashMap<>();
        if (map != null) {
            result.putAll((Map<?, ?>) map);
        }
        Iterator<Pair<Object, Object>> iterator = values.entries().iterator();
        while (iterator.hasNext()) {
            Pair<Object, Object> pair = iterator.next();
            result.put(pair.getFirst(), pair.getSecond());
        }
        return DataResult.success(result);
    }

    @Override
    public DataResult<Stream<Pair<Object, Object>>> getMapValues(Object input) {
        if (!(input instanceof Map)) {
            return DataResult.error("Not a map: " + input);
        }
        return DataResult.success(((Map<?, ?>) input).entrySet().stream().map(it -> Pair.of(it.getKey(), it.getValue())));
    }

    @Override
    public DataResult<Consumer<BiConsumer<Object, Object>>> getMapEntries(Object input) {
        if (!(input instanceof Map)) {
            return DataResult.error("Not a map: " + input);
        }
        return DataResult.success(c -> ((Map<?, ?>) input).forEach(c));
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<MapLike<Object>> getMap(Object input) {
        if (!(input instanceof Map)) {
            return DataResult.error("Not a map: " + input);
        }
        return DataResult.success(MapLike.forMap((Map<Object, Object>) input, this));
    }

    @Override
    public Object createMap(Stream<Pair<Object, Object>> map) {
        return map.collect(Collectors.toMap(Pair::getFirst, Pair::getSecond,
            (u, v) -> {throw new IllegalStateException("Duplicate key " + u);},
            LinkedHashMap::new));
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Stream<Object>> getStream(Object input) {
        if (input instanceof Collection) {
            return DataResult.success(((Collection<Object>) input).stream());
        }
        return DataResult.error("Not a list: " + input);
    }

    @Override
    public DataResult<Consumer<Consumer<Object>>> getList(Object input) {
        if (input instanceof Collection) {
            return DataResult.success(c -> ((Collection<?>) input).forEach(c));
        }
        return DataResult.error("Not a list: " + input);
    }

    @Override
    public Object createList(Stream<Object> input) {
        return input.collect(Collectors.toList());
    }

    @Override
    public Object remove(Object input, String key) {
        if (input instanceof Map) {
            ((Map<?, ?>) input).remove(key);
        }
        return input;
    }

    @Override
    public DataResult<IntStream> getIntStream(Object input) {
        if (input instanceof int[]) {
            return DataResult.success(IntStream.of((int[]) input));
        }
        return DataResult.error("Not a int array: " + input);
    }

    @Override
    public DataResult<LongStream> getLongStream(Object input) {
        if (input instanceof long[]) {
            return DataResult.success(LongStream.of((long[]) input));
        }
        return DataResult.error("Not a long array: " + input);
    }

    @Override
    public String toString() {
        return "Java Primitive";
    }
}
