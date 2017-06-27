package com.baojie.liuxinreconnect.util;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.PooledByteBufAllocator;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public final class SerializationUtil {

    private static final ConcurrentHashMap<Class<?>, Schema<?>> CachedSchema = new ConcurrentHashMap<Class<?>,
            Schema<?>>();
    private static final Logger log = LoggerFactory.getLogger(SerializationUtil.class);
    private static final Objenesis objenesis = new ObjenesisStd(true);

    static {
        System.getProperties().setProperty("protostuff.runtime.always_use_sun_reflection_factory", "true");
    }

    private SerializationUtil() {
        throw new IllegalArgumentException();
    }

    public static final <T> byte[] serialize(final T obj) {
        CheckNull.checkNull(obj, "'T obj' must not be null");
        final byte[] realBytes = realBytes(obj);
        return realBytes;
    }

    private static final <T> byte[] realBytes(final T obj) {
        final Class<T> cls = (Class<T>) obj.getClass();
        final Schema<T> schema = getSchema(cls);
        byte[] useBytes = new byte[LinkedBuffer.DEFAULT_BUFFER_SIZE];
        LinkedBuffer buffer = LinkedBuffer.use(useBytes);
        byte[] bytes;
        try {
            bytes = work(obj, schema, buffer);
        } finally {
            release(buffer, useBytes);
        }
        return bytes;
    }

    private static final <T> Schema<T> getSchema(final Class<T> cls) {
        CheckNull.checkNull(cls, "'Class<T> cls' must not be null");
        Schema<T> schema = (Schema<T>) CachedSchema.get(cls);
        if (null == schema) {
            setSystemProperties();
            schema = RuntimeSchema.getSchema(cls);
            if (null != schema) {
                CachedSchema.putIfAbsent(cls, schema);
            }
        }
        return schema;
    }

    private static final void setSystemProperties() {

    }

    private static final <T> byte[] work(final T obj, final Schema<T> schema, final LinkedBuffer buffer) {
        byte[] bytes = null;
        try {
            bytes = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } finally {
            if ((null == bytes) || (bytes.length == 0)) {
                bytes = new byte[0];
            }
        }
        return bytes;
    }

    private static final void release(LinkedBuffer buffer, byte[] useBytes) {
        if (null != buffer) {
            buffer.clear();
            buffer = null;
        }
        if (null != useBytes) {
            useBytes = null;
        }
    }

    public static final <T> T deserialize(final byte[] bytes, final Class<T> cls) {
        innerCheck(cls, bytes);
        final T message = getInstanceInner(cls);
        final Schema<T> schema = getSchema(cls);
        try {
            ProtostuffIOUtil.mergeFrom(bytes, 0, bytes.length, message, schema);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return message;
    }

    private static final <T> void innerCheck(final Class<T> cls, final byte[] bytes) {
        CheckNull.checkNull(cls, "'Class<T> cls' must not be null");
        CheckNull.checkByteArrayNull(bytes, "'byte[] bytes' must not be null");
        CheckNull.checkByteArrayZero(bytes, "'byte[] bytes.length' must not be zero");
    }

    private static final <T> T getInstanceInner(final Class<T> cls) {
        final T t = objenesis.newInstance(cls);
        if (null != t) {
            return t;
        } else {
            return localGet(cls);
        }
    }

    private static final <T> T localGet(final Class<T> cls) {
        T t = null;
        try {
            t = cls.newInstance();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            CheckNull.checkNull(null, "get Object by cls.newInstance() must not null");
        }
        return t;
    }

    public static void main(String args[]) {
        final byte[] bytes = new byte[10];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = 1;
        }

        for (int i = 0; i < bytes.length; i++) {
            System.out.println(bytes[i]);
        }
    }

}
