package org.minenite.cardforge.event;

/**
 * Minimal stand-in for Fabric's {@code net.fabricmc.fabric.api.event.Event}.
 *
 * Cardboard defines a handful of its own callbacks with Fabric's array-backed
 * event helper. Only {@code register} and {@code invoker} are used, so the
 * platform dependency is not worth carrying.
 */
public abstract class Event<T> {

    protected volatile T invoker;

    /** The dispatcher that fans out to every registered listener. */
    public T invoker() {
        return this.invoker;
    }

    /** Add a listener. */
    public abstract void register(T listener);
}
