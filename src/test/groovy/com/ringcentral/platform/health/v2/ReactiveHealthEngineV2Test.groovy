package com.ringcentral.platform.health.v2

import com.ringcentral.platform.health.HealthCheckConfig
import com.ringcentral.platform.health.HealthCheckFunction
import com.ringcentral.platform.health.HealthCheckID
import com.ringcentral.platform.health.HealthEngineConfig
import com.ringcentral.platform.health.HealthEngineConfigImpl
import com.ringcentral.platform.health.ReactiveHealthEngine
import spock.lang.Specification

import java.time.Clock
import java.time.Duration

import static com.ringcentral.platform.health.demo.TestFunction.*

class ReactiveHealthEngineV2Test extends Specification {

    def engineCfg = HealthEngineConfigImpl.builder().build()
    def healthCfg = Mock(HealthCheckConfig)

    def "should set global health to the highest severity of results"() {

        given:

        def f1 = ok('1')
        def f2 = fail('2')

        def checks = [f1, f2]

        _ * healthCfg.getSlowTimeout(_) >> Duration.ofMillis(500)
        _ * healthCfg.getPeriod(f1.id) >> Duration.ofSeconds(3)
        _ * healthCfg.getPeriod(f2.id) >> Duration.ofSeconds(2)
        _ * healthCfg.getRetryPeriod(f2.id) >> Duration.ofSeconds(1)

        def engine = new ReactiveHealthEngine(healthCfg, engineCfg, Clock.systemUTC(), checks as HealthCheckFunction[])

        when:
        print("test")

        then:
        Thread.sleep(50000)
    }

}
