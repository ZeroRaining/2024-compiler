package midend.SSA;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class DFG {
    private Queue<Integer> worklist;
    private HashMap<Integer, Boolean> visited;
    private HashMap<Integer, Boolean> placed;

    public DFG() {
        worklist = new LinkedList<Integer>();
        visited = new HashMap<>();
        placed = new HashMap<>();
    }

}
