package test;



//@RunWith(MockitoJUnitRunner.class)
public class RxJavaTest {

//        public int show() {
//            return (true ? null : 0);
//        }
//
//        public static void main(String[] args) {
//            RxJavaTest b = new RxJavaTest();
//            b.show();
//        }

//    @Test
    public void t1() throws InterruptedException {

//        Observable.from(new Integer[]{1,2,3,4,5,0,1,2,3,4,5})
//                .lift(new SafeMapOperator<>(false, i -> {if (i== 0) throw new RuntimeException(); return i;}, e -> e.printStackTrace()))
//                .subscribe(i -> System.out.println(i));

//        List<HChF> checkers = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            checkers.add(new HChF("" + i));
//        }
//
//        Observable<HChF> start = Observable.from(checkers);
//        SerializedSubject<HealthCheckFunction, HealthCheckFunction> subject = PublishSubject.<HealthCheckFunction>create().toSerialized();
//        Observable<HealthCheckFunction> tickSender = Observable.merge(start, subject);
//
//        Executor ex = Executors.newFixedThreadPool(30);
//
//        Subscriber<HealthCheckSignal> analyzer = new SerializedSubscriber<>(new ActionSubscriber<HealthCheckSignal>(
//                s -> {
//                    if (s.getType() == PERIODIC || s.getType() == PASSIVE) {
//                        HealthCheckResult result = (HealthCheckResult) s;
//                        System.out.println("Received " + result + " for " + result.getId().getShortName() + " on thread " + Thread.currentThread().getName());
//                        int time = 3;
//                        if (result.getState() == HealthStateEnum.OK) {
//                            time = 10;
//                        }
//                        Observable.timer(time, TimeUnit.SECONDS).subscribeOn(Schedulers.from(ex)).subscribe((i) -> subject.onNext(new HChF("" + result.getId().getShortName() + "0")));
//                    } else if (s.getType() == EXPIRATION_CONTROL) {
//                        System.out.println("received expiration signal on thread " + Thread.currentThread().getName());
//                    }
//                },
//                e -> {},
//                () -> {}
//        ));
//
//        Observable<HealthCheckSignal> interval = Observable.interval(10, TimeUnit.SECONDS).map(l -> HealthCheckSignal.expiration());
//        SerializedSubject<HealthCheckSignal, HealthCheckSignal> passiveSubject = PublishSubject.<HealthCheckSignal>create().toSerialized();
//
//        Observable<HealthCheckSignal> observable = Observable.merge(tickSender
//                .flatMap(val -> Observable.just(val)
//                        .map(f -> {
//                            try {
//                                return f.checkHealth();
//                            } catch (Exception e) {
//                                return new HealthCheckResult(f.getId(), PERIODIC, HealthStateEnum.Critical, e.getMessage());
//                            }
//                        })
//                        .timeout(3, TimeUnit.SECONDS)
//                        .onErrorReturn((e) -> new HealthCheckResult(val.getId(), PERIODIC, HealthStateEnum.Critical, "Timeout"))
//                        .subscribeOn(Schedulers.from(ex))
//                ), passiveSubject, interval);
//        observable.observeOn(Schedulers.newThread()).subscribe(analyzer);
//
//
//
//        System.out.println("finish");
////        for (int i = 0; i < 10; i++) {
////            passiveSubject.onNext(new HealthCheckResult(new HealthCheckID("MDS passive"), PASSIVE, HealthStateEnum.OK, ""));
////            Thread.sleep(10000);
////        }
//
//        //while (true) {}
//
//        Thread.sleep(10000);
//
//        BlockingObservable<HealthCheckResult> force = Observable.from(checkers).flatMap(val -> Observable.just(val)
//                .map(f -> {
//                    System.out.println("FORCE!!!!");
//                    try {
//                        return f.checkHealth();
//                    } catch (Exception e) {
//                        return new HealthCheckResult(f.getId(), FORCED, HealthStateEnum.Critical, e.getMessage());
//                    }
//                })
//                .timeout(3, TimeUnit.SECONDS)
//                .onErrorReturn((e) -> new HealthCheckResult(val.getId(), FORCED, HealthStateEnum.Critical, "Timeout"))
//                .subscribeOn(Schedulers.from(ex))
//        ).toBlocking();
//        force.subscribe(analyzer);
//
//        System.out.println("win!!!");
//
//        while(true) {}

    }

//    private static class HChF implements HealthCheckFunction {
//
//        private HealthCheckID id;
//
//        HChF(String name) {
//            this.id = new HealthCheckID(name);
//        }
//
//        @Override
//        public HealthCheckID getId() {
//            return id;
//        }
//
//        @Override
//        public HealthCheckResult checkHealth() throws Exception {
//            System.out.println("Starting " + id.getShortName() + " on thread " + Thread.currentThread().getName());
//            Thread.sleep(Math.random() >= 0.1 ? 2000 : 10000);
//            if (Math.random() <= 0.1) {
//                throw new RuntimeException("Ouch!!!");
//            }
//            return Math.random() >= 0.1 ? ok() : critical("Some error!!!");
//        }
//    }
}
