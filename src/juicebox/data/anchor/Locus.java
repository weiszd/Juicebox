/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Broad Institute, Aiden Lab
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package juicebox.data.anchor;

import juicebox.data.feature.Feature;

/**
 * Created by muhammadsaadshamim on 8/3/16.
 */
public class Locus extends Feature implements Comparable<Locus> {

    private final int chrIndex;
    private int x1;
    private int x2;

    /**
     * Inititalize anchor given parameters (e.g. from BED file)
     *
     * @param chrIndex
     * @param x1
     * @param x2
     */
    public Locus(int chrIndex, int x1, int x2) {
        this.chrIndex = chrIndex;
        if (x1 <= x2) {
            // x1 < x2
            this.x1 = x1;
            this.x2 = x2;
        } else {
            // x2 < x1 shouldn't ever happen, but just in case
            System.err.println("Improperly formatted Motif file");
            //this.x1 = x2;
            //this.x2 = x1;
        }
    }

    @Override
    public String getKey() {
        return "" + chrIndex;
    }

    /**
     * @return chromosome name
     */
    public int getChr() {
        return chrIndex;
    }

    /**
     * @return start point
     */
    public int getX1() {
        return x1;
    }

    /**
     * @param x1
     */
    public void setX1(int x1) {
        this.x1 = x1;
    }

    /**
     * @return end point
     */
    public int getX2() {
        return x2;
    }

    /**
     * @param x2
     */
    public void setX2(int x2) {
        this.x2 = x2;
    }

    /**
     * @return width of this anchor
     */
    public int getWidth() {
        return x2 - x1;
    }

    /**
     * @param anchor
     * @return true if this is strictly left of given anchor
     */
    public boolean isStrictlyToTheLeftOf(Locus anchor) {
        return x2 < anchor.x1;
    }

    /**
     * @param anchor
     * @return true if this is strictly right of given anchor
     */
    public boolean isStrictlyToTheRightOf(Locus anchor) {
        return anchor.x2 < x1;
    }


    /**
     * Expand this anchor (symmetrically) by the width given
     *
     * @param width
     */
    public void widenMarginsBy(int width) {
        x1 = x1 - width / 2;
        x2 = x2 + width / 2;
    }

    /**
     * Expand this anchor (symmetrically) by the width given
     *
     * @param width
     */
    public void widenToSetWidth(int width) {
        int midPt = (int) (x1 / 2. + x2 / 2.);
        x1 = midPt - width / 2;
        x2 = midPt + width / 2;
    }

    /**
     * @param x
     * @return true if x is within bounds of anchor
     */
    private boolean contains(int x) {
        return x >= x1 && x <= x2;
    }


    /**
     * @param anchor
     * @return true if given anchor overlaps at either edge with this anchor
     */
    public boolean hasOverlapWith(Locus anchor) {
        return chrIndex == anchor.chrIndex && (this.contains(anchor.x1) || this.contains(anchor.x2));
    }

    @Override
    public int hashCode() {
        return x2 * chrIndex + x1;
    }


    @Override
    public Feature deepClone() {
        return null;
    }

    @Override
    public int compareTo(Locus o) {
        if (chrIndex == o.chrIndex) {
            if (x1 == o.x1) {
                return (new Integer(x2)).compareTo(o.x2);
            }
            return (new Integer(x1)).compareTo(o.x1);
        }
        return (new Integer(chrIndex)).compareTo(o.chrIndex);
    }

    @Override
    public String toString() {
        return chrIndex + "\t" + x1 + "\t" + x2;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Locus) {
            Locus o = (Locus) obj;
            return chrIndex == o.chrIndex && x1 == o.x1 && x2 == o.x2;
        }
        return false;
    }
}
