package com.ringcentral.platform.health.v2

import com.ringcentral.platform.health.HealthCheckConfig
import com.ringcentral.platform.health.HealthCheckFunction
import com.ringcentral.platform.health.HealthCheckRequest
import com.ringcentral.platform.health.HealthCheckSignal
import rx.Observable
import rx.observers.TestSubscriber
import spock.lang.Specification

class HealthCheckSplitterTest extends Specification {

    def 'should produce no items for empty check list'() {
        setup:
        def splitter = new HealthCheckSplitter([], null)
        def observable = Observable.just(HealthCheckSignal.tick(Mock(HealthCheckConfig)))
                .flatMap({ splitter.convertTickToRequest(it) })
        def subscriber = new TestSubscriber<HealthCheckRequest>()

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertNoValues()
        subscriber.assertNoErrors()
    }

    def 'should produce no items for tick without config'() {
        setup:
        def splitter = new HealthCheckSplitter([Mock(HealthCheckFunction)], null)
        def observable = Observable.just(HealthCheckSignal.tick(null))
                .flatMap({ splitter.convertTickToRequest(it) })
        def subscriber = new TestSubscriber<HealthCheckRequest>()

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertNoValues()
        subscriber.assertNoErrors()
    }

    def 'should produce one item for one healthcheck'() {
        setup:
        def splitter = new HealthCheckSplitter([Mock(HealthCheckFunction)], null)
        def observable = Observable.just(HealthCheckSignal.tick(Mock(HealthCheckConfig)))
                .flatMap({ splitter.convertTickToRequest(it) })
        def subscriber = new TestSubscriber<HealthCheckRequest>()

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertValueCount(1)
        subscriber.assertNoErrors()
    }

    def 'should produce 4 items for 2 healthchecks and 2 ticks'() {
        setup:
        def splitter = new HealthCheckSplitter([Mock(HealthCheckFunction), Mock(HealthCheckFunction)], null)
        def observable = Observable.just(
                HealthCheckSignal.tick(Mock(HealthCheckConfig)),
                HealthCheckSignal.tick(Mock(HealthCheckConfig))
        )
                .flatMap({ splitter.convertTickToRequest(it) })
        def subscriber = new TestSubscriber<HealthCheckRequest>()

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertValueCount(4)
        subscriber.assertNoErrors()
    }
}
