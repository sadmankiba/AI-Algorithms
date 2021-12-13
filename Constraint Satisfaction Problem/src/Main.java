import java.io.*;
import java.util.*;
import java.text.*;
import java.util.stream.IntStream;

public class Main {
    static int n;
    static int[][] problem;
    // 0  = random, 1 = SDF, 2 = SDF + DV(brelaz), 3 = domddeg
    static int varSelection = 2;
    // 0 = FC, 1 = MAC
    static int inferenceSelection = 1;
    static int numNodes = 0;
    static int numFails = 0;
    static int firstPrint = 0;


    // Problem is approached with a n x n x n matrix that has the probable values of each cell.
    // data structure is Arraylist of Arraylist of integer.
    // Outer arraylist = aggregation of (rows sequentially top to bottom)
    // each inner arraylist is probable value. Initially, that arraylist has 1 to n.
    // Eliminating a value for a cell means removing that value from the arraylist for that cell.
    // Problem is solved when each cell has exactly 1 non-zero value in it's arraylist.

    private static void printCSPMatrix(ArrayList<ArrayList<Integer>> cm) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                System.out.format("%30s", cm.get(i*n + j));
            }
            System.out.println();
        }
    }

    public static ArrayList<ArrayList<Integer>> cloneList(ArrayList<ArrayList<Integer>> al) {
        ArrayList<ArrayList<Integer>> cal = new ArrayList<ArrayList<Integer>>(al.size());
        for (ArrayList<Integer> val : al) cal.add((ArrayList<Integer>) val.clone());
        return cal;
    }

    private static void readFile(String file) {

        System.out.println("readFile(" + file + ")");
        int lineCount = 0;

        try {
            BufferedReader inr = new BufferedReader(new FileReader(file));
            String str;
            while ((str = inr.readLine()) != null && str.length() > 0) {
                for (char c: "N=start;|[],".toCharArray()) {
                    str = str.replace(c, ' ');
                }
                str = str.trim();
                if (str.length() == 0) continue;
                // System.out.println(str);

                if (lineCount == 0) {
                    n = Integer.parseInt(str); // number of rows
                    problem = new int[n][n];

                } else {
                    // the problem matrix
                    String row[] = str.split("\\s+");
                    // System.out.println(row.length);
                    for (int i = 0; i < n; i++) {
                        String s = row[i];
                        problem[lineCount - 1][i] = Integer.parseInt(s);
                    }
                }

                lineCount++;
            } // end while

            inr.close();
            for (int row[]: problem) {
                for (int i: row) {
                    System.out.print(i + " ");
                }
                System.out.println();
            }

        } catch (IOException e) {
            System.out.println(e);
        }
    } // end readFile

    private static boolean isSolved(ArrayList<ArrayList<Integer>> cm) {
        boolean solved = true;
        for (int i = 0; i  < cm.size(); i++) {
            if (cm.get(i).size() != 1) {
                solved = false;
                break;
            }
        }
        return solved;
    }

    /**
     * @return number of constraints(degree) of a variable(node) with other unassigned variables(nodes)
     */
    private static int calculateDegWithUnassigned(ArrayList<ArrayList<Integer>> cm, int ind) {
        int r = ind / n;
        int c = ind % n;
        int d = 0;
        for(int i = 0; i < n; i++) {
            if (ind != (r * n + i) && cm.get(r * n + i).size() > 1) d += 1;
            if (ind != (i * n + c) && cm.get(i * n + c).size() > 1) d += 1;
        }
        return d;
    }

    /**
     * @return index of an unassigned node at random
     */
    private static int randomVarSelection(ArrayList<ArrayList<Integer>> cm, ArrayList<Boolean> vis) {
        Random rd = new Random();
        int cd = rd.nextInt(cm.size());
        while (vis.get(cd) == true || cm.get(cd).size() == 1) {
            cd = rd.nextInt(cm.size());
        }
        return cd;
    }

    /**
     * @return index of unassigned node with smallest domain
     */
    private static int sdf(ArrayList<ArrayList<Integer>> cm, ArrayList<Boolean> vis) {
//        int sd = vis.lastIndexOf(false);
//        for (int i = 0; i < cm.size(); i++) {
//            if (cm.get(i).size() < cm.get(sd).size() && vis.get(i) == false) {
//                sd = i;
//            }
//        }
//        return sd;
        ArrayList<Integer> sd = new ArrayList<Integer>(); // list of indices of nodes with smallest domain

        for (int i = 0; i < cm.size(); i++) {
            if (cm.get(i).size() > 1
                    && !(vis.get(i))
                    && (sd.size() == 0 || cm.get(i).size() < cm.get(sd.get(0)).size())) {
                sd = new ArrayList<Integer>();
                sd.add(i);
            }
            else if (cm.get(i).size() > 1
                        && !(vis.get(i))
                        && (sd.size() == 0 || cm.get(i).size() == cm.get(sd.get(0)).size() && (!sd.contains(Integer.valueOf(i))))) {
                sd.add(i);
            }
        }
        Random r = new Random();
//        System.out.println(sd);
        return sd.get(r.nextInt(sd.size()));
    }

    /**
     * @return index of unassigned node with smallest domain and ties broken with maximum forward degree
     */
    private static int brelaz(ArrayList<ArrayList<Integer>> cm, ArrayList<Boolean> vis) {
        ArrayList<Integer> sd = new ArrayList<Integer>(); // list of indices of nodes with smallest domain

        for (int i = 0; i < cm.size(); i++) {
            if (cm.get(i).size() > 1
                    && !(vis.get(i))
                    && (sd.size() == 0 || cm.get(i).size() < cm.get(sd.get(0)).size())) {
                sd = new ArrayList<Integer>();
                sd.add(i);
            }
            else if (cm.get(i).size() > 1
                        && !(vis.get(i))
                        && (sd.size() == 0 || cm.get(i).size() == cm.get(sd.get(0)).size())) {
                sd.add(i);
            }
        }
//        System.out.println(sd);

        ArrayList<Integer> sdmd = new ArrayList<Integer>(); // list of indices of nodes with smallest domain and max degree with unassigned variables
        sdmd.add(sd.get(0));
        for (int i = 0; i < sd.size(); i++) {
            if (calculateDegWithUnassigned(cm, sd.get(i)) > calculateDegWithUnassigned(cm, sdmd.get(0))) {
                sdmd = new ArrayList<Integer>();
                sdmd.add(sd.get(i));
            }
            else if (calculateDegWithUnassigned(cm, sd.get(i)) == calculateDegWithUnassigned(cm, sdmd.get(0))) {
                sdmd.add(sd.get(i));
            }
        }
//        System.out.println(sdmd);
        Random r  = new Random();
        int res = sdmd.get(r.nextInt(sdmd.size()));
        return res;
    }

    /**
     * @return index of unassigned node that minimizes the ratio of domain size to forward degree (unassigned)
     */
    private static int domddeg(ArrayList<ArrayList<Integer>> cm, ArrayList<Boolean> vis) {
        ArrayList<Integer> mr = new ArrayList<Integer>();
        for (int i = 0; i < cm.size(); i++) {
            if (cm.get(i).size() > 1 && !(vis.get(i))) {
                if (mr.size() == 0) {
                    mr.add(i);
                }
                else {
                    double rdi = cm.get(i).size() * 1.0 / calculateDegWithUnassigned(cm, i);
                    double rds = cm.get(mr.get(0)).size() * 1.0 / calculateDegWithUnassigned(cm, mr.get(0));

                    if (rdi < rds) {
                        mr.clear();
                        mr.add(i);
                    }
                    else if (Double.valueOf(rdi).equals(Double.valueOf(rds))) {
                        mr.add(i);
                    }

                }
            }
//            System.out.println(mr);
        }
        Random r = new Random();
        return mr.get(r.nextInt(mr.size()));
    }

    private static int selectUnassignedVariable(ArrayList<ArrayList<Integer>> cm, ArrayList<Boolean> vis) {
        switch (varSelection){
            case 0:
                return randomVarSelection(cm, vis);
            case 1:
                return sdf(cm, vis);
            case 2:
                return brelaz(cm, vis);
            case 3:
                return domddeg(cm, vis);
            default:
                break;
        }
        return 5;
    }

    /**
     *
     * @param cm
     * @param ind current node in backtrack search. assuming that it has exactly one value
     */
    private static void fc(ArrayList<ArrayList<Integer>> cm, int ind) {
        int r = ind / n;
        int c = ind % n;
        for(int i = 0; i < n; i++) {
            if (ind != (r * n + i)) cm.get(r * n + i).remove(Integer.valueOf(cm.get(ind).get(0)));
            if (ind != (i * n + c)) cm.get(i * n + c).remove(Integer.valueOf(cm.get(ind).get(0)));
        }
    }

    private static boolean macRevise(ArrayList<ArrayList<Integer>> cm, int indi, int indj) {
//        revised ← false
//        for each x in Di do
//            if no value y in Dj allows (x ,y) to satisfy the constraint between Xi and Xj then
        //        delete x from Di
        //        revised ← true
//        return revised
        boolean revised = false;
        if (cm.get(indj).size() == 1) {
            for (int i = 0; i < cm.get(indi).size(); i++) {
                if (cm.get(indj).get(0) == cm.get(indi).get(i)) {
                    cm.get(indi).remove(i);
                    revised = true;

                }
            }
        }
        return revised;
    }

    private static void mac(ArrayList<ArrayList<Integer>> cm, int ind) {
//        inputs: csp, a binary CSP with components (X, D, C)
//        local variables: queue, a queue of arcs, initially all the arcs in csp
//
//        while queue is not empty do
//            (Xi, Xj)← REMOVE-FIRST(queue)
//        if REVISE(csp, Xi, Xj) then
    //        if size of Di = 0 then return false
    //        for each Xk in Xi.NEIGHBORS - {Xj} do
    //            add (Xk, Xi) to queue
//        return true

        Queue<ArrayList<Integer>> q = new LinkedList<>();

        int r = ind / n;
        int c = ind % n;
        for(int i = 0; i < n; i++) {
            if (ind != (r * n + i) && cm.get(r * n + i).size() > 1) {
                ArrayList<Integer> ta = new ArrayList<>();
                ta.add(r * n + i); ta.add(ind);
                q.add(ta);
            }
            if (ind != (i * n + c) && cm.get(i * n + c).size() > 1) {
                ArrayList<Integer> ta = new ArrayList<>();
                ta.add(i * n + c); ta.add(ind);
                q.add(ta);
            }
        }

        while (! q.isEmpty()) {
//            System.out.println(q);
            ArrayList<Integer> a = q.remove();
            int ri = a.get(0), rj = a.get(1);
//            System.out.println("dom rem " + calculateDomainRemaining(cm));
            if (macRevise(cm, ri, rj)) {
                // System.out.println("dom rem " + calculateDomainRemaining(cm));
                // printCSPMatrix(cm);
                if (isFailure(cm)) return;
                int rir = ri / n;
                int ric = ri % n;
                for(int i = 0; i < n; i++) {
                    if (ri != (rir * n + i) && rj != (rir * n + i)) {
                        ArrayList<Integer> ta = new ArrayList<>();
                        ta.add(rir * n + i); ta.add(ri);
                        q.add(ta);
                    }
                    if (ri != (i * n + ric) && rj != (i * n + ric)) {
                        ArrayList<Integer> ta = new ArrayList<>();
                        ta.add(i * n + ric); ta.add(ri);
                        q.add(ta);
                    }
                }
            }
        }
    }

    /**
     *
     * @param cm overwritten by an inference; either FC or MAC
     */
    private static void inference(ArrayList<ArrayList<Integer>> cm, int nextInd) {
        switch (inferenceSelection) {
            case 0:
                fc(cm, nextInd);
                break;
            case 1:
                mac(cm, nextInd);
                break;
            default:
                break;
        }
    }

    private static boolean isFailure(ArrayList<ArrayList<Integer>> cm) {
        for (int i = 0; i < cm.size(); i++) {
            if (cm.get(i).isEmpty()) return true;
        }
        return false;
    }

    private static int calculateDomainRemaining(ArrayList<ArrayList<Integer>> cm) {
        int sum = 0;
        for (int i = 0; i < cm.size(); i++) {
            sum += cm.get(i).size();
        }
        return sum;
    }

    private static ArrayList<Integer> neighbours(int ind) {
        int r = ind / n;
        int c = ind % n;
        ArrayList<Integer> neigh = new ArrayList<>();
        for(int i = 0; i < n; i++) {
            if (ind != (r * n + i)) neigh.add(r * n + i);
            if (ind != (i * n + c)) neigh.add(i * n + c);
        }
        return neigh;
    }

    /**
     *
     * @param cm
     * @param ind
     * @return An arraylist which is the domain ordered by least-constraint value heuristic
     */
    private static ArrayList<Integer> optimalDomainOrder(ArrayList<ArrayList<Integer>> cm, int ind) {
        ArrayList<Integer> neigh = neighbours(ind);
        int[] numRuledOutChoices = new int [cm.get(ind).size()];

        for (int i = 0; i < numRuledOutChoices.length; i++)
            numRuledOutChoices[i] = 0;

        for (int i = 0; i < cm.get(ind).size(); i++) {
            for (int jn : neigh) {
                if (cm.get(jn).contains(cm.get(ind).get(i))) numRuledOutChoices[i] += 1;
            }
        }

//        for (int i = 0; i < numRuledOutChoices.length; i++)
//            System.out.print(numRuledOutChoices[i] + " ");
//        System.out.println("");

        int[] sortedIndices = IntStream.range(0, numRuledOutChoices.length)
                .boxed().sorted((i, j) -> (numRuledOutChoices[i] - numRuledOutChoices[j]))
                .mapToInt(ele -> ele).toArray();

//        for (int i = 0; i < sortedIndices.length; i++)
//            System.out.print(sortedIndices[i] + " ");
//        System.out.println("");

        ArrayList<Integer> od = new ArrayList<>();
        for (int i = 0; i < sortedIndices.length; i++) {
            od.add(cm.get(ind).get(sortedIndices[i]));
        }
        return od;
    }
    private static ArrayList<ArrayList<Integer>> backtrack(ArrayList<ArrayList<Integer>> cm, ArrayList<Boolean> vis) {

        if(isSolved(cm)) return cm;
        int nextInd = selectUnassignedVariable(cm, vis);

//        System.out.println("numNodes " + numNodes + " nextInd " + nextInd + " dom " + cm.get(nextInd) + " dom rem " + calculateDomainRemaining(cm));
        ArrayList<Boolean> cv = (ArrayList<Boolean>) vis.clone();
        cv.set(nextInd, true);
//        System.out.println("nextInd " + nextInd);
//        printCSPMatrix(cm);
        ArrayList<Integer> od = optimalDomainOrder(cm, nextInd);
        for(int val: od) {
            numNodes += 1;
            ArrayList<ArrayList<Integer>> cl = cloneList(cm);

            //assign value
            cl.get(nextInd).clear();
            cl.get(nextInd).add(val);

            inference(cl, nextInd);
//            System.out.println("nextInd " + nextInd + " val " + val);
//            printCSPMatrix(cl);

            if (! isFailure(cl)) {
                ArrayList<ArrayList<Integer>> res = backtrack(cl, cv);
                if (! isFailure(res)) return res;
            }
            else numFails += 1;
        }
        // a failure
        ArrayList<ArrayList<Integer>> fl = cloneList(cm);
        fl.get(0).clear();
        return fl;
    }

    public static void main(String[] args) throws Exception {
        readFile("d-10-01.txt.txt");
        // 0  = random, 1 = SDF, 2 = SDF + DV(brelaz), 3 = domddeg
        varSelection = 3;
        // 0 = FC, 1 = MAC
        inferenceSelection = 0;
        ArrayList<ArrayList<Integer>> cm = new ArrayList<ArrayList<Integer>>();

        // initialize csp matrix and node consistency
        for (int i = 0; i < n; i++) {
            for(int j = 0; j < n; j++) {
                ArrayList<Integer> temp = new ArrayList<Integer>();
                if (problem[i][j] != 0) {
                    temp.add(problem[i][j]);
                }
                else {
                    for (int k = 0; k < n; k++) {
                        temp.add(k+1);
                    }
                }
                cm.add(temp);
            }
        }

        printCSPMatrix(cm);

        // initial arc consistency
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (cm.get(i * n + j).size() == 1) {
                    fc(cm, i * n + j);
                }
            }
        }
        System.out.println("");
        printCSPMatrix(cm);
        System.out.println("\ninitial dom rem " + calculateDomainRemaining(cm));

        ArrayList<ArrayList<Integer>> cal;
        cal = cloneList(cm);

//        System.out.println("\n\nfc");
//        for (int i = 0; i < n; i++) {
//            for(int j = 0; j < n; j++) {
//                if(cm.get(i * n + j).size() == 1) fc(cm, i * n + j);
//            }
//        }
        //initialize visited nodes list as all false.
        ArrayList<Boolean> vis= new ArrayList<Boolean>();
        for (int i = 0; i < cm.size(); i++) {
            vis.add(false);
        }
//        System.out.println(vis);

//        for (int i = 0; i < n; i++) {
//            for (int j = 0; j < n; j++) {
//                System.out.print(calculateDegWithUnassigned(cm, i * n + j) + " ");
//            }
//            System.out.println();
//        }
//
//        for (int i = 0; i < 5; i++) {
//            ArrayList<Boolean> cv = (ArrayList<Boolean>) vis.clone();
//            for (int j = 0; j < cv.size(); j++) {
//                Random r = new Random();
//                if(r.nextDouble() < 0.5) {
//                    cv.set(j, true);
//                }
//            }
//            System.out.println(cv);
//            System.out.println(domddeg(cm, cv));
//        }

//        for (int i = 0; i < 5; i++) {
//            int ri;
//            do {
//                Random r = new Random();
//                ri = r.nextInt(cm.size());
//            } while(cm.get(ri).size() == 1);
//            System.out.println("selected idx " + ri);
//            System.out.println(optimalDomainOrder(cm, ri));
////            printCSPMatrix(cm);
//        }
        cal = backtrack(cm, vis);
        System.out.println("\n\nFinished");
        printCSPMatrix(cal);
        System.out.println("numNodes " + numNodes + " numFails " + numFails);
    }
}
