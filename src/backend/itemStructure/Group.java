package backend.itemStructure;

import java.util.Objects;

public class Group<T1, T2> {
    private T1 first;
    private T2 second;

    public Group(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    public T1 getFirst() {
        return first;
    }

    public T2 getSecond() {
        return second;
    }

    public boolean equals(Object o) {
        if (o instanceof Group<?, ?> group) {
            return first.equals(group.first) &&
                    second.equals(group.second);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(first, second);
    }

}
