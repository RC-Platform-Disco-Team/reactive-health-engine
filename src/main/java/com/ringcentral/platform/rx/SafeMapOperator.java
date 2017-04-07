package com.ringcentral.platform.rx;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

public class SafeMapOperator<T, R> implements Operator<T, R> {

    private final boolean unsubscribeOnError;
    private final Action1<Throwable> onError;
    private final Func1<? super R, ? extends T> transformer;

    public SafeMapOperator(boolean unsubscribeOnError, Func1<? super R, ? extends T> transformer, Action1<Throwable> onError) {
        this.transformer = transformer;
        this.onError = onError;
        this.unsubscribeOnError = unsubscribeOnError;
    }

    @Override
    public Subscriber<? super R> call(Subscriber<? super T> t1) {

        return new Subscriber<R>(t1) {

            @Override
            public void onNext(R t) {
                T result;
                try {
                    result = transformer.call(t);
                    t1.onNext(result);
                } catch (Throwable e) {
                    if (unsubscribeOnError) {
                        unsubscribe();
                    }
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                onError.call(e);
            }

            @Override
            public void onCompleted() {
                t1.onCompleted();
            }

        };
    }
}