package com.ringcentral.platform.health.v2

import com.ringcentral.platform.health.HealthCheckConfig
import com.ringcentral.platform.health.HealthEngineConfig
import spock.lang.Specification

import java.time.Clock
import java.time.Duration

import static com.ringcentral.platform.health.v2.TestFunction.*

class ReactiveHealthEngineV2Test extends Specification {

    def engineCfg = Mock(HealthEngineConfig)
    def healthCfg = Mock(HealthCheckConfig)

//    def "should set global health to the highest severity of results"() {
//
//        given:
//        def f1 = ok('1')
//        def f2 = ok('2')
//
////        def checks = [f1, f2]
////
////        1 * engineCfg.getTickSignalPeriod() >> Duration.ofSeconds(1)
////        _ * engineCfg.getExecutionTimeout() >> Duration.ofMillis(300)
////        _ * healthCfg.getSlowTimeout(_) >> Duration.ofMillis(500)
////
////        def engine = new ReactiveHealthEngineV2(healthCfg, engineCfg, Clock.systemUTC(), checks)
//
//
//        when:
//        print("test")
//
//        then:
//        sleep(5000)
//    }

    //TODO add tests with errors from config

}
