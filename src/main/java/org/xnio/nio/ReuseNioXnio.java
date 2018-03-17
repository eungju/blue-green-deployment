package org.xnio.nio;

import org.xnio.OptionMap;
import org.xnio.XnioWorker;

import java.io.IOException;

public class ReuseNioXnio extends NioXnio {
    @Override
    public XnioWorker createWorker(final ThreadGroup threadGroup, final OptionMap optionMap, final Runnable terminationTask) throws IOException, IllegalArgumentException {
        final NioXnioWorker worker = new ReuseNioXnioWorker(this, threadGroup, optionMap, terminationTask);
        worker.start();
        return worker;
    }
}
