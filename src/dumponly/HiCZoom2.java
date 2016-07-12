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

package dumponly;


/**
 * @author jrobinso
 *         Date: 12/17/12
 *         Time: 9:16 AM
 */
public class HiCZoom2 {

    private final Unit unit;
    private final int binSize;

    public HiCZoom2(Unit unit, int binSize) {
        this.unit = unit;
        this.binSize = binSize;
    }

    public HiCZoom2 clone() {
        return new HiCZoom2(unit, binSize);
    }

    public Unit getUnit() {
        return unit;
    }

    public int getBinSize() {
        return binSize;
    }

    public String getKey() {
        return unit.toString() + "_" + binSize;
    }

    public String toString() {
        return getKey();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HiCZoom2 hiCZoom2 = (HiCZoom2) o;

        return (binSize == hiCZoom2.binSize) && (unit == hiCZoom2.unit);
    }

    @Override
    public int hashCode() {
        int result = unit.hashCode();
        result = 31 * result + binSize;
        return result;
    }

    public enum Unit {BP, FRAG}
}
