package net.gcdc.ittgt.client;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

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

    private static boolean isTestInstrumentation(Field f) {
        return f.getName().startsWith("$");
    }
}
