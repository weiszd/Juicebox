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

import com.google.common.primitives.Ints;
import org.apache.log4j.Logger;
import org.broad.igv.feature.Chromosome;
import org.broad.igv.util.collections.LRUCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jrobinso
 * @since Aug 12, 2010
 */
public class Dataset22 {

    private static final Logger log = Logger.getLogger(Dataset22.class);

    // private boolean caching = true;
    private final Map<String, Matrix2> matrices = new HashMap<String, Matrix2>(25 * 25);
    private final DatasetReader22 reader;
    private final LRUCache<String, NormalizationVector2> normalizationVectorCache;
    //Chromosome lookup table
    private List<Chromosome> chromosomes;
    private List<HiCZoom2> bpZooms;
    private List<HiCZoom2> fragZooms;
    private List<Integer> bpZoomResolutions;
    private List<NormalizationType2> normalizationTypes;

    public Dataset22(DatasetReader22 reader) {
        this.reader = reader;
        normalizationVectorCache = new LRUCache<String, NormalizationVector2>(20);
        normalizationTypes = new ArrayList<NormalizationType2>();
    }


    public Matrix2 getMatrix(Chromosome chr1, Chromosome chr2) {

        // order is arbitrary, convention is lower # chr first
        int t1 = Math.min(chr1.getIndex(), chr2.getIndex());
        int t2 = Math.max(chr1.getIndex(), chr2.getIndex());

        String key = Matrix2.generateKey(t1, t2);
        Matrix2 m = matrices.get(key);

        if (m == null && reader != null) {
            try {
                m = reader.readMatrix(key);
                matrices.put(key, m);
            } catch (IOException e) {
                log.error("Error fetching matrix for: " + chr1.getName() + "-" + chr2.getName(), e);
            }
        }

        return m;

    }

    public void addNormalizationType(NormalizationType2 type) {
        if (!normalizationTypes.contains(type)) normalizationTypes.add(type);
    }

    public int getNumberZooms(HiCZoom2.Unit unit) {
        return unit == HiCZoom2.Unit.BP ? bpZooms.size() : fragZooms.size();
    }


    public HiCZoom2 getZoom(HiCZoom2.Unit unit, int index) {
        return unit == HiCZoom2.Unit.BP ? bpZooms.get(index) : fragZooms.get(index);
    }


    public List<Chromosome> getChromosomes() {
        return chromosomes;
    }


    public void setChromosomes(List<Chromosome> chromosomes) {
        this.chromosomes = chromosomes;
    }

    public void setBpZooms(int[] bpBinSizes) {

        bpZoomResolutions = Ints.asList(bpBinSizes);

        bpZooms = new ArrayList<HiCZoom2>(bpBinSizes.length);
        for (int bpBinSize : bpZoomResolutions) {
            bpZooms.add(new HiCZoom2(HiCZoom2.Unit.BP, bpBinSize));
        }
    }

    public void setFragZooms(int[] fragBinSizes) {

        // Don't show fragments in restricted mode
//        if (MainWindow.isRestricted()) return;

        this.fragZooms = new ArrayList<HiCZoom2>(fragBinSizes.length);
        for (int fragBinSize : fragBinSizes) {
            fragZooms.add(new HiCZoom2(HiCZoom2.Unit.FRAG, fragBinSize));
        }
    }


    public NormalizationVector2 getNormalizationVector(int chrIdx, HiCZoom2 zoom, NormalizationType2 type) {

        String key = NormalizationVector2.getKey(type, chrIdx, zoom.getUnit().toString(), zoom.getBinSize());
        if (type == NormalizationType2.NONE) {
            return null;
        } else if (!normalizationVectorCache.containsKey(key)) {

            try {
                NormalizationVector2 nv = reader.readNormalizationVector(type, chrIdx, zoom.getUnit(), zoom.getBinSize());
                normalizationVectorCache.put(key, nv);
            } catch (IOException e) {
                normalizationVectorCache.put(key, null);
            }
        }

        return normalizationVectorCache.get(key);

    }
}
