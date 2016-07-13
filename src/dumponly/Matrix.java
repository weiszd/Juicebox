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

public class Matrix {

    private List<MatrixZoomData> bpZoomData;

    /**
     * Constructor for creating a matrix from precomputed data.
     *
     * @param zoomDataList
     */
    public Matrix(List<MatrixZoomData> zoomDataList) {
        initZoomDataMap(zoomDataList);
    }

    public static String generateKey(int chr1, int chr2) {
        return "" + chr1 + "_" + chr2;
    }

    private void initZoomDataMap(List<MatrixZoomData> zoomDataList) {

        bpZoomData = new ArrayList<MatrixZoomData>();
        for (MatrixZoomData zd : zoomDataList) {
            if (zd.getZoom().getUnit() == HiCZoom.Unit.BP) {
                bpZoomData.add(zd);
            }

            // Zooms should be sorted, but in case they are not...
            Comparator<MatrixZoomData> comp = new Comparator<MatrixZoomData>() {
                @Override
                public int compare(MatrixZoomData o1, MatrixZoomData o2) {
                    return o2.getBinSize() - o1.getBinSize();
                }
            };
            Collections.sort(bpZoomData, comp);
        }

    }

    public MatrixZoomData getZoomData(HiCZoom zoom) {
        List<MatrixZoomData> zdList = bpZoomData;
        //linear search for bin size, the lists are not large
        for (MatrixZoomData zd : zdList) {
            if (zd.getBinSize() == zoom.getBinSize()) {
                return zd;
            }
        }

        return null;
    }
}
