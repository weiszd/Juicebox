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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author jrobinso
 * @since Aug 12, 2010
 */
public class Matrix2 {

    private final int chr1;
    private final int chr2;
    private List<MatrixZoomData2> bpZoomData;
    private List<MatrixZoomData2> fragZoomData;

    /**
     * Constructor for creating a matrix from precomputed data.
     *
     * @param chr1
     * @param chr2
     * @param zoomDataList
     */
    public Matrix2(int chr1, int chr2, List<MatrixZoomData2> zoomDataList) {
        this.chr1 = chr1;
        this.chr2 = chr2;
        initZoomDataMap(zoomDataList);
    }

    public static String generateKey(int chr1, int chr2) {
        return "" + chr1 + "_" + chr2;
    }

    public String getKey() {
        return generateKey(chr1, chr2);
    }

    private void initZoomDataMap(List<MatrixZoomData2> zoomDataList) {

        bpZoomData = new ArrayList<MatrixZoomData2>();
        fragZoomData = new ArrayList<MatrixZoomData2>();
        for (MatrixZoomData2 zd : zoomDataList) {
            if (zd.getZoom().getUnit() == HiCZoom2.Unit.BP) {
                bpZoomData.add(zd);
            } else {
                fragZoomData.add(zd);
            }

            // Zooms should be sorted, but in case they are not...
            Comparator<MatrixZoomData2> comp = new Comparator<MatrixZoomData2>() {
                @Override
                public int compare(MatrixZoomData2 o1, MatrixZoomData2 o2) {
                    return o2.getBinSize() - o1.getBinSize();
                }
            };
            Collections.sort(bpZoomData, comp);
            Collections.sort(fragZoomData, comp);
        }

    }

    public MatrixZoomData2 getFirstZoomData(HiCZoom2.Unit unit) {
        if (unit == HiCZoom2.Unit.BP) {
            return bpZoomData != null ? bpZoomData.get(0) : null;
        } else {
            return fragZoomData != null ? fragZoomData.get(0) : null;
        }

    }

    public MatrixZoomData2 getFirstPearsonZoomData(HiCZoom2.Unit unit) {
        if (unit == HiCZoom2.Unit.BP) {
            return bpZoomData != null ? bpZoomData.get(2) : null;
        } else {
            return fragZoomData != null ? fragZoomData.get(2) : null;
        }

    }

    public MatrixZoomData2 getZoomData(HiCZoom2 zoom) {
        List<MatrixZoomData2> zdList = (zoom.getUnit() == HiCZoom2.Unit.BP) ? bpZoomData : fragZoomData;
        //linear search for bin size, the lists are not large
        for (MatrixZoomData2 zd : zdList) {
            if (zd.getBinSize() == zoom.getBinSize()) {
                return zd;
            }
        }

        return null;
    }

    public int getNumberOfZooms(HiCZoom2.Unit unit) {
        return (unit == HiCZoom2.Unit.BP) ? bpZoomData.size() : fragZoomData.size();
    }

    public boolean isIntra() {
        return chr1 == chr2;
    }
}
