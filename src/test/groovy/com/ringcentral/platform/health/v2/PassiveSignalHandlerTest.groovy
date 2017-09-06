package com.ringcentral.platform.health.v2

import com.ringcentral.platform.health.HealthCheckResult
import com.ringcentral.platform.health.demo.TestFunction
import rx.observers.TestSubscriber
import spock.lang.Specification

import static com.ringcentral.platform.health.HealthStateEnum.*

class PassiveSignalHandlerTest extends Specification {

    def F1 = new TestFunction("1", 100, 100, {})
    def F2 = new TestFunction("2", 200, 200, {})

    def 'PassiveSignalHandler should process all passive events'() {
        setup:
        def observable = PassiveSignalHandler.createPassiveSubject([F1, F2], false)
        def subscriber = new TestSubscriber<>()

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.assertNoValues()
        subscriber.assertNotCompleted()

        when:
        observable.onNext(new HealthCheckResult(F1.getId(), OK, ""))

        then:
        subscriber.assertValueCount(1)

        when:
        observable.onNext(new HealthCheckResult(F1.getId(), OK, ""))
        observable.onNext(new HealthCheckResult(F2.getId(), OK, ""))
        observable.onNext(new HealthCheckResult(F2.getId(), Critical, ""))

        then:
        subscriber.assertValueCount(4)
    }

    def 'PassiveSignalHandler should filter non-changing events'() {
        setup:
        def observable = PassiveSignalHandler.createPassiveSubject([F1, F2], true)
        def subscriber = new TestSubscriber<>()

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.assertNoValues()
        subscriber.assertNotCompleted()

        when:
        observable.onNext(new HealthCheckResult(F1.getId(), OK, ""))

        then:
        subscriber.assertValueCount(1)

        when:
        observable.onNext(new HealthCheckResult(F1.getId(), OK, ""))
        observable.onNext(new HealthCheckResult(F2.getId(), Critical, ""))
        observable.onNext(new HealthCheckResult(F2.getId(), Critical, ""))
        observable.onNext(new HealthCheckResult(F2.getId(), Critical, ""))
        observable.onNext(new HealthCheckResult(F2.getId(), Critical, ""))
        observable.onNext(new HealthCheckResult(F2.getId(), OK, ""))
        observable.onNext(new HealthCheckResult(F1.getId(), OK, ""))
        observable.onNext(new HealthCheckResult(F1.getId(), OK, ""))
        observable.onNext(new HealthCheckResult(F1.getId(), Warning, ""))

        then:
        subscriber.assertValueCount(4)
        subscriber.getOnNextEvents().clear()

        when:
        observable.onNext(new HealthCheckResult(F1.getId(), OK, ""))
        observable.onNext(new HealthCheckResult(F1.getId(), Critical, ""))
        observable.onNext(new HealthCheckResult(F1.getId(), OK, ""))
        observable.onNext(new HealthCheckResult(F1.getId(), Critical, ""))
        observable.onNext(new HealthCheckResult(F1.getId(), OK, ""))
        observable.onNext(new HealthCheckResult(F1.getId(), Critical, ""))

        then:
        subscriber.assertValueCount(6)
    }
}
