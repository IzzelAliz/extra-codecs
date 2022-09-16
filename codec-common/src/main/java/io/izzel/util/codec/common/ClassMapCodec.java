package io.izzel.util.codec.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

class ClassMapCodec<A> extends MapCodec<A> {

    private final Class<A> type;
    private final TypeAccessor<A> accessor;

    ClassMapCodec(Class<A> type) {
        this.type = type;
        this.accessor = new Reflection<>(type);
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return accessor.componentKeys().stream().map(ops::createString);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
        List<String> keys = accessor.componentKeys();
        List<Codec<?>> codecs = accessor.componentCodecs();
        Object[] args = new Object[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            DataResult<?> result = codecs.get(i).parse(ops, input.get(key));
            if (!result.result().isPresent()) { // indicate error
                return (DataResult<A>) result;
            }
            args[i] = result.result().get();
        }
        return accessor.newInstance(args);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        List<String> keys = accessor.componentKeys();
        List<Codec<?>> codecs = accessor.componentCodecs();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Codec<Object> codec = (Codec<Object>) codecs.get(i);
            prefix = prefix.add(key, accessor.component(input, key).flatMap(it -> codec.encodeStart(ops, it)));
        }
        return prefix;
    }

    @Override
    public String toString() {
        return "ClassMapCodec[" + type + "]";
    }

    private interface TypeAccessor<A> {

        List<String> componentKeys();

        List<Codec<?>> componentCodecs();

        <R> DataResult<R> component(A instance, String key);

        DataResult<A> newInstance(Object... args);
    }

    private static class Reflection<A> implements TypeAccessor<A> {

        private final List<String> fields = new ArrayList<>();
        private final List<Codec<?>> codecs = new ArrayList<>();
        private final Map<String, ComponentAccessor<A>> accessors = new HashMap<>();
        private final Constructor<A> constructor;

        private Reflection(Class<A> type) {
            List<Class<?>> argTypes = new ArrayList<>();
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                fields.add(field.getName());
                codecs.add(TypeCodec.of(field.getGenericType()));
                accessors.put(field.getName(), ComponentAccessor.forField(type, field));
                argTypes.add(field.getType());
            }
            try {
                Constructor<A> candidate = type.getConstructor(argTypes.toArray(new Class<?>[0]));
                if (Modifier.isPublic(candidate.getModifiers())) {
                    this.constructor = candidate;
                } else {
                    throw new RuntimeException("Constructor " + candidate + " is not accessible");
                }
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

        }

        @Override
        public List<String> componentKeys() {
            return fields;
        }

        @Override
        public List<Codec<?>> componentCodecs() {
            return codecs;
        }

        @Override
        public <R> DataResult<R> component(A instance, String key) {
            return accessors.get(key).component(instance);
        }

        @Override
        public DataResult<A> newInstance(Object... args) {
            try {
                return DataResult.success(constructor.newInstance(args));
            } catch (Exception e) {
                return DataResult.error("Failed to initialize " + constructor.getDeclaringClass() + ": " + e.getMessage());
            }
        }
    }

    private interface ComponentAccessor<A> {

        <R> DataResult<R> component(A instance);

        static <A> ComponentAccessor<A> forField(Class<A> type, Field field) {
            Optional<Method> optional = findAccessible(type, field.getType(), field.getName(), "get" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1));
            if (optional.isPresent()) {
                return new MethodAccessor<>(optional.get());
            } else {
                if (Modifier.isPublic(field.getModifiers())) {
                    return new FieldAccessor<>(field);
                } else {
                    throw new RuntimeException("Unable to find accessor for component " + field.getName());
                }
            }
        }

        static Optional<Method> findAccessible(Class<?> type, Class<?> returnType, String... names) {
            for (String s : names) {
                try {
                    Method method = type.getMethod(s);
                    if (!Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                        && method.getParameterCount() == 0 && method.getReturnType().equals(returnType)) {
                        return Optional.of(method);
                    }
                } catch (Exception ignored) {
                }
            }
            return Optional.empty();
        }
    }

    private static class MethodAccessor<A> implements ComponentAccessor<A> {

        private final Method method;

        private MethodAccessor(Method method) {
            this.method = method;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> DataResult<R> component(A instance) {
            try {
                return DataResult.success((R) method.invoke(instance));
            } catch (Exception e) {
                return DataResult.error("Failed to invoke " + method + ": " + e.getMessage());
            }
        }
    }

    private static class FieldAccessor<A> implements ComponentAccessor<A> {

        private final Field field;

        private FieldAccessor(Field field) {
            this.field = field;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> DataResult<R> component(A instance) {
            try {
                return DataResult.success((R) field.get(instance));
            } catch (IllegalAccessException e) {
                return DataResult.error("Failed to get " + field + ": " + e.getMessage());
            }
        }
    }
}
