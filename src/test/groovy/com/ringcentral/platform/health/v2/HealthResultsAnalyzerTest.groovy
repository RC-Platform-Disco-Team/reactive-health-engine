package com.ringcentral.platform.health.v2

import com.ringcentral.platform.health.*
import com.ringcentral.platform.health.HealthCheckSignal.Type
import rx.functions.Action1
import rx.subjects.PublishSubject
import spock.lang.Specification

import static com.ringcentral.platform.health.HealthCheckSignal.Type.*
import static com.ringcentral.platform.health.HealthStateEnum.Critical
import static com.ringcentral.platform.health.HealthStateEnum.OK

class HealthResultsAnalyzerTest extends Specification {

    def ID1 = new HealthCheckID("1")
    def ID2 = new HealthCheckID("2")

    def "should set global health to the highest severity of results"() {
        setup:
        def state = Mock(HealthState)
        def analyzer = HealthResultsAnalyzer.create(state)
        def subject = PublishSubject.<HealthCheckSignal> create()

        when:
        subject.subscribe(analyzer)

        then:
        0 * state._(_)

        when:
        subject.onNext(result(ID1, OK, PERIODIC))
        subject.onNext(result(ID1, OK, PERIODIC))
        subject.onNext(result(ID1, OK, PERIODIC))

        then:
        3 * state.updateState({ it -> it.id == ID1 && it.state == OK })

        when:
        subject.onNext(result(ID2, OK, PERIODIC))
        subject.onNext(result(ID2, Critical, PASSIVE))
        subject.onNext(result(ID1, OK, null))
        subject.onNext(result(ID2, OK, PERIODIC))
        subject.onNext(result(ID2, OK, TICK))


        then:
        3 * state.updateState({ it -> it.id == ID2 })
    }

    def result(HealthCheckID id, HealthStateEnum state, Type type) {
        new HealthCheckResultWrapper(new HealthCheckResult(id, state, ""), type, null)
    }

}
