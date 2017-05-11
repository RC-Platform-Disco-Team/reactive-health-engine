package com.ringcentral.platform.health

import com.ringcentral.platform.rx.MapSubject
import rx.Observable
import rx.observers.TestSubscriber
import spock.lang.Specification

class MapSubjectTest extends Specification {

    def 'MapSubject signals should be filtered and converted'() {
        def mapFunction = { Observable<String> it ->
            it.flatMap({ str ->
                Observable.just(Integer.parseInt(str))
            }).filter({ i -> i % 2 == 0 })
        }
        setup:

        def observable = MapSubject.<String, Integer> create(mapFunction)
        def subscriber = new TestSubscriber<>()

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.assertNoValues()
        subscriber.assertNotCompleted()

        when:
        observable.onNext("1")
        observable.onNext("2")
        observable.onNext("3")
        observable.onNext("4")
        observable.onCompleted()

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertValueCount(2)
        subscriber.assertValues(2, 4)
    }

    def 'MapSubject should handle errors'() {
        def mapFunction = { Observable<String> it ->
            it.flatMap({ str ->
                Observable.just(Integer.parseInt(str))
            }).filter({ i -> i % 2 == 0 })
        }
        setup:

        def observable = MapSubject.<String, Integer> create(mapFunction)
        def subscriber = new TestSubscriber<>()

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.assertNoValues()
        subscriber.assertNotCompleted()

        when:
        observable.onNext("1")
        observable.onNext("3")
        observable.onNext("4")
        observable.onError(new RuntimeException())
        observable.onNext("5")
        observable.onNext("6")

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertValueCount(1)
        subscriber.assertValues(4)
    }
}
