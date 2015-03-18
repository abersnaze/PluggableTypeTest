package ptt;

import org.reactivestreams.Subscriber;

public interface Operator<R, T> extends Function<Subscriber<? super R>, Subscriber<? super T>> {
}
