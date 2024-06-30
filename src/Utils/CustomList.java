package Utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class CustomList<N, L> implements Iterable<CustomList.Node<N, L>> {
    private Node<N, L> head;
    private Node<N, L> tail;
    private L value;
    private int size;

    public CustomList(L value) {
        this.value = value;
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    public Node<N, L> getHead() {
        return head;
    }

    private void setHead(Node<N, L> head) {
        this.head = head;
    }

    public Node<N, L> getTail() {
        return tail;
    }

    private void setTail(Node<N, L> tail) {
        this.tail = tail;
    }

    public L getValue() {
        return value;
    }

    public void setValue(L value) {
        this.value = value;
    }

    public int getSize() {
        return size;
    }

    private void incrementSize() {
        this.size++;
    }

    private void decrementSize() {
        this.size--;
    }

    public void addToTail(Node<N, L> node) {
        if (this.tail == null) {
            this.head = this.tail = node;
        } else {
            this.tail.next = node;
            node.prev = this.tail;
            this.tail = node;
        }
        node.parent = this;
        incrementSize();
    }

    public void addToHead(Node<N, L> node) {
        if (this.head == null) {
            this.head = this.tail = node;
        } else {
            this.head.prev = node;
            node.next = this.head;
            this.head = node;
        }
        node.parent = this;
        incrementSize();
    }

    public N getHeadValue() {
        return getHead().getValue();
    }

    public N getTailValue() {
        return getTail().getValue();
    }

    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public Iterator<Node<N, L>> iterator() {
        return new CustomIterator(this.head);
    }

    class CustomIterator implements Iterator<Node<N, L>> {
        private Node<N, L> current;

        CustomIterator(Node<N, L> head) {
            this.current = head;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public Node<N, L> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Node<N, L> temp = current;
            current = current.next;
            return temp;
        }

        @Override
        public void remove() {
            if (current == null || current.parent == null) {
                throw new IllegalStateException();
            }
            Node<N, L> temp = current;
            current = current.next;
            temp.removeFromList();
        }
    }

    public static class Node<N, L> {
        private N value;
        private Node<N, L> prev;
        private Node<N, L> next;
        private CustomList<N, L> parent;

        public Node(N value,L type) {
            this.value = value;
        }

        public void insertAfter(Node<N, L> node) {
            if (node == null) {
                throw new IllegalArgumentException("The given node cannot be null.");
            }
            this.next = node.next;
            node.next = this;
            this.prev = node;
            if (this.next != null) {
                this.next.prev = this;
            } else {
                this.parent.tail = this;
            }
            this.parent = node.parent;
            this.parent.incrementSize();
        }

        public void insertBefore(Node<N, L> node) {
            if (node == null) {
                throw new IllegalArgumentException("The given node cannot be null.");
            }
            this.prev = node.prev;
            node.prev = this;
            this.next = node;
            if (this.prev != null) {
                this.prev.next = this;
            } else {
                this.parent.head = this;
            }
            this.parent = node.parent;
            this.parent.incrementSize();
        }

        public void removeFromList() {
            if (this.parent == null) {
                return;
            }
            if (this.prev != null) {
                this.prev.next = this.next;
            } else {
                this.parent.head = this.next;
            }
            if (this.next != null) {
                this.next.prev = this.prev;
            } else {
                this.parent.tail = this.prev;
            }
            this.parent.decrementSize();
            this.prev = this.next = null;
            this.parent = null;
        }

        public N getValue() {
            return value;
        }

        public void setValue(N value) {
            this.value = value;
        }

        public Node<N, L> getPrev() {
            return prev;
        }

        public void setPrev(Node<N, L> prev) {
            this.prev = prev;
        }

        public Node<N, L> getNext() {
            return next;
        }

        public void setNext(Node<N, L> next) {
            this.next = next;
        }

        public CustomList<N, L> getParent() {
            return parent;
        }
    }
}
