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

public class SerializationUtil {

    private static final ConcurrentHashMap<Class<?>, Schema<?>> CachedSchema = new ConcurrentHashMap<Class<?>,
            Schema<?>>();
    private static final Logger log = LoggerFactory.getLogger(SerializationUtil.class);
    private static final Objenesis objenesis = new ObjenesisStd(true);
    private static final ConcurrentLinkedQueue<byte[]> Byte_Cache=new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<byte[]> Temp=new ConcurrentLinkedQueue<>();


    static {
        System.getProperties().setProperty("protostuff.runtime.always_use_sun_reflection_factory", "true");
        for(int i=0;i<4;i++){
            BytesClean bytesClean=BytesClean.create(Temp,Byte_Cache);
            Thread thread=new Thread(bytesClean,"bytes_clean");
            thread.start();
        }
    }

    private SerializationUtil() {
        throw new IllegalArgumentException();
    }

    public static <T> byte[] serialize(final T obj) {
        if (null == obj) {
            log.error("'T obj' must not be null.");
            throw new NullPointerException("'T obj' must not be null.");
        }
        final byte[] realBytes = realBytes(obj);
        return realBytes;
    }

    private static <T> byte[] realBytes(final T obj) {
        byte[] bytes = null;
        Class<T> cls = (Class<T>) obj.getClass();
        Schema<T> schema = getSchema(cls);
        byte[] bytesForBuffer = Byte_Cache.poll();
        if(null==bytesForBuffer){
            bytesForBuffer=new byte[LinkedBuffer.DEFAULT_BUFFER_SIZE];
        }
        LinkedBuffer buffer = LinkedBuffer.use(bytesForBuffer);
        try {
            bytes = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
            if ((null == bytes) || (bytes.length == 0)) {
                bytes = new byte[0];
            }
        } finally {
            releaseResource(buffer, bytesForBuffer);
        }
        return bytes;
    }

    private static <T> Schema<T> getSchema(final Class<T> cls) {
        if (null == cls) {
            log.error("'Class<T> cls' must not be null.");
            throw new NullPointerException("'Class<T> cls' must not be null.");
        }
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

    private static void setSystemProperties() {

    }

    private static <T> void releaseResource(LinkedBuffer buffer, byte bytesForBuffer[]) {
        if (null != buffer) {
            buffer.clear();
            buffer = null;
        }
        if (null != bytesForBuffer) {
            Temp.offer(bytesForBuffer);
        }
    }

    public static <T> T deserialize(final byte[] bytes, final Class<T> cls) {
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

    private static <T> T getInstanceInner(final Class<T> cls) {
        final T t = objenesis.newInstance(cls);
        if (null != t) {
            return t;
        } else {
            return localGet(cls);
        }
    }

    private static <T> T localGet(final Class<T> cls) {
        T t = null;
        try {
            t = cls.newInstance();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            t = null;
            throw new NullPointerException("get Object by cls.newInstance() must not null.");
        }
        return t;
    }

    private static <T> void innerCheck(final Class<T> cls, final byte[] bytes) {
        if (null == cls) {
            log.error("'Class<T> cls' must not be null.");
            throw new NullPointerException("'Class<T> cls' must not be null.");
        }
        if (null == bytes) {
            log.error("'byte[] bytes' must not be null.");
            throw new NullPointerException("'byte[] bytes' must not be null.");
        }
        if (bytes.length == 0) {
            log.error("'byte[] bytes.length' must not be zero.");
            throw new IllegalArgumentException("'byte[] bytes.length' must not be zero.");
        }
    }

    private static final class BytesClean implements Runnable{

        private final ConcurrentLinkedQueue<byte[]> temp;

        private final ConcurrentLinkedQueue<byte[]> cache;

        private final AtomicBoolean work=new AtomicBoolean(true);

        private BytesClean(final ConcurrentLinkedQueue<byte[]> temp,final ConcurrentLinkedQueue<byte[]> cache){
            this.temp=temp;
            this.cache=cache;
        }

        public static BytesClean create(final ConcurrentLinkedQueue<byte[]> temp,final ConcurrentLinkedQueue<byte[]> cache){
            return  new BytesClean(temp,cache);
        }

        @Override
        public void run(){
            final Thread thread=Thread.currentThread();
            int kk=0;
            byte[] bytes=null;
            for(;;){
                boolean stop=work.get();
                if(stop){
                    bytes=get();
                    if(null!=bytes){
                        doWork(bytes);
                    }else {
                        bytes=get();
                        if(null!=bytes){
                            doWork(bytes);
                        }else{
                            park();
                        }
                    }
                }else {
                    if(thread.isInterrupted()){
                        break;
                    }else{
                        if(kk==3){
                            break;
                        }else {
                            kk++;
                        }
                    }
                }
            }
        }

        private byte[] get(){
            return temp.poll();
        }

        public boolean put(final byte[] bytes){
            if(null!=bytes){
                return cache.offer(bytes);
            }else {
                return false;
            }
        }

        private void doWork(final byte[] bytes){
            if(null!=bytes){
                Arrays.fill(bytes,(byte)0);
                put(bytes);
            }
        }

        private void park(){
            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(2,TimeUnit.NANOSECONDS));
        }

        public void stop(){
            if(work.get()){
                work.compareAndSet(true,false);
            }else {
                return;
            }
        }


    }

    public static void main(String args[]) {
        final byte[] bytes = new byte[10];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i]=1;
        }

        for (int i = 0; i < bytes.length; i++) {
            System.out.println(bytes[i]);
        }
    }

}
