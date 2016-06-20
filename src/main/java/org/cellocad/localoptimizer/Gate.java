/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cellocad.localoptimizer;

import static java.lang.Math.pow;
import java.util.ArrayList;
import java.util.Comparator;

/**
 *
 * @author arashkh
 *
 * Since at the start of the project it was not supposed to be integrated into
 * Cello, We had to define our own Gate class
 */
public class Gate {

    /**
     *
     * Enum to show the type of the gate
     */
    public enum TYPE {

        /**
         *
         * Input is not considered a gate type but was needed for us so included
         */
        INPUT,
        /**
         *
         * Output is not considered a gate type but was needed for us so
         * included
         */
        OUTPUT,
        NOR,
        NOT,
        /**
         *
         * Output_or is not considered a gate type but was needed for us so
         * included
         */
        OUTPUT_OR
    }

    private String name;
    private double high, low, ymax, ymin, k, n;
    private Gate to;
    private ArrayList<Gate> from = new ArrayList();

    public TYPE type;

    /**
     * Constructor
     *
     * @param name name of the gate
     */
    public Gate(String name) {
        this.name = name;
    }

    /**
     * Constructor
     *
     * @param name name of the gate
     * @param high actual output high value of the gate with regards to its
     * inputs
     * @param low actual output low value of the gate with regards to its inputs
     */
    public Gate(String name, double high, double low) {
        this.name = name;
        this.high = high;
        this.low = low;
    }

    /**
     * Constructor
     *
     * @param name name of the gate
     * @param ymax ymax parameter of the gate
     * @param ymin ymin parameter of the gate
     * @param k horizontal offset of the gate
     * @param n slope of the response function
     */
    public Gate(String name, double ymax, double ymin, double k, double n) {
        this.name = name;
        this.ymax = ymax;
        this.ymin = ymin;
        this.k = k;
        this.n = n;
    }

    /**
     * Constructor
     *
     * @param gate gate to be replicated from
     */
    public Gate(Gate gate) {
        this.name = gate.getName();
        this.ymax = gate.getYmax();
        this.ymin = gate.getYmin();
        this.k = gate.getK();
        this.n = gate.getN();
        this.high = gate.getHigh();
        this.low = gate.getLow();
    }

    private double responseCalculator(double x) {
        return ymin + (ymax - ymin) / (1 + Math.pow(x / k, n));
    }

    /**
     * Updates the high and low outputs with respect to the inputs
     *
     */
    public void updateOutputs() {
        ArrayList<Double> range = getInputs();
        double localHigh = range.get(0), localLow = range.get(1);
        if (null != type) {
            switch (type) {
                case INPUT:
                    return;
                case NOR:
                case NOT:
                    this.high = responseCalculator(localLow);
                    this.low = responseCalculator(localHigh);
                    break;
                case OUTPUT_OR:
                case OUTPUT:
                    this.high = localHigh;
                    this.low = localLow;
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Gets high and low inputs taking care of the two-input gates as well
     *
     * @return ArrayList of high and low inputs
     */
    public ArrayList<Double> getInputs() {
        ArrayList<Double> result = new ArrayList();
        ArrayList<Double> highs = new ArrayList();
        ArrayList<Double> lows = new ArrayList();
        double localHigh = 0, localLow = 0;
        if (!from.isEmpty()) {
            for (Gate g : from) {
                highs.add(g.getHigh());
                lows.add(g.getLow());
            }
            if (highs.size() == 1) {
                localHigh = highs.get(0);
            } else {
                localHigh = Math.min(highs.get(0) + lows.get(1), highs.get(1) + lows.get(0));
            }
        }
        for (Double d : lows) {
            localLow += d;
        }
        result.add(localHigh);
        result.add(localLow);
        return result;
    }

    /**
     * From getter
     *
     * @return the gate previous to this gate
     */
    public ArrayList<Gate> getFrom() {
        return from;
    }

    /**
     * Name getter
     *
     * @return name of this gate
     */
    public String getName() {
        return name;
    }

    /**
     * Name setter
     *
     * @param name name to be assigned
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * High value getter
     *
     * @return High value of this gate
     */
    public double getHigh() {
        return high;
    }

    /**
     * High value setter
     *
     * @param high High value to be assigned
     */
    public void setHigh(double high) {
        this.high = high;
    }

    /**
     * Low value getter
     *
     * @return Low value of this gate
     */
    public double getLow() {
        return low;
    }

    /**
     * Low value setter
     *
     * @param low Low value to be assigned
     */
    public void setLow(double low) {
        this.low = low;
    }

    /**
     * Ymax value getter
     *
     * @return Ymax value of this gate
     */
    public double getYmax() {
        return ymax;
    }

    /**
     * Ymax value setter
     *
     * @param ymax Ymax value to be assigned
     */
    public void setYmax(double ymax) {
        this.ymax = ymax;
    }

    /**
     * Ymin value getter
     *
     * @return Ymin value of this gate
     */
    public double getYmin() {
        return ymin;
    }

    /**
     * Ymin value setter
     *
     * @param ymin Ymin value to be assigned
     */
    public void setYmin(double ymin) {
        this.ymin = ymin;
    }

    /**
     * K value setter
     *
     * @return K value of this gate
     */
    public double getK() {
        return k;
    }

    /**
     * K value setter
     *
     * @param k K value to be assigned
     */
    public void setK(double k) {
        this.k = k;
    }

    /**
     * N value getter
     *
     * @return N value of this gate
     */
    public double getN() {
        return n;
    }

    /**
     * N value setter
     *
     * @param n N value to be assigned
     */
    public void setN(double n) {
        this.n = n;
    }

    /**
     * To getter
     *
     * @return The gate next to this gate
     */
    public Gate getTo() {
        return to;
    }

    /**
     * To setter
     *
     * @param to The gate to be assigned next to this gate
     */
    public void setTo(Gate to) {
        this.to = to;
    }

    /**
     * Off_threshold value getter
     *
     * @return Off_threshold of this gate
     */
    public double getOff_threshold() {
        double y = 0.5 * this.ymax;
        return this.k * pow((this.ymax - y) / (y - this.ymin), 1 / this.n);
    }

    /**
     * On_threshold value getter
     *
     * @return On_threshold of this gate
     */
    public double getOn_threshold() {
        double y = 2 * this.ymin;
        return this.k * pow((this.ymax - y) / (y - this.ymin), 1 / this.n);
    }

    /**
     * Dynamic range getter
     *
     * @return Dynamic range of this gate
     */
    public double getDyn_range() {
        return this.ymax / this.ymin;
    }

    /**
     * toString method for this gate
     *
     * @return String representation of this gate
     */
    @Override
    public String toString() {
        String fromString = "";
        for (Gate g : this.from) {
            fromString += g.getName() + ",\t";
        }
        String toString = "";
        if (to != null) {
            toString = to.getName();
        }
        //return "Gate{" + "name=" + name + ", high=" + high + ", low=" + low + ", ymax=" + ymax + ", ymin=" + ymin + ", k=" + k + ", n=" + n + ", type=" + type + "\n, to=" + toString + "\n, from=" + fromString + '}';
        return "name:" + name + ", high:" + high + ", low:" + low + ", ymax:" + ymax + ", ymin:" + ymin + ", k:" + k + ", n:" + n;
    }

    //Comparison b/w the gates
    enum GateComparator implements Comparator<Gate> {
        DYN_SORT {
            @Override
            public int compare(Gate o1, Gate o2) {
                if (o1.getDyn_range() < o2.getDyn_range()) {
                    return -1;
                }
                if (o1.getDyn_range() > o2.getDyn_range()) {
                    return 1;
                }
                return 0;
            }
        },
        N_SORT {
            @Override
            public int compare(Gate o1, Gate o2) {
                if (o1.getN() < o2.getN()) {
                    return -1;
                }
                if (o1.getN() > o2.getN()) {
                    return 1;
                }
                return 0;
            }

        }
    }

}
