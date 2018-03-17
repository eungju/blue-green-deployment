package org.xnio.nio;

import org.xnio.Xnio;
import org.xnio.XnioProvider;

public class ReuseNioXnioProvider implements XnioProvider {
    private static final Xnio INSTANCE = new ReuseNioXnio();

    /** {@inheritDoc} */
    public Xnio getInstance() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    public String getName() {
        return INSTANCE.getName();
    }
}

