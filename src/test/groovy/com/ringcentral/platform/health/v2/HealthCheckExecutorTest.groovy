package com.ringcentral.platform.health.v2

import com.ringcentral.platform.health.*
import com.ringcentral.platform.health.demo.TestFunction
import org.hamcrest.CustomTypeSafeMatcher
import org.hamcrest.Description
import rx.Observable
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import rx.subjects.Subject
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.RejectedExecutionException
import java.util.stream.Collectors

import static com.ringcentral.platform.health.HealthCheckSignal.Type.PERIODIC
import static com.ringcentral.platform.health.HealthStateEnum.*
import static com.ringcentral.platform.health.demo.TestFunction.*

import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.hamcrest.CoreMatchers.everyItem
import static org.junit.Assert.assertArrayEquals
import static org.junit.Assert.assertThat

class HealthCheckExecutorTest extends Specification {

    @Shared engineConfig = Mock(HealthEngineConfig)
    @Shared singleThreadedExecutor = new HealthCheckExecutor(engineConfig, new FixedSizeScheduledPool(1, 5))
    @Shared fiveThreadedExecutor = new HealthCheckExecutor(engineConfig, new FixedSizeScheduledPool(5, 10))
    @Shared TestSubscriber<HealthCheckResultWrapper> subscriber

    def setupSpec() {
        engineConfig.getExecutionTimeout() >> Duration.ofSeconds(1)
    }

    def setup() {
        subscriber = new TestSubscriber<HealthCheckResultWrapper>()
    }

    def 'should successfully execute 1 check'() {
        given:
        def functions = [ok("1")]
        def observable = observable1(functions)

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        subscriber.assertValueCount(1)
        assertThat(subscriber.getOnNextEvents(), everyItem(hasState(OK)))
    }

    def 'should successfully execute 1 check with failure'() {
        setup:
        def functions = [fail("1")]
        def observable = observable1(functions)

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        subscriber.assertValueCount(1)
        assertThat(subscriber.getOnNextEvents(), everyItem(hasState(Critical)))
    }

    def 'should successfully execute 1 check with timeout'() {
        setup:
        def functions = [timeout("1")]
        def observable = observable1(functions)

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        assertThat(subscriber.getOnNextEvents(), everyItem(hasState(Timeout)))
    }

    def 'should successfully execute 1 check with slow'() {
        setup:
        def functions = [slow("1")]
        def observable = observable1(functions)

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        assertThat(subscriber.getOnNextEvents(), everyItem(hasState(Warning)))
    }

    def 'should successfully execute with different results'() {
        setup:
        def functions = [ok("1"),
                         fail("2"),
                         ok("3"),
                         slow("4"),
        ]
        def observable = observable1(functions)

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        assertThat(subscriber.getOnNextEvents(), allHaveStatesWithoutOrder([OK, Critical, OK, Warning]))
    }

    def 'should successfully execute with different results many threads'() {
        setup:
        def functions = [ok("1"),
                         fail("2"),
                         ok("3"),
                         slow("4"),
        ]
        def observable = observable5(functions)

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        assertThat(subscriber.getOnNextEvents(), allHaveStatesWithoutOrder([OK, Critical, OK, Warning]))
    }

    def 'should successfully execute with different results in consecutive order'() {
        setup:
        def functions = [ok("1"),
                         fail("2"),
                         timeout("3"),
                         fail("4"),
                         ok("5"),
                         slow("6")
        ]
        def observable = observable1(functions)

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        def actual = subscriber.getOnNextEvents().stream().map({ it.state }).toArray({ size -> new HealthStateEnum[size]})
        def expected = [OK, Critical, Timeout, Critical, OK, Warning] as HealthStateEnum[]
        assertArrayEquals(expected, actual)
    }

    def 'should full the pool and start rejecting requests'() {
        setup:
        def functions = [timeoutFail("1"),
                         ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"),
        ]
        def observable = observable1(functions)

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertError(RejectedExecutionException)
    }

    def 'should full the pool and start rejecting requests 5 threads'() {
        setup:
        def functions = [timeoutFail("1"), timeoutFail("1"), timeoutFail("1"), timeoutFail("1"), timeoutFail("1"),
                         ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"), ok("1"),
        ]
        def observable = observable5(functions)

        when:
        observable.subscribe(subscriber)

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertError(RejectedExecutionException)
    }

    def 'should successfully execute with isolating thread pool'() {
        setup:
        def functions = [ok("function"),
                         ok("check"),
                         ok("some-integration"),
                         ok("SOMETHING"),
        ]
        def subject = isoSubject(functions)

        when:
        subject.onNext(ok("function"))
        subject.onNext(ok("check"))
        subject.onNext(ok("some-integration"))
        subject.onNext(ok("SOMETHING"))
        subject.onNext(ok("function"))
        subject.onNext(slow("check"))
        subject.onNext(ok("some-integration"))
        subject.onNext(ok("SOMETHING"))
        subject.onNext(fail("function"))
        subject.onNext(timeout("check"))
        subject.onNext(ok("some-integration"))
        subject.onNext(fail("SOMETHING"))
        subject.onCompleted()

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        assertThat(subscriber.getOnNextEvents(), allHaveStatesWithoutOrder([OK, OK, OK, OK, OK, Warning, OK, OK, Critical, Timeout, Critical, OK]))
    }

    private Subject<TestFunction, TestFunction> isoSubject(List<TestFunction> functions) {
        def isolatingExecutor = new HealthCheckExecutor(engineConfig, new IsolatingScheduledThreadPool(functions))
        def subject = PublishSubject.<TestFunction> create()
        def observable = subject.map(this.&createRequest).flatMap({ isolatingExecutor.execute(it) })
        observable.subscribe(subscriber)
        subject
    }

    def "should ignore checks that is unexpected "() {
        def functions = [ok("1")]
        def subject = isoSubject(functions)

        when:
        subject.onNext(ok("function"))
        subject.onNext(ok("check"))
        subject.onNext(ok("some-integration"))
        subject.onNext(ok("SOMETHING"))

        then:
        subscriber.assertNoErrors()
        subscriber.assertNoValues()

        when:
        subject.onNext(ok("1"))
        subject.onNext(ok("function"))
        subject.onNext(ok("check"))
        subject.onCompleted()

        then:
        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        subscriber.assertValueCount(1)
    }

    def observable1(List<TestFunction> functions) {
        Observable.from(functions).map(this.&createRequest).flatMap({ singleThreadedExecutor.execute(it) })
    }

    def observable5(List<TestFunction> functions) {
        Observable.from(functions).map(this.&createRequest).flatMap({ fiveThreadedExecutor.execute(it) })
    }

    def observable5WithDelay(List<TestFunction> functions) {
        Observable.interval(500, MILLISECONDS).map({functions[it.intValue()]}).map(this.&createRequest).flatMap({ fiveThreadedExecutor.execute(it) })
    }

    def createRequest(TestFunction f) {
        return new HealthCheckRequest(f, PERIODIC, false, f.getSlowDuration())
    }

    def toRequest(TestFunction func) {
        new HealthCheckRequest(func, PERIODIC, false, func.getSlowDuration())
    }

    def hasState(expected) {
        new CustomTypeSafeMatcher<HealthCheckResultWrapper>("has exact state") {

            @Override
            protected boolean matchesSafely(HealthCheckResultWrapper item) {
                return (item as HealthCheckResultWrapper).state == expected
            }
        }
    }

    def allHaveStatesWithoutOrder(List<HealthStateEnum> expected) {
        new CustomTypeSafeMatcher<List<HealthCheckResultWrapper>>("all have states: " + serializeState(expected)) {

            String error

            @Override
            protected boolean matchesSafely(List<HealthCheckResultWrapper> actual) {
                def copy = new ArrayList<HealthCheckResultWrapper>(actual)
                for (HealthStateEnum item : expected) {
                    int idx = copy.findIndexOf { it.getState() == item }
                    if (idx == -1) {
                        error = "doesn't contain [" + item + "]"
                        return false
                    }
                    copy.remove(idx)
                }
                if (copy.isEmpty()) {
                    return true
                } else {
                    error = "has unexpected items " + serializeResult(copy)
                    return false
                }
            }

            @Override
            protected void describeMismatchSafely(List<HealthCheckResultWrapper> items, Description mismatchDescription) {
                if (error != null) {
                    mismatchDescription.appendText(error.toString() + " ")
                }
                super.describeMismatchSafely(serializeResult(items), mismatchDescription)
            }
        }
    }

    def serializeState(List<HealthStateEnum> items) {
        '[' + items.stream().map({ it.toString() }).collect(Collectors.joining(", ")) + ']'
    }

    def serializeResult(List<HealthCheckResultWrapper> items) {
        '[' + items.stream().map({ it.state.toString() }).collect(Collectors.joining(", ")) + ']'
    }
}
