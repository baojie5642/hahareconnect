package com.baojie.liuxinreconnect.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HaThreadGroup {

    private static final Logger log = LoggerFactory.getLogger(HaThreadGroup.class);

    private HaThreadGroup() {

    }

    public static final ThreadGroup innerThreadGroup(final SecurityManager sm) {
        ThreadGroup threadGroup = null;
        if (null != sm) {
            threadGroup = sm.getThreadGroup();
        } else {
            threadGroup = Thread.currentThread().getThreadGroup();
        }
        if (null == threadGroup) {
            log.error("ThreadGroup must not be null.");
            throw new NullPointerException("ThreadGroup must not be null.");
        }
        return threadGroup;
    }

}
