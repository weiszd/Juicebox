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

import htsjdk.tribble.util.LittleEndianOutputStream;
import org.apache.log4j.Logger;
import org.broad.igv.feature.Chromosome;
import org.broad.igv.util.collections.LRUCache;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author jrobinso
 * @since Aug 10, 2010
 */
public class MatrixZoomData2 {

    private static final Logger log = Logger.getLogger(MatrixZoomData2.class);
    private final Chromosome chr1;  // Chromosome on the X axis
    private final Chromosome chr2;  // Chromosome on the Y axis
    private final HiCZoom2 zoom;    // Unit and bin size
    // Observed values are organized into sub-matrices ("Block2s")
    private final int Block2BinCount;   // Block2 size in bins
    private final int Block2ColumnCount;     // number of Block2 columns
    // Cache the last 20 Block2s loaded
    private final LRUCache<String, Block2> Block2Cache = new LRUCache<String, Block2>(20);
    private final DatasetReader22 reader;
    private HiCFixedGridAxis2 xGridAxis = null;
    private HiCFixedGridAxis2 yGridAxis = null;
    private double averageCount = -1;
//    private static final SuperAdapter superAdapter = new SuperAdapter();
//    private static final Slideshow slideshow = superAdapter.getSlideshow();


//    float sumCounts;
//    float avgCounts;
//    float stdDev;
//    float percent95 = -1;
//    float percent80 = -1;


    /**
     * Constructor, sets the grid axes.  Called when read from file.
     *
     * @param chr1             Chromosome 1
     * @param chr2             Chromosome 2
     * @param zoom             Zoom (bin size and BP or FRAG)
     * @param Block2BinCount    Number of bins divided by number of columns (around 1000)
     * @param Block2ColumnCount Number of bins divided by 1000 (Block2_SIZE)
     * @param chr1Sites        Used for looking up fragment
     * @param chr2Sites        Used for looking up fragment
     * @param reader           Pointer to file reader
     */
    public MatrixZoomData2(Chromosome chr1, Chromosome chr2, HiCZoom2 zoom, int Block2BinCount, int Block2ColumnCount,
                           int[] chr1Sites, int[] chr2Sites, DatasetReader22 reader) {

        this.reader = reader;

        this.chr1 = chr1;
        this.chr2 = chr2;
        this.zoom = zoom;
        this.Block2BinCount = Block2BinCount;
        this.Block2ColumnCount = Block2ColumnCount;

        int correctedBinCount = Block2BinCount;
        if (reader.getVersion() < 8 && chr1.getLength() < chr2.getLength()) {
            boolean isFrag = zoom.getUnit() == HiCZoom2.Unit.FRAG;
            int len1 = isFrag ? (chr1Sites.length + 1) : chr1.getLength();
            int len2 = isFrag ? (chr2Sites.length + 1) : chr2.getLength();
            int nBinsX = Math.max(len1, len2) / zoom.getBinSize() + 1;
            correctedBinCount = nBinsX / Block2ColumnCount + 1;
        }

        if (zoom.getUnit() == HiCZoom2.Unit.BP) {
            this.xGridAxis = new HiCFixedGridAxis2(correctedBinCount * Block2ColumnCount, zoom.getBinSize(), chr1Sites);
            this.yGridAxis = new HiCFixedGridAxis2(correctedBinCount * Block2ColumnCount, zoom.getBinSize(), chr2Sites);
        }
    }

    public Chromosome getChr1() {
        return chr1;
    }

    public Chromosome getChr2() {
        return chr2;
    }

    public HiCFixedGridAxis2 getXGridAxis() {
        return xGridAxis;
    }

    public HiCFixedGridAxis2 getYGridAxis() {
        return yGridAxis;
    }

    public int getBinSize() {
        return zoom.getBinSize();
    }

    public int getChr1Idx() {
        return chr1.getIndex();
    }


    public int getChr2Idx() {
        return chr2.getIndex();
    }

    public HiCZoom2 getZoom() {
        return zoom;
    }

    private int getBlock2ColumnCount() {
        return Block2ColumnCount;
    }

    public String getKey() {
        return chr1.getName() + "_" + chr2.getName() + "_" + zoom.getKey();
    }


    /**
     * Return the Block2s of normalized, observed values overlapping the rectangular region specified.
     * The units are "bins"
     *
     * @param binY1 leftmost position in "bins"
     * @param binX2 rightmost position in "bins"
     * @param binY2 bottom position in "bins"
     * @param no    normalization type
     * @return List of overlapping Block2s, normalized
     */
    private List<Block2> getNormalizedBlock2sOverlapping(int binX1, int binY1, int binX2, int binY2, final NormalizationType2 no) {

        int col1 = binX1 / Block2BinCount;
        int row1 = binY1 / Block2BinCount;

        int col2 = binX2 / Block2BinCount;
        int row2 = binY2 / Block2BinCount;

        int maxSize = (col2 - col1 + 1) * (row2 - row1 + 1);

        final List<Block2> Block2List = new ArrayList<Block2>(maxSize);
        final List<Integer> Block2sToLoad = new ArrayList<Integer>();
        for (int r = row1; r <= row2; r++) {
            for (int c = col1; c <= col2; c++) {
                int Block2Number = r * getBlock2ColumnCount() + c;

                String key = getKey() + "_" + Block2Number + "_" + no;
                Block2 b;
                if (MyGlobals.useCache && Block2Cache.containsKey(key)) {
                    b = Block2Cache.get(key);
                    Block2List.add(b);
                } else {
                    Block2sToLoad.add(Block2Number);
                }
            }
        }

        final AtomicInteger errorCounter = new AtomicInteger();

        List<Thread> threads = new ArrayList<Thread>();
        for (final int Block2Number : Block2sToLoad) {
            Runnable loader = new Runnable() {
                @Override
                public void run() {
                    try {
                        String key = getKey() + "_" + Block2Number + "_" + no;
                        Block2 b = reader.readNormalizedBlock(Block2Number, MatrixZoomData2.this, no);
                        if (b == null) {
                            b = new Block2(Block2Number);   // An empty Block2
                        }
                        if (MyGlobals.useCache) {
                            Block2Cache.put(key, b);
                        }
                        Block2List.add(b);
                    } catch (IOException e) {
                        errorCounter.incrementAndGet();
                    }
                }
            };

            Thread t = new Thread(loader);
            threads.add(t);
            t.start();
        }

        // Wait for all threads to complete
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ignore) {
            }
        }

        // untested since files got fixed - MSS
        if (errorCounter.get() > 0) {
            return null;
        }

        return Block2List;
    }


    /**
     * Return the Block2s of normalized, observed values overlapping the rectangular region specified.
     * The units are "bins"
     *
     * @param binY1 leftmost position in "bins"
     * @param binX2 rightmost position in "bins"
     * @param binY2 bottom position in "bins"
     * @param no    normalization type
     * @return List of overlapping Block2s, normalized
     */
    public int addNormalizedBlock2sToList(final List<Block2> Block2List, int binX1, int binY1, int binX2, int binY2, final NormalizationType2 no) {

        int col1 = binX1 / Block2BinCount;
        int row1 = binY1 / Block2BinCount;

        int col2 = binX2 / Block2BinCount;
        int row2 = binY2 / Block2BinCount;

        List<Integer> Block2sToLoad = new ArrayList<Integer>();
        for (int r = row1; r <= row2; r++) {
            for (int c = col1; c <= col2; c++) {
                int Block2Number = r * getBlock2ColumnCount() + c;

                String key = getKey() + "_" + Block2Number + "_" + no;
                Block2 b;
                if (MyGlobals.useCache && Block2Cache.containsKey(key)) {
                    b = Block2Cache.get(key);
                    Block2List.add(b);
                } else {
                    Block2sToLoad.add(Block2Number);
                }
            }
        }

        final AtomicInteger errorCounter = new AtomicInteger();

        List<Thread> threads = new ArrayList<Thread>();
        for (final int Block2Number : Block2sToLoad) {
            Runnable loader = new Runnable() {
                @Override
                public void run() {
                    try {
                        String key = getKey() + "_" + Block2Number + "_" + no;
                        Block2 b = reader.readNormalizedBlock(Block2Number, MatrixZoomData2.this, no);
                        if (b == null) {
                            b = new Block2(Block2Number);   // An empty Block2
                        }
                        if (MyGlobals.useCache) {
                            Block2Cache.put(key, b);
                        }
                        Block2List.add(b);
                    } catch (IOException e) {
                        errorCounter.incrementAndGet();
                    }
                }
            };

            Thread t = new Thread(loader);
            threads.add(t);
            t.start();
        }

        // Wait for all threads to complete
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ignore) {
            }
        }

        // untested since files got fixed - MSS
        return errorCounter.get();
    }


    /**
     * Return the observed value at the specified location. Supports tooltip text
     * This implementation is naive, but might get away with it for tooltip.
     *
     * @param binX              X bin
     * @param binY              Y bin
     * @param normalizationType Normalization type
     */
    public float getObservedValue(int binX, int binY, NormalizationType2 normalizationType) {

        // Intra stores only lower diagonal
        if (chr1 == chr2) {
            if (binX > binY) {
                int tmp = binX;
                //noinspection SuspiciousNameCombination
                binX = binY;
                binY = tmp;

            }
        }

        List<Block2> Block2s = getNormalizedBlock2sOverlapping(binX, binY, binX, binY, normalizationType);
        if (Block2s == null) return 0;
        for (Block2 b : Block2s) {
            for (ContactRecord2 rec : b.getContactRecords()) {
                if (rec.getBinX() == binX && rec.getBinY() == binY) {
                    return rec.getCounts();
                }
            }
        }
        // No record found for this bin
        return 0;
    }


    /**
     * For a specified region, select the Block2 numbers corresponding to it
     * @param regionIndices
     * @return
     */
    private List<Integer> getBlock2NumbersForRegionFromGenomePosition(int[] regionIndices) {
        int resolution = zoom.getBinSize();
        int[] regionBinIndices = new int[4];
        for (int i = 0; i < regionBinIndices.length; i++) {
            regionBinIndices[i] = regionIndices[i] / resolution;
        }
        return getBlock2NumbersForRegionFromBinPosition(regionBinIndices);
    }

    private List<Integer> getBlock2NumbersForRegionFromBinPosition(int[] regionIndices) {

        int col1 = regionIndices[0] / Block2BinCount;
        int col2 = (regionIndices[1] + 1) / Block2BinCount;
        int row1 = regionIndices[2] / Block2BinCount;
        int row2 = (regionIndices[3] + 1) / Block2BinCount;

        // first check the upper triangular matrix
        Set<Integer> Block2sSet = new HashSet<Integer>();
        for (int r = row1; r <= row2; r++) {
            for (int c = col1; c <= col2; c++) {
                int Block2Number = r * getBlock2ColumnCount() + c;
                Block2sSet.add(Block2Number);
            }
        }
        // check region part that overlaps with lower left triangle
        // but only if intrachromosomal
        if (chr1.getIndex() == chr2.getIndex()) {
            for (int r = col1; r <= col2; r++) {
                for (int c = row1; c <= row2; c++) {
                    int Block2Number = r * getBlock2ColumnCount() + c;
                    Block2sSet.add(Block2Number);
                }
            }
        }

        List<Integer> Block2sToIterateOver = new ArrayList<Integer>(Block2sSet);
        Collections.sort(Block2sToIterateOver);
        return Block2sToIterateOver;
    }


    public void dump(PrintWriter printWriter, LittleEndianOutputStream les, NormalizationType2 norm, MatrixType2 matrixType2,
                     boolean useRegionIndices, int[] regionIndices, ExpectedValueFunction2 df) throws IOException {

        // determine which output will be used
        if (printWriter == null && les == null) {
            printWriter = new PrintWriter(System.out);
        }
        boolean usePrintWriter = printWriter != null && les == null;
        boolean isIntraChromosomal = chr1.getIndex() == chr2.getIndex();

        if (matrixType2 == MatrixType2.PEARSON) {
            return;
        }

        // Get the Block2 index keys, and sort
        List<Integer> Block2sToIterateOver;
        if (useRegionIndices) {
            Block2sToIterateOver = getBlock2NumbersForRegionFromGenomePosition(regionIndices);
        } else {
            Block2sToIterateOver = reader.getBlockNumbers(this);
            Collections.sort(Block2sToIterateOver);
        }

        for (Integer Block2Number : Block2sToIterateOver) {
            Block2 b = reader.readNormalizedBlock(Block2Number, MatrixZoomData2.this, norm);
            if (b != null) {
                for (ContactRecord2 rec : b.getContactRecords()) {
                    float counts = rec.getCounts();
                    int x = rec.getBinX();
                    int y = rec.getBinY();
                    int xActual = x * zoom.getBinSize();
                    int yActual = y * zoom.getBinSize();
                    float oeVal = 0f;
                    if (matrixType2 == MatrixType2.OE) {
                        int dist = Math.abs(x - y);
                        double expected = 0;
                        try {
                            expected = df.getExpectedValue(chr1.getIndex(), dist);
                        } catch (Exception e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                        double observed = rec.getCounts(); // Observed is already normalized
                        oeVal = (float) (observed / expected);
                    }
                    if (!useRegionIndices || // i.e. use full matrix
                            // or check regions that overlap with upper left
                            (xActual >= regionIndices[0] && xActual <= regionIndices[1] &&
                                    yActual >= regionIndices[2] && yActual <= regionIndices[3]) ||
                            // or check regions that overlap with lower left
                            (isIntraChromosomal && yActual >= regionIndices[0] && yActual <= regionIndices[1] &&
                                    xActual >= regionIndices[2] && xActual <= regionIndices[3])) {
                        // but leave in upper right triangle coordinates
                        if (usePrintWriter) {
                            if (matrixType2 == MatrixType2.OBSERVED) {
                                printWriter.println(xActual + "\t" + yActual + "\t" + counts);
                            } else if (matrixType2 == MatrixType2.OE) {
                                printWriter.println(xActual + "\t" + yActual + "\t" + oeVal);
                            }
                        } else {
                            if (matrixType2 == MatrixType2.OBSERVED) {
                                les.writeInt(x);
                                les.writeInt(y);
                                les.writeFloat(counts);
                            } else if (matrixType2 == MatrixType2.OE) {
                                les.writeInt(x);
                                les.writeInt(y);
                                les.writeFloat(oeVal);
                            }
                        }
                    }
                }
            }
        }

        if (usePrintWriter) {
            printWriter.close();
        }
    }

    public void dump1DTrackFromCrossHairAsWig(PrintWriter printWriter, Chromosome chromosomeForPosition,
                                              int binStartPosition, boolean isIntraChromosomal, int[] regionBinIndices,
                                              NormalizationType2 norm, MatrixType2 matrixType2,
                                              ExpectedValueFunction2 expectedValues) throws IOException {

        if (!MatrixType2.isObservedOrControl(matrixType2)) {
            System.out.println("This feature is only available for Observed or Control views");
            return;
        }

        int binCounter = 0;

        // Get the Block2 index keys, and sort
        List<Integer> Block2sToIterateOver = getBlock2NumbersForRegionFromBinPosition(regionBinIndices);
        Collections.sort(Block2sToIterateOver);

        for (Integer Block2Number : Block2sToIterateOver) {
            Block2 b = reader.readNormalizedBlock(Block2Number, MatrixZoomData2.this, norm);
            if (b != null) {
                for (ContactRecord2 rec : b.getContactRecords()) {
                    float counts = rec.getCounts();
                    int x = rec.getBinX();
                    int y = rec.getBinY();

                    if (    //check regions that overlap with upper left
                            (x >= regionBinIndices[0] && x <= regionBinIndices[1] &&
                                    y >= regionBinIndices[2] && y <= regionBinIndices[3]) ||
                                    // or check regions that overlap with lower left
                                    (isIntraChromosomal && x >= regionBinIndices[2] && x <= regionBinIndices[3] &&
                                            y >= regionBinIndices[0] && y <= regionBinIndices[1])) {
                        // but leave in upper right triangle coordinates

                        if (x == binStartPosition) {
                            while (binCounter < y) {
                                printWriter.println("0");
                                binCounter++;
                            }
                        } else if (y == binStartPosition) {
                            while (binCounter < x) {
                                printWriter.println("0");
                                binCounter++;
                            }
                        } else {
                            System.err.println("Something went wrong while generating 1D track");
                            System.err.println("Improper input was likely provided");
                        }

                        printWriter.println(counts);
                        binCounter++;

                    }
                }
            }
        }
    }

    /**
     * Returns the average count
     *
     * @return Average count
     */
    public double getAverageCount() {
        return averageCount;
    }

    /**
     * Sets the average count
     *
     * @param averageCount Average count to set
     */
    public void setAverageCount(double averageCount) {
        this.averageCount = averageCount;
    }

    /**
     * Returns iterator for contact records
     *
     * @return iterator for contact records
     */
    public Iterator<ContactRecord2> contactRecordIterator() {
        return new ContactRecordIterator();
    }

    /**
     * Class for iterating over the contact records
     */
    public class ContactRecordIterator implements Iterator<ContactRecord2> {

        final List<Integer> Block2Numbers;
        int Block2Idx;
        Iterator<ContactRecord2> currentBlock2Iterator;

        /**
         * Initializes the iterator
         */
        public ContactRecordIterator() {
            this.Block2Idx = -1;
            this.Block2Numbers = reader.getBlockNumbers(MatrixZoomData2.this);
        }

        /**
         * Indicates whether or not there is another Block2 waiting; checks current Block2
         * iterator and creates a new one if need be
         *
         * @return true if there is another Block2 to be read
         */
        @Override
        public boolean hasNext() {

            if (currentBlock2Iterator != null && currentBlock2Iterator.hasNext()) {
                return true;
            } else {
                Block2Idx++;
                if (Block2Idx < Block2Numbers.size()) {
                    try {
                        int Block2Number = Block2Numbers.get(Block2Idx);

                        // Optionally check the cache
                        String key = getKey() + "_" + Block2Number + "_" + NormalizationType2.NONE;
                        Block2 nextBlock2;
                        if (MyGlobals.useCache && Block2Cache.containsKey(key)) {
                            nextBlock2 = Block2Cache.get(key);
                        } else {
                            nextBlock2 = reader.readBlock(Block2Number, MatrixZoomData2.this);
                        }
                        currentBlock2Iterator = nextBlock2.getContactRecords().iterator();
                        return true;
                    } catch (IOException e) {
                        log.error("Error fetching Block2 ", e);
                        return false;
                    }
                }
            }

            return false;
        }

        /**
         * Returns the next contact record
         *
         * @return The next contact record
         */
        @Override
        public ContactRecord2 next() {
            return currentBlock2Iterator == null ? null : currentBlock2Iterator.next();
        }

        /**
         * Not supported
         */
        @Override
        public void remove() {
            //Not supported
            throw new RuntimeException("remove() is not supported");
        }
    }
//    public void preloadSlides(){

//    }
}
