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

package dumponly.basics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dataset {

    // private boolean caching = true;
    private final Map<String, Matrix> matrices = new HashMap<String, Matrix>(25 * 25);
    private final DatasetReader reader;
    private final LRUCache<String, NormalizationVector> normalizationVectorCache;
    //Chromosome lookup table
    private List<Chromosome> chromosomes;
    private List<HiCZoom> bpZooms;
    private List<HiCZoom> fragZooms;
    private List<Integer> bpZoomResolutions;
    private List<NormalizationType> normalizationTypes;

    public Dataset(DatasetReader reader) {
        this.reader = reader;
        normalizationVectorCache = new LRUCache<String, NormalizationVector>(20);
        normalizationTypes = new ArrayList<NormalizationType>();
    }


    public Matrix getMatrix(Chromosome chr1, Chromosome chr2) {

        // order is arbitrary, convention is lower # chr first
        int t1 = Math.min(chr1.getIndex(), chr2.getIndex());
        int t2 = Math.max(chr1.getIndex(), chr2.getIndex());

        String key = Matrix.generateKey(t1, t2);
        Matrix m = matrices.get(key);

        if (m == null && reader != null) {
            try {
                m = reader.readMatrix(key);
                matrices.put(key, m);
            } catch (IOException e) {
                System.err.println("Error fetching matrix for: " + chr1.getName() + "-" + chr2.getName());
            }
        }

        return m;

    }

    public void addNormalizationType(NormalizationType type) {
        if (!normalizationTypes.contains(type)) normalizationTypes.add(type);
    }

    public int getNumberZooms(HiCZoom.Unit unit) {
        return unit == HiCZoom.Unit.BP ? bpZooms.size() : fragZooms.size();
    }


    public HiCZoom getZoom(HiCZoom.Unit unit, int index) {
        return unit == HiCZoom.Unit.BP ? bpZooms.get(index) : fragZooms.get(index);
    }


    public List<Chromosome> getChromosomes() {
        return chromosomes;
    }


    public void setChromosomes(List<Chromosome> chromosomes) {
        this.chromosomes = chromosomes;
    }

    public void setBpZooms(int[] bpBinSizes) {

        bpZoomResolutions = intArrayToList(bpBinSizes);

        bpZooms = new ArrayList<HiCZoom>(bpBinSizes.length);
        for (int bpBinSize : bpZoomResolutions) {
            bpZooms.add(new HiCZoom(HiCZoom.Unit.BP, bpBinSize));
        }
    }

    private List<Integer> intArrayToList(int[] values) {
        List<Integer> list = new ArrayList<Integer>(values.length);
        for (int value : values) {
            list.add(value);
        }
        return list;
    }


    public NormalizationVector getNormalizationVector(int chrIdx, HiCZoom zoom, NormalizationType type) {

        String key = NormalizationVector.getKey(type, chrIdx, zoom.getUnit().toString(), zoom.getBinSize());
        if (type == NormalizationType.NONE) {
            return null;
        } else if (!normalizationVectorCache.containsKey(key)) {

            try {
                NormalizationVector nv = reader.readNormalizationVector(type, chrIdx, zoom.getUnit(), zoom.getBinSize());
                normalizationVectorCache.put(key, nv);
            } catch (IOException e) {
                normalizationVectorCache.put(key, null);
            }
        }

        return normalizationVectorCache.get(key);

    }
}
