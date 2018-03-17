package org.xnio.nio;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.ServerSocketChannel;

public class ReusePort {
    private static final int SO_REUSEPORT;
    private static final int SOL_SOCKET;

    static {
        final String osName = System.getProperty("os.name").toLowerCase();
        if (isMac(osName)) {
            SOL_SOCKET = 0xffff;
            SO_REUSEPORT = 0x0200;
        } else if (isLinux(osName)) {
            SOL_SOCKET = 1;
            SO_REUSEPORT = 15;
        } else {
            SOL_SOCKET = 0xffff;
            SO_REUSEPORT = 0;
        }
    }

    private static boolean isMac(String osName) {
        return osName.startsWith("mac");
    }

    private static boolean isLinux(String osName) {
        return osName.startsWith("linux");
    }

    public static void enableReusePort(ServerSocketChannel channel) throws IOException {
        try {
            Field fieldFd = channel.getClass().getDeclaredField("fd");
            fieldFd.setAccessible(true);
            FileDescriptor fd = (FileDescriptor) fieldFd.get(channel);

            Class<?> netClass = Class.forName("sun.nio.ch.Net");
            Method methodSetIntOption0;
            try {
                methodSetIntOption0 = netClass.getDeclaredMethod(
                        "setIntOption0", FileDescriptor.class, Boolean.TYPE, Integer.TYPE, Integer.TYPE,
                        Integer.TYPE, Boolean.TYPE);
                methodSetIntOption0.setAccessible(true);
                methodSetIntOption0.invoke(null, fd, false, SOL_SOCKET, SO_REUSEPORT, 1, true);
            } catch (NoSuchMethodException e) {
                methodSetIntOption0 = netClass.getDeclaredMethod(
                        "setIntOption0", FileDescriptor.class, Boolean.TYPE, Integer.TYPE, Integer.TYPE,
                        Integer.TYPE);
                methodSetIntOption0.setAccessible(true);
                methodSetIntOption0.invoke(null, fd, false, SOL_SOCKET, SO_REUSEPORT, 1);
            }
        } catch (Exception e) {
            throw new IOException("Unable to set SO_REUSEPORT", e);
        }
    }
}
