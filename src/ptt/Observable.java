package ptt;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Map;

public class Observable<T> implements Publisher<T> {
    private Publisher<T> f;

    public Observable(Publisher<T> f) {
        this.f = f;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        f.subscribe(s);
    }

    private <O extends Observable<R>, R> O lift(final Operator<? extends R, ? super T> op) {
        return null;
    }

    public <R> Observable<R> map(Function<T, R> transform) {
        return lift(rs -> new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                rs.onSubscribe(s);
            }

            @Override
            public void onNext(T t) {
                R r;
                try {
                    r = transform.call(t);
                } catch (Throwable e) {
                    rs.onError(e);
                    return;
                }
                rs.onNext(r);
            }

            @Override
            public void onError(Throwable e) {
                rs.onError(e);
            }

            @Override
            public void onComplete() {
                rs.onComplete();
            }
        });
    }

    public <R> Observable<R> merge() {
        return lift(rs -> new Subscriber<T>() {
            // work in progress (wip) is the number of uncompleted subscription. set to zero on error. decremented to
            // zero on all complete.
            int wip = 1;
            Subscription outterSub;
            Map<Subscriber<R>, Subscription> innerSubs;

            @Override
            public void onSubscribe(Subscription s) {
                outterSub = s;
                s.request(Long.MAX_VALUE);
                rs.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                    }

                    @Override
                    public void cancel() {
                        for (Subscription sub : innerSubs.values()) {
                            sub.cancel();
                        }
                    }
                });
            }

            @Override
            public void onNext(T t) {
                if (wip == 0)
                    return;
                wip++;
                ((Observable<R>) t).subscribe(new Subscriber<R>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                    }

                    @Override
                    public void onNext(R r) {
                        synchronized (rs) {
                            if (wip > 0)
                                rs.onNext(r);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        synchronized (rs) {
                            done(e);
                        }
                    }

                    @Override
                    public void onComplete() {
                        synchronized (rs) {
                            done(null);
                        }
                    }
                });
            }

            private void done(Throwable e) {
                if (e != null) {
                    if (wip > 0) {
                        rs.onError(e);
                        wip = 0;
                    }
                } else {
                    if (wip > 0) {
                        wip--;
                        if (wip == 0) {
                            rs.onComplete();
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                synchronized (rs) {
                    done(e);
                }
            }

            @Override
            public void onComplete() {
                synchronized (rs) {
                    done(null);
                }
            }
        });
    }

    public <K> Observable<Observable<T>> groupBy(Function<T, K> keySelect) {
        return lift(grps -> new Subscriber<T>() {
            Map<K, Subscriber<? super T>> groups;

            @Override
            public void onSubscribe(Subscription s) {
                grps.onSubscribe(s);
            }

            @Override
            public void onNext(T t) {
                K key = keySelect.call(t);
                Subscriber<? super T> grp = groups.get(key);
                if (grp == null) {
                    grps.onNext(new Observable<T>(newGrp -> {
                        groups.put(key, newGrp);
                    }));
                    grp = groups.get(key);
                    if (grp == null)
                        grps.onError(new IllegalStateException("Sub Observable not immediatly subscribed to"));
                }
                grp.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                grps.onError(t);
            }

            @Override
            public void onComplete() {
                grps.onComplete();
            }
        });
    }
}
