package com.ringcentral.platform.rx;

import rx.Subscriber;
import rx.observers.SafeSubscriber;

/**
 * Subscriber wrapper that prevents calling unsubscribe method in case of error
 * @param <T> type of event
 */
public class RealSafeSubscriber<T> extends SafeSubscriber<T> {

    private final Subscriber<? super T> actual;

    /**
     * Constructor
     *
     * @param actual actual subscriber to wrap
     */
    public RealSafeSubscriber(Subscriber<T> actual) {
        super(actual);
        this.actual = super.getActual();
    }

    @Override
    public void onNext(T args) {
        try {
            actual.onNext(args);
        } catch (Throwable e) {
            actual.onError(e);
        }
    }
}
