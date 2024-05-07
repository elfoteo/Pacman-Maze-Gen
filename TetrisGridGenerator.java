import java.util.*;

public class TetrisGridGenerator {
    static int rows = 9;
    static int cols = 5;
    static Cell[] cells = new Cell[rows * cols];
    private static final int[] narrowCols = new int[rows * cols];
    private static final int[] tallRows = new int[rows * cols];

    private static final int subrows = rows * 3 + 1 + 3;
    private static final int subcols = cols * 3 - 1 + 2;

    private static final int midcols = subcols - 2;
    private static final int fullcols = (subcols - 2) * 2;

    private static final char[] tiles = new char[subrows * fullcols]; // each is a character indicating a wall(|), path(.), or blank(_).
    private static final Cell[] tileCells = new Cell[subrows * subcols];

    static int UP = 0;
    static int RIGHT = 1;
    static int DOWN = 2;
    static int LEFT = 3;

    private static final Random random = new Random();

    // Function to generate a random integer between min and max (inclusive)
    public static int getRandomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    TetrisGridGenerator(){
        cells = new Cell[rows * cols];
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                cells[x+y*cols] = new Cell(x, y);
            }
        }
    }

    public static class Cell {
        // Fields
        public int x;
        public int y;
        public boolean filled;
        public Boolean[] connect;
        public Integer[] next;
        public Integer no;
        public Integer group;
        public boolean isShrinkWidthCandidate = false;
        public boolean isRaiseHeightCandidate = false;
        public boolean shrinkWidth = false;
        public boolean raiseHeight = false;
        public int final_x = 0;
        public int final_y = 0;
        public int final_w = 0;
        public int final_h = 0;
        public boolean isEdgeTunnelCandidate = false;
        public boolean isVoidTunnelCandidate = false;
        public boolean isSingleDeadEndCandidate = false;
        public int singleDeadEndDir;
        public boolean isDoubleDeadEndCandidate;
        public boolean topTunnel;
        public boolean isJoinCandidate;


        // Constructor
        public Cell(int x, int y) {
            this.x = x;
            this.y = y;
            this.filled = false;
            this.connect = new Boolean[] {false, false, false, false}; // Assuming 4 directions: LEFT, RIGHT, UP, DOWN
            this.next = new Integer[4];
            for (int i = 0; i < 4; i++) {
                connect[i] = false;
                next[i] = null;
            }
            this.no = null;
            this.group = null;
        }

        // toString method to represent Cell object as a string (Optional, for debugging purposes)
        @Override
        public String toString() {
            return "Cell{" +
                    "x=" + x +
                    ", y=" + y +
                    ", filled=" + filled +
                    ", connect=" + Arrays.toString(connect) +
                    ", no=" + no +
                    ", group=" + group +
                    ", next=" + Arrays.toString(next) +
                    ", final_x=" + final_x +
                    ", final_y=" + final_y +
                    ", final_w=" + final_w +
                    ", final_h=" + final_h +
                    '}';
        }
    }

    public static void reset() {
        for (int i = 0; i < rows * cols; i++) {
            int x = i % cols;
            int y = i / cols;
            cells[i] = new Cell(x, y);
        }

        for (int i = 0; i < rows * cols; i++) {
            if (cells[i].x > 0)
                cells[i].next[LEFT] = i - 1;
            if (cells[i].x < cols - 1)
                cells[i].next[RIGHT] = i + 1;
            if (cells[i].y > 0)
                cells[i].next[UP] = i - cols;
            if (cells[i].y < rows - 1)
                cells[i].next[DOWN] = i + cols;
        }

        int i = 3 * cols;
        cells[i].filled = true;
        cells[i].connect[LEFT] = cells[i].connect[RIGHT] = cells[i].connect[DOWN] = true;

        i++;
        cells[i].filled = true;
        cells[i].connect[LEFT] = cells[i].connect[DOWN] = true;

        i += cols - 1;
        cells[i].filled = true;
        cells[i].connect[LEFT] = cells[i].connect[UP] = cells[i].connect[RIGHT] = true;

        i++;
        cells[i].filled = true;
        cells[i].connect[UP] = cells[i].connect[LEFT] = true;
    }

    public static List<Integer> getLeftMostEmptyCells() {
        List<Integer> leftCells = new ArrayList<>();
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                int idx = x + y * cols;
                if (!cells[idx].filled) {
                    leftCells.add(idx);
                }
            }
            if (!leftCells.isEmpty()) {
                break;
            }
        }
        return leftCells;
    }

    public static boolean isOpenCell(int cellIdx, int i) {
        return isOpenCell(cellIdx, i, -1, -1);
    }

    public static boolean isOpenCell(int cellIdx, int i, int prevDir, int size) {
        if (cellIdx < 0 || cellIdx >= cells.length) {
            return false;
        }

        Cell cell = cells[cellIdx];

        // prevent wall from going through starting position
        if ((cell.y == 6 && cell.x == 0 && i == DOWN) ||
                (cell.y == 7 && cell.x == 0 && i == UP)) {
            return false;
        }

        // prevent long straight pieces of length 3
        if (prevDir != -1 && size == 2 && (i == prevDir || (i + 2) % 4 == prevDir)) {
            return false;
        }

        // examine an adjacent empty cell
        if (cell.next[i] != null && !cells[cell.next[i]].filled) {
            // only open if the cell to the left of it is filled
            return cells[cell.next[i]].next[LEFT] == null || cells[cells[cell.next[i]].next[LEFT]].filled;
        }
        return false;
    }

    public static class OpenCellsResult {
        List<Integer> openCells;
        int numOpenCells;

        public OpenCellsResult(List<Integer> openCells, int numOpenCells) {
            this.openCells = openCells;
            this.numOpenCells = numOpenCells;
        }
    }

    public static OpenCellsResult getOpenCells(int cellIdx, int prevDir, int size) {
        List<Integer> openCells = new ArrayList<>();
        int numOpenCells = 0;
        for (int i = 0; i < 4; i++) {
            if (isOpenCell(cellIdx, i, prevDir, size)) {
                openCells.add(i);
                numOpenCells++;
            }
        }
        return new OpenCellsResult(openCells, numOpenCells);
    }

    public static void connectCell(int cellIdx, int dir) {
        cells[cellIdx].connect[dir] = true;
        if (cells[cellIdx].next[dir] != null) {
            cells[cells[cellIdx].next[dir]].connect[(dir + 2) % 4] = true;
        }
        if (cells[cellIdx].x == 0 && dir == RIGHT) {
            cells[cellIdx].connect[LEFT] = true;
        }
    }

    public static void setResizeCandidates() {
        for (int i = 0; i < rows * cols; i++) {
            // determine if it has flexible height

            // |_|
            // or
            //  _
            // | |
            if ((cells[i].x == 0 || !cells[i].connect[LEFT]) &&
                    (cells[i].x == cols - 1 || !cells[i].connect[RIGHT]) &&
                    cells[i].connect[UP] != cells[i].connect[DOWN]) {
                cells[i].isRaiseHeightCandidate = true;
            }

            //  _ _
            // |_ _|
            if (cells[i].next[RIGHT] != null) {
                if (((cells[i].x == 0 || !cells[i].connect[LEFT]) &&
                        !cells[i].connect[UP] && !cells[i].connect[DOWN]) &&
                        ((cells[cells[i].next[RIGHT]].x == cols - 1 ||
                                !cells[cells[i].next[RIGHT]].connect[RIGHT]) &&
                                !cells[cells[i].next[RIGHT]].connect[UP] &&
                                !cells[cells[i].next[RIGHT]].connect[DOWN])) {
                    cells[cells[i].next[RIGHT]].isRaiseHeightCandidate = true;
                    cells[i].isRaiseHeightCandidate = true;
                }
            }

            // determine if it has flexible width

            // if cell is on the right edge with an opening to the right
            if (cells[i].x == cols - 1 && cells[i].connect[RIGHT]) {
                cells[i].isShrinkWidthCandidate = true;
            }

            //  _
            // |_
            // or
            //  _
            //  _|
            if ((cells[i].y == 0 || !cells[i].connect[UP]) &&
                    (cells[i].y == rows - 1 || !cells[i].connect[DOWN]) &&
                    cells[i].connect[LEFT] != cells[i].connect[RIGHT]) {
                cells[i].isShrinkWidthCandidate = true;
            }
        }
    }

    public static void fillCell(int cellIndex, int numFilled, int numGroups) {
        cells[cellIndex].filled = true;
        cells[cellIndex].no = numFilled;
        cells[cellIndex].group = numGroups;
    }


    public static void gen() {
        int cellIdx;                        // cell at the center of growth (open cells are chosen around this cell)
        Integer newCellIdx = null;          // most recent cell filled
        int firstCellIdx;                   // the starting cell of the current group

        List<Integer> openCells;            // list of open cells around the center cell
        int numOpenCells;                   // size of openCells

        int dir = 0;                        // the most recent direction of growth relative to the center cell
        int i;                              // loop control variable used for iterating directions

        int numFilled = 0;  // current count of total cells filled
        int numGroups;      // current count of cell groups created
        int size;           // current number of cells in the current group
        double[] probStopGrowingAtSize = { // probability of stopping growth at sizes...
                0,     // size 0
                0,     // size 1
                0.10,  // size 2
                0.5,   // size 3
                0.75,  // size 4
                1};    // size 5

        // A single cell group of size 1 is allowed at each row at y=0 and y=rows-1,
        // so keep count of those created.
        Map<Integer, Integer> singleCount = new HashMap<>();
        singleCount.put(0, 0);
        singleCount.put(rows - 1, 0);
        double probTopAndBotSingleCellJoin = 0.35;

        // A count and limit of the number long pieces (i.e. an "L" of size 4 or "T" of size 5)
        int longPieces = 0;
        int maxLongPieces = 1;
        // double probExtendAtSize2 = 1;
        double probExtendAtSize3or4 = 0.5;

        for (numGroups = 0;; numGroups++) {
            // find all the leftmost empty cells
            openCells = getLeftMostEmptyCells();

            // stop add pieces if there are no more empty cells.
            numOpenCells = openCells.size();
            if (numOpenCells == 0) {
                break;
            }

            // choose the center cell to be a random open cell, and fill it.
            int currentIndex = openCells.get(getRandomInt(0, numOpenCells - 1));
            cellIdx = currentIndex;
            firstCellIdx = currentIndex;
            fillCell(currentIndex, numFilled, numGroups);
            numFilled++;

            // randomly allow one single-cell piece on the top or bottom of the map.
            if (cells[cellIdx].x < cols - 1 && (singleCount.containsKey(cells[cellIdx].y)) &&
                    Math.random() <= probTopAndBotSingleCellJoin) {
                if (singleCount.get(cells[cellIdx].y) == 0) {
                    cells[cellIdx].connect[cells[cellIdx].y == 0 ? UP : DOWN] = true;
                    singleCount.put(cells[cellIdx].y, 1);
                    continue;
                }
            }

            // number of cells in this contiguous group
            size = 1;

            if (cells[cellIdx].x == cols - 1) {
                // if the first cell is at the right edge, then don't grow it.
                cells[cellIdx].connect[RIGHT] = true;
                cells[cellIdx].isRaiseHeightCandidate = true;
            } else {
                // only allow the piece to grow to 5 cells at most.
                while (size < 5) {
                    boolean stop = false;
                    if (size == 2) {
                        // With a horizontal 2-cell group, try to turn it into a 4-cell "L" group.
                        // This is done here because this case cannot be reached when a piece has already grown to size 3.
                        if (cells[currentIndex].connect[RIGHT] &&
                                cells[currentIndex].next[RIGHT] != null && cells[cells[currentIndex].next[RIGHT]].next[RIGHT] != null) {
                            if (longPieces < maxLongPieces) {
                                currentIndex = cells[cells[currentIndex].next[RIGHT]].next[RIGHT];

                                Map<Integer, Boolean> dirs = new HashMap<>();
                                if (isOpenCell(currentIndex, UP)) {
                                    dirs.put(UP, true);
                                }
                                if (isOpenCell(currentIndex, DOWN)) {
                                    dirs.put(DOWN, true);
                                }

                                if (dirs.containsKey(UP) && dirs.containsKey(DOWN)) {
                                    i = (new int[]{UP, DOWN})[getRandomInt(0, 1)];
                                } else if (dirs.containsKey(UP)) {
                                    i = UP;
                                } else if (dirs.containsKey(DOWN)) {
                                    i = DOWN;
                                } else {
                                    i = -1;
                                }

                                if (i != -1) {
                                    connectCell(currentIndex, LEFT);
                                    fillCell(currentIndex, numFilled, numGroups);
                                    numFilled++;
                                    connectCell(currentIndex, i);
                                    cells[cells[currentIndex].next[i]].filled = true;
                                    cells[cells[currentIndex].next[i]].no = numFilled;
                                    cells[cells[currentIndex].next[i]].group = numGroups;

                                    numFilled++;
                                    longPieces++;
                                    size += 2;
                                    stop = true;
                                }
                            }
                        }
                    }

                    if (!stop) {
                        // find available open adjacent cells.
                        OpenCellsResult result = getOpenCells(cellIdx, dir, size);

                        openCells = result.openCells;
                        numOpenCells = result.numOpenCells;

                        // if no open cells found from center point, then use the last cell as the new center
                        // but only do this if we are of length 2 to prevent numerous short pieces.
                        // then recalculate the open adjacent cells.
                        if (numOpenCells == 0 && size == 2) {
                            cellIdx = newCellIdx;
                            result = getOpenCells(cellIdx, dir, size);
                            openCells = result.openCells;
                            numOpenCells = result.numOpenCells;
                        }

                        // no more adjacent cells, so stop growing this piece.
                        if (numOpenCells == 0) {
                            if (size == 1){
                                System.err.println("---> Sorry i need to crash, this shouldn't be possible");
                                int y = 0/0;
                            }
                            stop = true;
                        } else {
                            // This condition gets triggered at least one time before the one up

                            // choose a random valid direction to grow.
                            dir = openCells.get(getRandomInt(0, numOpenCells - 1)); // CHANGE MADE HERE
                            newCellIdx = cells[cellIdx].next[dir];
                            /*
                            int exportDir;
                            do{
                                dir = getRandomInt(0, 3);
                                newCellIdx = cells[cellIdx].next[dir];
                                exportDir = dir;
                            }
                            while (newCellIdx == null);
                            dir = exportDir;
                            */
                            // connect the cell to the new cell.
                            connectCell(cellIdx, dir);

                            // fill the cell
                            fillCell(cellIdx, numFilled, numGroups);
                            numFilled++;

                            // increase the size count of this piece.
                            size++;
                            // don't let center pieces grow past 3 cells
                            if (cells[firstCellIdx].x == 0 && size == 3) {
                                stop = true;
                            }

                            // Use a probability to determine when to stop growing the piece.
                            if (Math.random() <= probStopGrowingAtSize[size]) {
                                stop = true;
                            }
                        }
                    }

                    // Close the piece.
                    if (stop) {
                        if (size == 1){
                            System.err.println("---> Sorry i need to crash, this shouldn't be possible");
                            int x = 0/0; // CHECK THE ORIGINAL CODE; THIS SHOULD NOT BE POSSIBLE!!!!
                        }
                        else if (size == 2) {
                            // With a vertical 2-cell group, attach to the right wall if adjacent.
                            if (cells[firstCellIdx].x == cols - 1) {
                                // select the top cell
                                if (cells[firstCellIdx].connect[UP]) {
                                    firstCellIdx = cells[firstCellIdx].next[UP];
                                }
                                cells[firstCellIdx].connect[RIGHT] = true;

                                if (cells[firstCellIdx].next[DOWN] != null){
                                    cells[cells[firstCellIdx].next[DOWN]].connect[RIGHT] = true;
                                }
                            }
                        } else if (size == 3 || size == 4) {

                            // Try to extend group to have a long leg
                            if (longPieces < maxLongPieces && cells[firstCellIdx].x > 0 && Math.random() <= probExtendAtSize3or4) {
                                Map<Integer, Boolean> dirs = new HashMap<>();
                                int dirsLength = 0;
                                for (i = 0; i < 4; i++) {
                                    if (cells[cellIdx].connect[i] && isOpenCell(cells[cellIdx].next[i], i)) {
                                        dirs.put(i, true);
                                        dirsLength++;
                                    }
                                }
                                if (dirsLength > 0) {
                                    i = new ArrayList<>(dirs.keySet()).get(getRandomInt(0, dirsLength - 1));

                                    connectCell(cellIdx, i);
                                    fillCell(cells[cells[cellIdx].next[i]].next[i], numFilled, numGroups);
                                    numFilled++;
                                    longPieces++;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
        setResizeCandidates();
    }

    // Identify if a cell is the center of a cross.
    public static boolean cellIsCrossCenter(Cell c) {
        return c.connect[UP] && c.connect[RIGHT] && c.connect[DOWN] && c.connect[LEFT];
    }

    public static <T> void shuffle(List<T> list) {
        int len = list.size();
        for (int i = 0; i < len; i++) {
            int j = getRandomInt(0, len - 1);
            T temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }

    public static boolean chooseNarrowCols() {
        for (int cellIdx = cols - 1; cellIdx >= 0; cellIdx--) {
            if (cells[cellIdx].isShrinkWidthCandidate && canShrinkWidth(cellIdx, 0)) {
                cells[cellIdx].shrinkWidth = true;
                narrowCols[cells[cellIdx].y] = cells[cellIdx].x;
                return true;
            }
        }
        return false;
    }

    public static boolean canShrinkWidth(int x, int y) {
        // Can cause no more tight turns.
        if (y == rows - 1) {
            return true;
        }

        // get the right-hand-side bound
        int x0;
        Cell c;
        Integer c2 = null;
        for (x0 = x; x0 < cols; x0++) {
            c = cells[x0 + y * cols];
            c2 = c.next[DOWN];
            if ((!c.connect[RIGHT] || cellIsCrossCenter(c)) &&
                    (!cells[c2].connect[RIGHT] || cellIsCrossCenter(cells[c2]))) {
                break;
            }
        }

        // build candidate list
        ArrayList<Integer> candidates = new ArrayList<>();
        int numCandidates = 0;
        for (; c2 != null; c2 = cells[c2].next[LEFT]) {
            if (cells[c2].isShrinkWidthCandidate) {
                candidates.add(c2);
                numCandidates++;
            }

            // cannot proceed further without causing irreconcilable tight turns
            if ((!cells[c2].connect[LEFT] || cellIsCrossCenter(cells[c2])) &&
                    (!cells[cells[c2].next[UP]].connect[LEFT] || cellIsCrossCenter(cells[cells[c2].next[UP]]))) {
                break;
            }
        }
        Collections.shuffle(candidates);

        for (int i = 0; i < numCandidates; i++) {
            c2 = candidates.get(i);
            if (canShrinkWidth(cells[c2].x, cells[c2].y)) {
                cells[c2].shrinkWidth = true;
                narrowCols[cells[c2].y] = cells[c2].x;
                return true;
            }
        }

        return false;
    }


    public static boolean chooseTallRows() {
        for (int y = 0; y < 3; y++) {
            int cellIdx = y * cols;
            if (cells[cellIdx].isRaiseHeightCandidate && canRaiseHeight(0, y)) {
                cells[cellIdx].raiseHeight = true;
                tallRows[cells[cellIdx].x] = cells[cellIdx].y;
                return true;
            }
        }

        return false;
    }

    public static boolean canRaiseHeight(int x, int y) {
        // Can cause no more tight turns.
        if (x == cols - 1) {
            return true;
        }

        // find the first cell below that will create too tight a turn on the right
        int y0;
        Cell c;
        Integer c2 = null;
        for (y0 = y; y0 >= 0; y0--) {
            c = cells[x + y0 * cols];
            c2 = c.next[RIGHT];
            if ((!c.connect[UP] || cellIsCrossCenter(c)) &&
                    (!cells[c2].connect[UP] || cellIsCrossCenter(cells[c2]))) {
                break;
            }
        }

        // Proceed from the right cell upwards, looking for a cell that can be raised.
        List<Integer> candidates = new ArrayList<>();
        int numCandidates = 0;
        for (; c2 != null; c2 = cells[c2].next[DOWN]) {
            if (cells[c2].isRaiseHeightCandidate) {
                candidates.add(c2);
                numCandidates++;
            }

            // cannot proceed further without causing irreconcilable tight turns
            if ((!cells[c2].connect[DOWN] || cellIsCrossCenter(cells[c2])) &&
                    (!cells[cells[c2].next[LEFT]].connect[DOWN] || cellIsCrossCenter(cells[cells[c2].next[LEFT]]))) {
                break;
            }
        }
        Collections.shuffle(candidates);

        for (int i = 0; i < numCandidates; i++) {
            c2 = candidates.get(i);
            if (canRaiseHeight(cells[c2].x, cells[c2].y)) {
                cells[c2].raiseHeight = true;
                tallRows[cells[c2].x] = cells[c2].y;
                return true;
            }
        }

        return false;
    }


    public static boolean isDesirable() {
        // Ensure a solid top right corner
        Cell c = cells[4];

        if (c.connect[UP] || c.connect[RIGHT]) {
            System.out.println("err in first check");
            return false;
        }

        // Ensure a solid bottom right corner
        c = cells[rows * cols - 1];
        if (c.connect[DOWN] || c.connect[RIGHT]) {
            System.out.println("err in second check");
            return false;
        }

        // Ensure there are no two stacked/side-by-side 2-cell pieces
        for (int y = 0; y < rows - 1; y++) {
            for (int x = 0; x < cols - 1; x++) {
                if ((isHori(x, y) && isHori(x, y + 1)) ||
                        (isVert(x, y) && isVert(x + 1, y))) {

                    // Don't allow them in the middle because they'll be two large when reflected.
                    if (x == 0) {
                        System.out.println("err in third check");
                        return false;
                    }

                    // Join the four cells to create a square
                    cells[x + y * cols].connect[DOWN] = true;
                    cells[x + y * cols].connect[RIGHT] = true;
                    int group = cells[x + y * cols].group;

                    cells[x + 1 + y * cols].connect[DOWN] = true;
                    cells[x + 1 + y * cols].connect[LEFT] = true;
                    cells[x + 1 + y * cols].group = group;

                    cells[x + (y + 1) * cols].connect[UP] = true;
                    cells[x + (y + 1) * cols].connect[RIGHT] = true;
                    cells[x + (y + 1) * cols].group = group;

                    cells[x + 1 + (y + 1) * cols].connect[UP] = true;
                    cells[x + 1 + (y + 1) * cols].connect[LEFT] = true;
                    cells[x + 1 + (y + 1) * cols].group = group;
                }
            }
        }
        if (false){
            chooseTallRows();
            chooseNarrowCols();
            return true;
        }
        if (!chooseTallRows()) {
            System.out.println("err in 4th check");
            return false;
        }
        return chooseNarrowCols();
    }

    private static boolean isHori(int x, int y) {
        Cell c1 = cells[x + y * cols];
        Cell c2 = cells[x + 1 + y * cols];
        return !c1.connect[UP] && !c1.connect[DOWN] &&
                (x == 0 || !c1.connect[LEFT]) && c1.connect[RIGHT] &&
                !c2.connect[UP] && !c2.connect[DOWN] &&
                c2.connect[LEFT] && !c2.connect[RIGHT];
    }

    private static boolean isVert(int x, int y) {
        Cell c1 = cells[x + y * cols];
        Cell c2 = cells[x + (y + 1) * cols];
        if (x == cols - 1) {
            // Special case (we can consider two single cells as vertical at the right edge)
            return !c1.connect[LEFT] && !c1.connect[UP] &&
                    !c1.connect[DOWN] && !c2.connect[LEFT] &&
                    !c2.connect[UP] && !c2.connect[DOWN];
        }
        return !c1.connect[LEFT] && !c1.connect[RIGHT] &&
                !c1.connect[UP] && c1.connect[DOWN] &&
                !c2.connect[LEFT] && !c2.connect[RIGHT] &&
                c2.connect[UP] && !c2.connect[DOWN];
    }

    public void setUpScaleCoords() {
        for (int cellIdx = 0; cellIdx < rows * cols; cellIdx++) {
            cells[cellIdx].final_x = cells[cellIdx].x * 3;
            if (narrowCols[cells[cellIdx].y] < cells[cellIdx].x) {
                cells[cellIdx].final_x--;
            }
            cells[cellIdx].final_y = cells[cellIdx].y * 3;
            if (tallRows[cells[cellIdx].x] < cells[cellIdx].y) {
                cells[cellIdx].final_y++;
            }
            cells[cellIdx].final_w = cells[cellIdx].shrinkWidth ? 2 : 3;
            cells[cellIdx].final_h = cells[cellIdx].raiseHeight ? 4 : 3;
        }
    }


    public static <T> boolean isNotOutOfBounds(T[] array, int index) {
        return index >= 0 && index < array.length;
    }

    public static boolean createTunnels() {
        List<Integer> singleDeadEndCells = new ArrayList<>();
        List<Integer> topSingleDeadEndCells = new ArrayList<>();
        List<Integer> botSingleDeadEndCells = new ArrayList<>();
        List<Integer> voidTunnelCells = new ArrayList<>();
        List<Integer> topVoidTunnelCells = new ArrayList<>();
        List<Integer> botVoidTunnelCells = new ArrayList<>();
        List<Integer> edgeTunnelCells = new ArrayList<>();
        List<Integer> topEdgeTunnelCells = new ArrayList<>();
        List<Integer> botEdgeTunnelCells = new ArrayList<>();
        List<Integer> doubleDeadEndCells = new ArrayList<>();
        int numTunnelsCreated;

        for (int y = 0; y < rows; y++) {
            Integer currCellIdx = cols - 1 + y * cols;
            if (cells[currCellIdx].connect[UP]) {
                continue;
            }
            if (cells[currCellIdx].y > 1 && cells[currCellIdx].y < rows - 2) {
                cells[currCellIdx].isEdgeTunnelCandidate = true;
                edgeTunnelCells.add(currCellIdx);
                if (cells[currCellIdx].y <= 2) {
                    topEdgeTunnelCells.add(currCellIdx);
                } else if (cells[currCellIdx].y >= 5) {
                    botEdgeTunnelCells.add(currCellIdx);
                }
            }
            boolean upDead = (isNotOutOfBounds(cells[currCellIdx].next, UP) || cells[cells[currCellIdx].next[UP]].connect[RIGHT]); // MODIFIED
            boolean downDead = (isNotOutOfBounds(cells[currCellIdx].next, DOWN) || cells[cells[currCellIdx].next[DOWN]].connect[RIGHT]); // MODIFIED
            if (cells[currCellIdx].connect[RIGHT]) {
                if (upDead) {
                    cells[currCellIdx].isVoidTunnelCandidate = true;
                    voidTunnelCells.add(currCellIdx);
                    if (cells[currCellIdx].y <= 2) {
                        topVoidTunnelCells.add(currCellIdx);
                    } else if (cells[currCellIdx].y >= 6) {
                        botVoidTunnelCells.add(currCellIdx);
                    }
                }
            } else {
                if (cells[currCellIdx].connect[DOWN]) {
                    continue;
                }
                if (upDead != downDead) {
                    if (!cells[currCellIdx].raiseHeight && y < rows - 1 && !cells[cells[currCellIdx].next[LEFT]].connect[LEFT]) {
                        singleDeadEndCells.add(currCellIdx);
                        cells[currCellIdx].isSingleDeadEndCandidate = true;
                        cells[currCellIdx].singleDeadEndDir = upDead ? UP : DOWN;
                        int offset = upDead ? 1 : 0;
                        if (cells[currCellIdx].y <= 1 + offset) {
                            topSingleDeadEndCells.add(currCellIdx);
                        } else if (cells[currCellIdx].y >= 5 + offset) {
                            botSingleDeadEndCells.add(currCellIdx);
                        }
                    }
                } else if (upDead) {
                    if (y > 0 && y < rows - 1) {
                        if (cells[cells[currCellIdx].next[LEFT]].connect[UP] && cells[cells[currCellIdx].next[LEFT]].connect[DOWN]) {
                            cells[currCellIdx].isDoubleDeadEndCandidate = true;
                            if (cells[currCellIdx].y >= 2 && cells[currCellIdx].y <= 5) {
                                doubleDeadEndCells.add(currCellIdx);
                            }
                        }
                    }
                }
            }
        }

        int numTunnelsDesired = random.nextDouble() <= 0.45 ? 2 : 1;
        Integer randomCellIdx;
        if (numTunnelsDesired == 1) {
            if ((randomCellIdx = randomElement(voidTunnelCells)) != null) {
                cells[randomCellIdx].topTunnel = true;
            } else if ((randomCellIdx = randomElement(singleDeadEndCells)) != null) {
                selectSingleDeadEnd(randomCellIdx);
            } else if ((randomCellIdx = randomElement(edgeTunnelCells)) != null) {
                cells[randomCellIdx].topTunnel = true;
            } else {
                return false;
            }
        } else {
            if ((randomCellIdx = randomElement(doubleDeadEndCells)) != null) {
                cells[randomCellIdx].connect[RIGHT] = true;
                cells[randomCellIdx].topTunnel = true;
                cells[cells[randomCellIdx].next[DOWN]].topTunnel = true;
            } else {
                numTunnelsCreated = 1;
                if ((randomCellIdx = randomElement(topVoidTunnelCells)) != null) {
                    cells[randomCellIdx].topTunnel = true;
                } else if ((randomCellIdx = randomElement(topSingleDeadEndCells)) != null) {
                    selectSingleDeadEnd(randomCellIdx);
                } else if ((randomCellIdx = randomElement(topEdgeTunnelCells)) != null) {
                    cells[randomCellIdx].topTunnel = true;
                } else {
                    numTunnelsCreated = 0;
                }

                if ((randomCellIdx = randomElement(botVoidTunnelCells)) != null) {
                    cells[randomCellIdx].topTunnel = true;
                } else if ((randomCellIdx = randomElement(botSingleDeadEndCells)) != null) {
                    selectSingleDeadEnd(randomCellIdx);
                } else if ((randomCellIdx = randomElement(botEdgeTunnelCells)) != null) {
                    cells[randomCellIdx].topTunnel = true;
                } else {
                    if (numTunnelsCreated == 0) {
                        return false;
                    }
                }
            }
        }

        for (int y = 0; y < rows; y++) {
            randomCellIdx = cols - 1 + y * cols;
            if (cells[randomCellIdx].topTunnel) {
                boolean exit = true;
                int topy = cells[randomCellIdx].final_y;
                while (cells[randomCellIdx].next[LEFT] != null) {
                    randomCellIdx = cells[randomCellIdx].next[LEFT];
                    if (!(!cells[randomCellIdx].connect[UP] && cells[randomCellIdx].final_y == topy)) {
                        exit = false;
                        break;
                    }
                }
                if (exit) {
                    return false;
                }
            }
        }

        for (Integer voidTunnelCell : voidTunnelCells) {
            randomCellIdx = voidTunnelCell;
            if (!cells[randomCellIdx].topTunnel) {
                replaceGroup(cells[randomCellIdx].group, cells[cells[randomCellIdx].next[UP]].group);
                cells[randomCellIdx].connect[UP] = true;
                cells[cells[randomCellIdx].next[UP]].connect[DOWN] = true;
            }
        }

        return true;
    }

    public static <T> T randomElement(List<T> list) {
        if (list.isEmpty()) return null;
        int index = random.nextInt(list.size());
        return list.remove(index);
    }

    private static void selectSingleDeadEnd(int cellIdx) {
        cells[cellIdx].connect[RIGHT] = true;
        if (cells[cellIdx].singleDeadEndDir == UP) {
            cells[cellIdx].topTunnel = true;
        } else {
            cells[cells[cellIdx].next[DOWN]].topTunnel = true;
        }
    }

    private static void replaceGroup(Integer oldg, Integer newg) {
        for (int cellIdx = 0; cellIdx < rows * cols; cellIdx++) {
            if (Objects.equals(cells[cellIdx].group, oldg)) {
                cells[cellIdx].group = newg;
            }
        }
    }

    public static void joinWalls() {

        // join cells to the top boundary
        for (int currCellIdx = 0; currCellIdx < cols; currCellIdx++) {
            if (!cells[currCellIdx].connect[LEFT] && !cells[currCellIdx].connect[RIGHT] && !cells[currCellIdx].connect[UP] &&
                    (!cells[currCellIdx].connect[DOWN] || !cells[currCellIdx + cols].connect[DOWN])) {

                // ensure it will not create a dead-end
                if ((isNotOutOfBounds(cells[currCellIdx].next, LEFT) || !cells[cells[currCellIdx].next[LEFT]].connect[UP]) &&
                        (cells[currCellIdx].next[RIGHT] != null && !cells[cells[currCellIdx].next[RIGHT]].connect[UP])) {

                    // prevent connecting very large piece
                    if (!(cells[currCellIdx].next[DOWN] != null && cells[cells[currCellIdx].next[DOWN]].connect[RIGHT] && cells[cells[cells[currCellIdx].next[DOWN]].next[RIGHT]].connect[RIGHT])) {
                        cells[currCellIdx].isJoinCandidate = true;
                        if (Math.random() <= 0.25) {
                            cells[currCellIdx].connect[UP] = true;
                        }
                    }
                }
            }
        }

        // join cells to the bottom boundary
        for (int x = 0; x < cols; x++) {
            int cellIdx = x + (rows - 1) * cols;
            if (!cells[cellIdx].connect[LEFT] && !cells[cellIdx].connect[RIGHT] && !cells[cellIdx].connect[DOWN] &&
                    (!cells[cellIdx].connect[UP] || !cells[x + (rows - 2) * cols].connect[UP])) {

                // ensure it will not create a dead-end
                if ((isNotOutOfBounds(cells[cellIdx].next, LEFT) || !cells[cells[cellIdx].next[LEFT]].connect[DOWN]) &&
                        (cells[cellIdx].next[RIGHT] != null && !cells[cells[cellIdx].next[RIGHT]].connect[DOWN])) {

                    // prevent connecting very large piece
                    if (!(cells[cellIdx].next[UP] != null && cells[cells[cellIdx].next[UP]].connect[RIGHT] && cells[cells[cells[cellIdx].next[UP]].next[RIGHT]].connect[RIGHT])) {
                        cells[cellIdx].isJoinCandidate = true;
                        if (Math.random() <= 0.25) {
                            cells[cellIdx].connect[DOWN] = true;
                        }
                    }
                }
            }
        }

        // join cells to the right boundary
        for (int y = 1; y < rows - 1; y++) {
            int cellIdx = cols - 1 + y * cols;
            if (cells[cellIdx].raiseHeight) {
                continue;
            }
            if (!cells[cellIdx].connect[RIGHT] && !cells[cellIdx].connect[UP] && !cells[cellIdx].connect[DOWN] &&
                    !cells[cells[cellIdx].next[UP]].connect[RIGHT] && !cells[cells[cellIdx].next[DOWN]].connect[RIGHT]) {
                if (cells[cellIdx].connect[LEFT]) {
                    int otherCellIdx = cells[cellIdx].next[LEFT];
                    if (isNotOutOfBounds(cells[otherCellIdx].connect, UP) && !cells[otherCellIdx].connect[DOWN] && !cells[otherCellIdx].connect[LEFT]) {
                        cells[cellIdx].isJoinCandidate = true;
                        if (Math.random() <= 0.5) {
                            cells[cellIdx].connect[RIGHT] = true;
                        }
                    }
                }
            }
        }
    }

    void genRandom() {
        // Try to generate a valid map, and keep count of tries.
        int genCount = 0;
        boolean failed = false;
        while (true) {
            reset();
            gen();
            genCount++;
            if (!isDesirable()) {
                System.out.println("GRRRR, not desirable, attempt num "+genCount);
                if (genCount >= 15_000){
                    System.out.println("Failed to generate!");
                    System.out.println("Here is the last generated map");
                    failed = true;
                    break;
                }
                continue;
            }
            setUpScaleCoords();
            joinWalls();
            if (!createTunnels()) {
                continue;
            }
            break;
        }
        if (failed){
            setUpScaleCoords();
            joinWalls();
            createTunnels();
        }
        else{
            System.out.println("Success!");
        }
        String generatedMap = getTiles();
        int maxLength = 28;
        for (int i = 0; i < generatedMap.length(); i += maxLength) {
            int endIndex = Math.min(i + maxLength, generatedMap.length());
            System.out.println(generatedMap.substring(i, endIndex));
        }
    }

    public static void main(String[] args) {
        TetrisGridGenerator tetris = new TetrisGridGenerator();
        tetris.genRandom();
    }

    // Function to transform cells to tiles
    public static String getTiles() {
        // initialize tiles
        int i;
        for (i=0; i<subrows*fullcols; i++) {
            tiles[i] = '_';
        }
        for (i=0; i<subrows*subcols; i++) {
            tileCells[i] = null;
        }
        // set tile cells
        Cell c;
        int x, y;
        int x0, y0;
        for (i = 0; i < rows * cols; i++) {
            c = cells[i];
            for (x0 = 0; x0 < c.final_w; x0++) {
                for (y0 = 0; y0 < c.final_h; y0++) {
                    setTileCell(c.final_x + x0, c.final_y + 1 + y0, c);
                }
            }
        }

        Cell cl, cu;
        for (y = 0; y < subrows; y++) {
            for (x = 0; x < subcols; x++) {
                c = getTileCell(x, y); // cell
                cl = getTileCell(x - 1, y); // left cell
                cu = getTileCell(x, y - 1); // up cell

                if (c != null) {
                    // inside map
                    if ((cl != null && !Objects.equals(c.group, cl.group)) || // at vertical boundary
                            (cu != null && !Objects.equals(c.group, cu.group)) || // at horizontal boundary
                            (cu == null && !c.connect[UP])) { // at top boundary
                        setTile(x, y, '.');
                    }
                } else {
                    // outside map
                    if ((cl != null && (!cl.connect[RIGHT] || getTile(x - 1, y) == '.')) || // at right boundary
                            (cu != null && (!cu.connect[DOWN] || getTile(x, y - 1) == '.'))) { // at bottom boundary
                        setTile(x, y, '.');
                    }
                }

                // at corner connecting two paths
                if (getTile(x - 1, y) == '.' && getTile(x, y - 1) == '.' && getTile(x - 1, y - 1) == '_') {
                    setTile(x, y, '.');
                }
            }
        }

        // extend tunnels (MODIFIED)
        c = cells[cols - 1];
        while (c != null){
            if (c.next[DOWN] == null){
                break;
            }
            c = cells[c.next[DOWN]];
            if (c.topTunnel) {
                y = c.final_y + 1;
                setTile(subcols - 1, y, '.');
                setTile(subcols - 2, y, '.');
            }
        }

        // fill in walls
        for (y = 0; y < subrows; y++) {
            for (x = 0; x < subcols; x++) {
                // any blank tile that shares a vertex with a path tile should be a wall tile
                if (getTile(x, y) != '.' && (getTile(x - 1, y) == '.' || getTile(x, y - 1) == '.' || getTile(x + 1, y) == '.'
                        || getTile(x, y + 1) == '.' || getTile(x - 1, y - 1) == '.' || getTile(x + 1, y - 1) == '.'
                        || getTile(x + 1, y + 1) == '.' || getTile(x - 1, y + 1) == '.')) {
                    setTile(x, y, '|');
                }
            }
        }

        // create the ghost door
        setTile(2,12,'-');

        x = subcols - 2;
        Map<String, Integer> range = null;
        try{
            range = getTopEnergizerRange();
        }
        catch (Exception ex){
            System.out.println("Exception occurred during getTopEnergizerRange()");
        }
        if (range != null) {
            y = getRandomInt(range.get("miny"), range.get("maxy"));
            setTile(x, y, 'o');
        }

        range = null;
        try{
            range = getBotEnergizerRange();
        }
        catch (Exception ex){
            System.out.println("Exception occurred during getBotEnergizerRange()");
        }
        if (range != null) {
            y = getRandomInt(range.get("miny"), range.get("maxy"));
            setTile(x, y, 'o');
        }

        x = subcols - 1;

        for (y = 0; y < subrows; y++) {
            if (getTile(x, y) == '.') {
                eraseUntilIntersection(x, y);
            }
        }

        // erase pellets on starting position
        setTile(1, subrows - 8, ' ');

        // erase pellets around the ghost house
        int j;
        for (i = 0; i < 7; i++) {

            // erase pellets from bottom of the ghost house proceeding down until
            // reaching a pellet tile that isn't surrounded by walls
            // on the left and right
            y = subrows - 14;
            setTile(i, y, ' ');
            j = 1;
            while (getTile(i, y + j) == '.' &&
                    getTile(i - 1, y + j) == '|' &&
                    getTile(i + 1, y + j) == '|') {
                setTile(i, y + j, ' ');
                j++;
            }

            // erase pellets from top of the ghost house proceeding up until
            // reaching a pellet tile that isn't surrounded by walls
            // on the left and right
            y = subrows - 20;
            setTile(i, y, ' ');
            j = 1;
            while (getTile(i, y - j) == '.' &&
                    getTile(i - 1, y - j) == '|' &&
                    getTile(i + 1, y - j) == '|') {
                setTile(i, y - j, ' ');
                j++;
            }
        }

        // erase pellets on the side of the ghost house
        for (i = 0; i < 7; i++) {

            // erase pellets from side of the ghost house proceeding right until
            // reaching a pellet tile that isn't surrounded by walls
            // on the top and bottom.
            x = 6;
            y = subrows - 14 - i;
            setTile(x, y, ' ');
            j = 1;
            while (getTile(x + j, y) == '.' &&
                    getTile(x + j, y - 1) == '|' &&
                    getTile(x + j, y + 1) == '|') {
                setTile(x + j, y, ' ');
                j++;
            }
        }

        // return a tile string (3 empty lines on top and 2 on bottom)

        return "____________________________" +
                "____________________________" +
                "____________________________" +
                new String(tiles) +
                "____________________________" +
                "____________________________";
    }

    // Method to erase pellets in the tunnels
    private static void eraseUntilIntersection(int x, int y) {
        List<Map<String, Integer>> adj;
        while (true) {
            adj = new ArrayList<>();
            if (getTile(x - 1, y) == '.') {
                Map<String, Integer> point = new HashMap<>();
                point.put("x", x - 1);
                point.put("y", y);
                adj.add(point);
            }
            if (getTile(x + 1, y) == '.') {
                Map<String, Integer> point = new HashMap<>();
                point.put("x", x + 1);
                point.put("y", y);
                adj.add(point);
            }
            if (getTile(x, y - 1) == '.') {
                Map<String, Integer> point = new HashMap<>();
                point.put("x", x);
                point.put("y", y - 1);
                adj.add(point);
            }
            if (getTile(x, y + 1) == '.') {
                Map<String, Integer> point = new HashMap<>();
                point.put("x", x);
                point.put("y", y + 1);
                adj.add(point);
            }
            if (adj.size() == 1) {
                setTile(x, y, ' ');
                x = adj.get(0).get("x");
                y = adj.get(0).get("y");
            } else {
                break;
            }
        }
    }

    // Method to get the range of top energizers
    private static Map<String, Integer> getTopEnergizerRange() {
        int miny = 0;
        int maxy = subrows / 2;
        int x = subcols - 2;
        int y;
        for (y = 2; y < maxy; y++) {
            if (getTile(x, y) == '.' && getTile(x, y + 1) == '.') {
                miny = y + 1;
                break;
            }
        }
        maxy = Math.min(maxy, miny + 7);
        for (y = miny + 1; y < maxy; y++) {
            if (getTile(x - 1, y) == '.') {
                maxy = y - 1;
                break;
            }
        }
        Map<String, Integer> range = new HashMap<>();
        range.put("miny", miny);
        range.put("maxy", maxy);
        return range;
    }

    // Method to get the range of bottom energizers
    private static Map<String, Integer> getBotEnergizerRange() {
        int miny = subrows / 2;
        int maxy = 0;
        int x = subcols - 2;
        int y;
        for (y = subrows - 3; y >= miny; y--) {
            if (getTile(x, y) == '.' && getTile(x, y + 1) == '.') {
                maxy = y;
                break;
            }
        }
        miny = Math.max(miny, maxy - 7);
        for (y = maxy - 1; y > miny; y--) {
            if (getTile(x - 1, y) == '.') {
                miny = y + 1;
                break;
            }
        }
        Map<String, Integer> range = new HashMap<>();
        range.put("miny", Math.min(miny, maxy));
        range.put("maxy", Math.max(miny, maxy));
        return range;
    }


    private static void setTile(int x, int y, char v) {
        if (x < 0 || x > subcols - 1 || y < 0 || y > subrows - 1) {
            return;
        }
        x -= 2;
        tiles[midcols + x + y * fullcols] = v;
        tiles[midcols - 1 - x + y * fullcols] = v;
    }

    private static char getTile(int x, int y) {
        if (x < 0 || x > subcols - 1 || y < 0 || y > subrows - 1) {
            return '\0'; // Return null character or any other appropriate default value
        }
        x -= 2;
        return tiles[midcols + x + y * fullcols];
    }

    private static void setTileCell(int x, int y, Cell cell) {
        if (x < 0 || x > subcols - 1 || y < 0 || y > subrows - 1) {
            return;
        }
        x -= 2;
        if (x + y * subcols >= tileCells.length) {
            tileCells[x + y * subcols] = cell;
        }
    }

    private static Cell getTileCell(int x, int y) {
        if (x < 0 || x > subcols - 1 || y < 0 || y > subrows - 1) {
            return null;
        }
        x -= 2;
        if (x + y * subcols >= tileCells.length){
            return tileCells[x + y * subcols];
        }
        return null;
    }
}
