package org.minenite.cardforge.event;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Array-backed event construction, mirroring Fabric's EventFactory API. */
public final class EventFactory {

    private EventFactory() {
    }

    /**
     * @param type            the callback interface
     * @param invokerFactory  builds a dispatcher from the current listener array
     */
    public static <T> Event<T> createArrayBacked(Class<T> type, Function<T[], T> invokerFactory) {
        return new ArrayBackedEvent<>(type, invokerFactory);
    }

    private static final class ArrayBackedEvent<T> extends Event<T> {

        private final Class<T> type;
        private final Function<T[], T> invokerFactory;
        private final List<T> listeners = new ArrayList<>();

        private ArrayBackedEvent(Class<T> type, Function<T[], T> invokerFactory) {
            this.type = type;
            this.invokerFactory = invokerFactory;
            rebuild();
        }

        @Override
        public synchronized void register(T listener) {
            this.listeners.add(listener);
            rebuild();
        }

        @SuppressWarnings("unchecked")
        private void rebuild() {
            T[] array = this.listeners.toArray((T[]) Array.newInstance(this.type, 0));
            this.invoker = this.invokerFactory.apply(array);
        }
    }
}
