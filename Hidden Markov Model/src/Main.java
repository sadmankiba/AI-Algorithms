import java.util.*;

class Cell {
    static int N = 3;
    public int row;
    public int col;
    Cell(int row, int col) {
        this.row = row;
        this.col = col;
    }
    int getIdx() {
        return row * N + col;
    }
    static Cell makeCell(int idx) {
        return new Cell(idx / N, idx % N);
    }
    static boolean isValid(Cell c) {
        if (c.row >= 0 && c.row < N && c.col >= 0 && c.col < N)
            return true;
        return false;
    }
    boolean isValid() {
        if (this.row >= 0 && this.row < N && this.col >= 0 && this.col < N)
            return true;
        return false;
    }
}

public class Main {
    static int N = 3;
    static double AXIS_MOV_PROB = 0.999;  // < 1
    static double SENSOR_RELIABILITY = 0.95;
    static ArrayList<Double> probTable = new ArrayList<>();
    static int ghostPos;
    enum Color {
        RED, ORANGE, GREEN
    }

    private static ArrayList<Integer> getAxesMov (int cellNo) {
        ArrayList<Integer> am = new ArrayList<>();
        Cell c = Cell.makeCell(cellNo);
        Cell e = new Cell(c.row, c.col + 1);
        Cell w = new Cell(c.row, c.col - 1);
        Cell n = new Cell(c.row - 1, c.col);
        Cell s = new Cell(c.row + 1 , c.col);
        if(e.isValid())
            am.add(e.getIdx());
        if(w.isValid())
            am.add(w.getIdx());
        if(n.isValid())
            am.add(n.getIdx());
        if(s.isValid())
            am.add(s.getIdx());
        return am;
    }
    private static ArrayList<Integer> getRandMov (int cellNo) {
        ArrayList<Integer> rm = new ArrayList<>();
        Cell c = Cell.makeCell(cellNo);
        Cell ne = new Cell(c.row - 1, c.col + 1);
        Cell nw = new Cell(c.row - 1, c.col - 1);
        Cell se = new Cell(c.row + 1, c.col + 1);
        Cell sw = new Cell(c.row + 1 , c.col - 1);
        rm.add(cellNo);
        if(ne.isValid())
            rm.add(ne.getIdx());
        if(nw.isValid())
            rm.add(nw.getIdx());
        if(se.isValid())
            rm.add(se.getIdx());
        if(sw.isValid())
            rm.add(sw.getIdx());
        return rm;
    }
    private static ArrayList<Integer> getOneDistCells (int cellNo) {
        return getAxesMov(cellNo);
    }
    private static ArrayList<Integer> getTwoDistCells (int cellNo) {
        ArrayList<Integer> twc = new ArrayList<>();
        Cell c = Cell.makeCell(cellNo);
        for (int i = 0; i < N*N; i++) {
            Cell ci = Cell.makeCell(i);
            int rd = ci.row - c.row;
            rd = rd < 0? -rd : rd;
            int cd = ci.col - c.col;
            cd = cd < 0? -cd : cd;
            if ((rd + cd) == 2)
                twc.add(i);
        }
        return twc;
    }
    private static ArrayList<Integer> getMoreThanOneDistCells (int cellNo) {
        ArrayList<Integer> mc = new ArrayList<>();
        Cell c = Cell.makeCell(cellNo);
        for (int i = 0; i < N*N; i++) {
            Cell ci = Cell.makeCell(i);
            int rd = ci.row - c.row;
            rd = rd < 0? -rd : rd;
            int cd = ci.col - c.col;
            cd = cd < 0? -cd : cd;
            if ((rd + cd) > 1)
                mc.add(i);
        }
        return mc;
    }
    private static void printGrid(ArrayList<Double> a) {
        for (int i = 0; i < N; i++) {
            for(int j = 0; j < N; j++) {
                System.out.format("%.2f ", a.get(i * N + j));
            }
            System.out.println("");
        }
        System.out.println("");
    }
    private static ArrayList<Double> generateTransitionProb(int cellNo) {
        ArrayList<Double> t= new ArrayList<Double>();
        for (int i = 0; i < N*N; i++)
            t.add(0.0);
        ArrayList<Integer> am = getAxesMov(cellNo);
        ArrayList<Integer> rm = getRandMov(cellNo);
        for (int i = 0; i < am.size(); i++) {
            t.set(am.get(i), AXIS_MOV_PROB / am.size());
        }
        for (int i = 0; i < rm.size(); i++) {
            t.set(rm.get(i), (1 - AXIS_MOV_PROB) / rm.size());
        }
        return t;
    }
    private static ArrayList<Double> generateEmissionProb(int cellNo, Color color) {
        ArrayList<Double> e= new ArrayList<Double>();
        for (int i = 0; i < N*N; i++)
            e.add(0.0);
        switch (color) {
            case RED:
                e.set(cellNo, SENSOR_RELIABILITY / 1.0);
                for (int i = 0; i < e.size(); i++) {
                    if (e.get(i) > 0.0)
                        continue;
                    e.set(i, (1 - SENSOR_RELIABILITY) / (N * N - 1));
                }
                break;
            case ORANGE:
                ArrayList<Integer> twc = getOneDistCells(cellNo);
                for (int i = 0; i < twc.size(); i++)
                    e.set(twc.get(i), SENSOR_RELIABILITY / twc.size());
                for (int i = 0; i < e.size(); i++) {
                    if (e.get(i) > 0.0)
                        continue;
                    e.set(i, (1 - SENSOR_RELIABILITY) / ( N * N - twc.size()));
                }
                break;
            case GREEN:
                ArrayList<Integer> mrc = getMoreThanOneDistCells(cellNo);
                for (int i = 0; i < mrc.size(); i++)
                    e.set(mrc.get(i), SENSOR_RELIABILITY / mrc.size());
                for (int i = 0; i < e.size(); i++) {
                    if (e.get(i) > 0.0)
                        continue;
                    e.set(i, (1 - SENSOR_RELIABILITY) / ( N * N - mrc.size()));
                }
                break;
            default:
                break;
        }
        return e;
    }
    private static int selectOptimalCell() {
        Scanner in = new Scanner(System.in);
        System.out.print("Make a guess: ");
        return in.nextInt();
    }
    private static int calcManh(int a, int b) {
        Cell ca = Cell.makeCell(a);
        Cell cb = Cell.makeCell(b);
        int rd = ca.row - cb.row;
        rd = rd < 0? -rd: rd;
        int cd = ca.col - cb.col;
        cd = cd < 0? -cd: cd;
        return rd + cd;
    }
    private static Color getSensorOutput(int cellNo) {
        int d = calcManh(ghostPos, cellNo);
        switch (d) {
            case 0:
                return Color.RED;
            case 1:
                return Color.ORANGE;
            default:
                return Color.GREEN;
        }
    }
    private static void moveGhost() {
        ArrayList<Double> gm = generateTransitionProb(ghostPos);
        double sum = 0.0;
        Random r = new Random();
        double d = r.nextDouble();
        for (int i = 0; i < N * N; i++) {
            if (d >= sum && d < (sum + gm.get(i))) {
                ghostPos = i;
                break;
            }
            sum += gm.get(i);
        }
    }
    private static void updateTable(Color color, int cellNo) {
        ArrayList<Double> tmpProb = new ArrayList<>();
        for (int j = 0; j < N * N; j++)
            tmpProb.add(0.0);
        for (int i = 0; i < N * N; i++) {
            ArrayList<Double> t = generateTransitionProb(i);
            // System.out.println("i " + i + " " + t);
            for (int j = 0; j < N * N; j++) {
                tmpProb.set(j, tmpProb.get(j) + probTable.get(i) * t.get(j));
            }
        }
        probTable = tmpProb;
        System.out.println("Update with transition");
        printGrid(probTable);
        ArrayList<Double> e = generateEmissionProb(cellNo, color);
        // printGrid(e);
        for (int i = 0; i < N * N; i++) {
            probTable.set(i, e.get(i) * probTable.get(i));
        }
        double sum = 0;
        for (int j = 0; j < probTable.size(); j++) {
            sum += probTable.get(j);
        }
        for (int j = 0; j < probTable.size(); j++) {
            probTable.set(j, probTable.get(j) / sum);
        }
        System.out.println("Update with emission");
        printGrid(probTable);
    }
    private static void runGame() {
        Random r = new Random();
        ghostPos = r.nextInt(N * N);
        for (int i = 0; i < N * N; i++)
            probTable.add(1.0 / (N * N));
        outer:
        while (true) {
            Scanner in = new Scanner(System.in);
            System.out.print("Press button");
            in.next();
            moveGhost();
            // System.out.println("Ghost is now in cell " + ghostPos);
            // System.out.println("Prediction Probability");
            // printGrid(probTable);
            int cellNo = selectOptimalCell();
            Color color = getSensorOutput(cellNo);
            System.out.println(color);
            updateTable(color, cellNo);
            for (int i = 0; i < N * N; i++) {
                if (probTable.get(i) > 0.95) {
                    System.out.println("caught ghost at " + i);
                    break outer;
                }
            }
        }
    }
    public static void main(String[] args) {
        System.out.println("Hello World!");
//        printGrid(generateEmissionProb(4, Color.RED));
//        printGrid(generateEmissionProb(8, Color.RED));
//        printGrid(generateEmissionProb(3, Color.RED));
//        printGrid(generateEmissionProb(1, Color.RED));
        runGame();
//        Random r = new Random();
//        ghostPos = r.nextInt(N * N);
//        for (int i = 0; i < N * N; i++)
//            probTable.add(1.0 / (N * N));
//        printGrid(probTable);
//        updateTable(Color.GREEN, 3);
//        updateTable(Color.ORANGE, 8);
//        updateTable(Color.RED, 4);
//        updateTable(Color.RED, 4);
    }
}
