package io.mosip.packet.core.util;

import java.util.LinkedList;

public class FixedListQueue<E> extends LinkedList<E> {
    private int SizeLimitOfQueue;

    public FixedListQueue(int SizeLimitOfQueue) {
        this.SizeLimitOfQueue = SizeLimitOfQueue;
    }

    @Override
    public boolean add(E e) {
        while (this.size() == SizeLimitOfQueue) {
            super.remove();
        }
        super.add(e);
        return true;
    }
}
