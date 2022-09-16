package io.izzel.util.codec.common;

import com.mojang.serialization.Codec;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class TypeCodec {

    private static final Map<Class<?>, Codec<?>> PRIMITIVES = new HashMap<>();

    static {
        PRIMITIVES.put(boolean.class, Codec.BOOL);
        PRIMITIVES.put(byte.class, Codec.BYTE);
        PRIMITIVES.put(short.class, Codec.SHORT);
        PRIMITIVES.put(int.class, Codec.INT);
        PRIMITIVES.put(long.class, Codec.LONG);
        PRIMITIVES.put(float.class, Codec.FLOAT);
        PRIMITIVES.put(double.class, Codec.DOUBLE);
        PRIMITIVES.put(String.class, Codec.STRING);
        PRIMITIVES.put(int[].class, Codec.INT_STREAM.xmap(IntStream::toArray, IntStream::of));
        PRIMITIVES.put(long[].class, Codec.LONG_STREAM.xmap(LongStream::toArray, LongStream::of));
        PRIMITIVES.put(byte[].class, Codec.BYTE_BUFFER.xmap(it -> {
            byte[] arr = new byte[it.remaining()];
            it.get(arr);
            return arr;
        }, ByteBuffer::wrap));
    }

    @SuppressWarnings("unchecked")
    public static <A> Codec<A> of(Class<A> type) {
        Codec<?> codec = PRIMITIVES.get(type);
        if (codec != null) {
            return (Codec<A>) codec;
        } else {
            if (type.isArray()) {
                Class<Object> componentType = (Class<Object>) type.getComponentType();
                if (Object.class.isAssignableFrom(componentType)) {
                    return (Codec<A>) of(componentType).listOf().xmap(
                        it -> it.toArray((Object[]) Array.newInstance(componentType, 0)),
                        Arrays::asList);
                } else {
                    throw new IllegalArgumentException("Cannot create codec for " + type);
                }
            } else {
                return new ClassMapCodec<>(type).codec();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <A> Codec<A> of(Type type) {
        if (type instanceof Class) {
            return of((Class<A>) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) pType.getRawType();
            if ((rawType == Collection.class || rawType == List.class) && pType.getActualTypeArguments().length == 1) {
                return (Codec<A>) of(pType.getActualTypeArguments()[0]).listOf();
            } else if (rawType == Map.class && pType.getActualTypeArguments().length == 2) {
                return (Codec<A>) Codec.unboundedMap(of(pType.getActualTypeArguments()[0]), of(pType.getActualTypeArguments()[1]));
            }
        }
        throw new RuntimeException("Cannot find codec for " + type);
    }
}
