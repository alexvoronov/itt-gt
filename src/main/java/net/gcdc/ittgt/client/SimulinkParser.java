package net.gcdc.ittgt.client;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulinkParser {
    final static Logger logger = LoggerFactory.getLogger(SimulinkParser.class);

    public static <T> T parse(byte[] bytes, Class<T> classOfT) {
        try {
            return parse(ByteBuffer.wrap(bytes), classOfT);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Can't parse " + classOfT, e);
        }
    }

    public static <T> T parse(ByteBuffer buffer, Class<T> classOfT) throws InstantiationException,
            IllegalAccessException {
        T obj = classOfT.newInstance();
        for (Field f : classOfT.getDeclaredFields()) {
            if (isTestInstrumentation(f)) {
                continue;
            }
            if (f.getType().isAssignableFrom(long.class)) {
                f.set(obj, buffer.getLong());
            } else if (f.getType().isAssignableFrom(double.class)) {
                f.set(obj, buffer.getDouble());
            } else {
                throw new UnsupportedOperationException("Parsing of " + f.getType().getName() +
                        " is not supported yet, needed for field " + f.getName() + " of "
                        + classOfT.getName());
            }
        }
        return obj;
    }

    public static byte[] encode(SimulinkWorld obj) {
        ByteBuffer bb = ByteBuffer.allocate(65535);
        for (SimulinkGt v: obj.vehicles) {
            try {
                encode2(v, bb);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                logger.error("can't encode simulink world {}", obj, e);
                throw new IllegalArgumentException("can't encode world " + obj, e);
            }
        }
        bb.flip();
        byte[] result = Arrays.copyOfRange(bb.array(), bb.arrayOffset(), bb.arrayOffset() + bb.limit());
        return result;
    }

    private static ByteBuffer encode2(SimulinkGt obj, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException {
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (isTestInstrumentation(f)) {
                continue;
            }
            if (double.class.isAssignableFrom(f.getType())) {
                buffer.putDouble((double) f.get(obj));
            } else {
                throw new UnsupportedOperationException("Parsing of " + f.getType().getName() +
                        " is not supported yet, needed for field " + f.getName() + " of "
                        + obj.getClass().getName());
            }
        }
        return buffer;
    }

    private static boolean isTestInstrumentation(Field f) {
        return f.getName().startsWith("$");
    }
}
