package Utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class CustomList<E> implements Iterable<CustomList.Node<E>> {
    private Node<E> head;
    private Node<E> tail;
//    private L value;
    private int size;

    public CustomList() {
//        this.value = value;
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    public Node<E> getHead() {
        return head;
    }

    private void setHead(Node<E> head) {
        this.head = head;
    }

    public Node<E> getTail() {
        return tail;
    }

    private void setTail(Node<E> tail) {
        this.tail = tail;
    }

//    public L getValue() {
//        return value;
//    }

//    public void setValue(L value) {
//        this.value = value;
//    }

    public int getSize() {
        return size;
    }

    private void incrementSize() {
        this.size++;
    }

    private void decrementSize() {
        this.size--;
    }

    public void addToTail(Node<E> node) {
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

    public void addToHead(Node<E> node) {
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

//    public N getHeadValue() {
//        return getHead().getValue();
//    }

//    public N getTailValue() {
//        return getTail().getValue();
//    }

    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public Iterator<Node<E>> iterator() {
        return new CustomIterator(this.head);
    }

    class CustomIterator implements Iterator<Node<E>> {
        private Node<E> current;

        CustomIterator(Node<E> head) {
            this.current = head;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public Node<E> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Node<E> temp = current;
            current = current.next;
            return temp;
        }

        @Override
        public void remove() {
            if (current == null || current.parent == null) {
                throw new IllegalStateException();
            }
            Node<E> temp = current;
            current = current.next;
            temp.removeFromList();
        }
    }

    public static class Node<E> {
        private final E element;
        private Node<E> prev;
        private Node<E> next;
        private CustomList<E> parent;

        public Node(E element) {
            this.element = element;
        }

        public void insertAfter(Node<E> node) {
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

        public void insertBefore(Node<E> node) {
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

        public E getElement() {
            return element;
        }

//        public void setValue(N value) {
//            this.value = value;
//        }

        public Node<E> getPrev() {
            return prev;
        }

        public void setPrev(Node<E> prev) {
            this.prev = prev;
        }

        public Node<E> getNext() {
            return next;
        }

        public void setNext(Node<E> next) {
            this.next = next;
        }

        public CustomList<E> getParent() {
            return parent;
        }
    }
}
