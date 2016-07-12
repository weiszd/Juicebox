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
import org.broad.igv.feature.Chromosome;
import org.broad.igv.util.collections.LRUCache;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class MatrixZoomData2 {

    private final Chromosome chr1;  // Chromosome on the X axis
    private final Chromosome chr2;  // Chromosome on the Y axis
    private final HiCZoom2 zoom;    // Unit and bin size
    // Observed values are organized into sub-matrices ("Block2s")
    private final int Block2BinCount;   // Block2 size in bins
    private final int Block2ColumnCount;     // number of Block2 columns
    // Cache the last 20 Block2s loaded
    private final LRUCache<String, Block2> Block2Cache = new LRUCache<String, Block2>(20);
    private final DatasetReader2V3 reader;

    /**
     * Constructor, sets the grid axes.  Called when read from file.
     *
     * @param chr1             Chromosome 1
     * @param chr2             Chromosome 2
     * @param zoom             Zoom (bin size and BP or FRAG)
     * @param Block2BinCount    Number of bins divided by number of columns (around 1000)
     * @param Block2ColumnCount Number of bins divided by 1000 (Block2_SIZE)
     * @param reader           Pointer to file reader
     */
    public MatrixZoomData2(Chromosome chr1, Chromosome chr2, HiCZoom2 zoom, int Block2BinCount, int Block2ColumnCount,
                           DatasetReader2V3 reader) {

        this.reader = reader;

        this.chr1 = chr1;
        this.chr2 = chr2;
        this.zoom = zoom;
        this.Block2BinCount = Block2BinCount;
        this.Block2ColumnCount = Block2ColumnCount;
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
                     boolean useRegionIndices, int[] regionIndices, ExpectedValueFunctionImpl2 df) throws IOException {

        // determine which output will be used
        if (printWriter == null && les == null) {
            printWriter = new PrintWriter(System.out);
        }
        boolean usePrintWriter = printWriter != null && les == null;
        boolean isIntraChromosomal = chr1.getIndex() == chr2.getIndex();

        if (matrixType2 == null) {
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
                            }
                        } else {
                            if (matrixType2 == MatrixType2.OBSERVED) {
                                les.writeInt(x);
                                les.writeInt(y);
                                les.writeFloat(counts);
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
                        System.err.println("Error fetching Block2");
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
}
