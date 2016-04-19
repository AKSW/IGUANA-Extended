package org.aksw.iguana.reborn;

/**
 * Again a pity that this is not part of standard java8 :(
 *
 * @author raven
 *
 * @param <T>
 */
public class Holder<T> {
    private T value = null;

    public Holder() {
    }

    public Holder(T value) {
        this.value = value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}