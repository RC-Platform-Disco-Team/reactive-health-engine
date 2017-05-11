package com.ringcentral.platform.rx;

import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * Simple wrapper for Subject that converts data during processing
 */
public class MapSubject {

    /**
     * Create {@link Subject} with map function
     *
     * @param mapFunction function to convert initial {@link Observable} into resulting {@link Observable}
     * @param <T> type of initial event
     * @param <R> type of resulting event
     * @return wrapped {@link Subject}
     */
    public static <T, R> Subject<T, R> create(Func1<Observable<T>, Observable<R>> mapFunction) {
        final PublishSubject<T> subject = PublishSubject.create();
        Observable<R> observable = mapFunction.call(subject);
        return new WrappedSubject<>(observable::subscribe, subject);
    }

    public static final class WrappedSubject<T, R> extends Subject<T, R> {

        private Subject<T, T> subject;

        private WrappedSubject(OnSubscribe<R> onSubscribe, Subject<T, T> input) {
            super(onSubscribe);
            this.subject = input;
        }

        @Override
        public void onCompleted() {
            subject.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            subject.onError(e);
        }

        @Override
        public void onNext(T newValue) {
            subject.onNext(newValue);
        }

        @Override
        public boolean hasObservers() {
            return subject.hasObservers();
        }
    }
}
