package com.ringcentral.platform.health.v2

import com.ringcentral.platform.health.HealthCheckConfig
import com.ringcentral.platform.health.HealthCheckSignal
import com.ringcentral.platform.health.HealthEngineConfig
import org.hamcrest.CustomTypeSafeMatcher
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import spock.lang.Specification

import java.time.Duration
import java.util.function.Supplier

import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.hamcrest.CoreMatchers.everyItem
import static org.junit.Assert.assertThat

class TickSignalObservableTest extends Specification {

    def engineCfg = Mock(HealthEngineConfig)
    def healthCfg = Mock(Supplier) as Supplier<HealthCheckConfig>

    @SuppressWarnings("GroovyAssignabilityCheck")
    'test subscriber with test scheduler'() {
        setup:

        1 * engineCfg.tickSignalPeriod >> Duration.ofSeconds(1)
        def v1 = Mock(HealthCheckConfig)
        def v2 = Mock(HealthCheckConfig)
        3 * healthCfg.get() >> v1
        3 * healthCfg.get() >> v2

        def testScheduler = new TestScheduler()
        def observable = TickSignalObservable.createObservable(engineCfg, healthCfg, testScheduler)
        def subscriber = new TestSubscriber<>()

        when:
        observable.subscribeOn(testScheduler).subscribe(subscriber)

        then:
        subscriber.assertNoValues()
        subscriber.assertNotCompleted()

        when:
        testScheduler.advanceTimeBy(3500, MILLISECONDS)

        then:
        subscriber.assertValueCount(3)
        assertThat(subscriber.getOnNextEvents(), everyItem(hasConfig(v1)))
        subscriber.getOnNextEvents().clear()

        when:
        testScheduler.advanceTimeBy(3000, MILLISECONDS)

        then:
        subscriber.assertValueCount(3)
        assertThat(subscriber.getOnNextEvents(), everyItem(hasConfig(v2)))

    }

    def hasConfig(expected) {
        new CustomTypeSafeMatcher<HealthCheckSignal>("has exact config") {

            @Override
            protected boolean matchesSafely(HealthCheckSignal item) {
                return (item as HealthCheckSignal.TickSignal).getHealthCheckConfig() == expected
            }
        }
    }
}
