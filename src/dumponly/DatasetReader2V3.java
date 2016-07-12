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


import htsjdk.samtools.seekablestream.SeekableHTTPStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.tribble.util.LittleEndianInputStream;
import org.broad.igv.feature.Chromosome;
import org.broad.igv.util.CompressionUtils;
import org.broad.igv.util.stream.IGVSeekableStreamFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


/**
 * @author jrobinso
 * @date Aug 17, 2010
 */
public class DatasetReader2V3 extends AbstractDatasetReader2 {

    /**
     * Cache of chromosome name -> array of restriction sites
     */
    private final Map<String, int[]> fragmentSitesCache = new HashMap<String, int[]>();
    private final CompressionUtils compressionUtils;
    private SeekableStream stream;
    private Map<String, IndexEntry> masterIndex;
    private Map<String, IndexEntry> normVectorIndex;
    private Dataset22 dataset = null;
    private int version = -1;
    private Map<String, FragIndexEntry> fragmentSitesIndex;
    private Map<String, Map<Integer, IndexEntry>> blockIndexMap;
    private long masterIndexPos;

    public DatasetReader2V3(String path) throws IOException {

        super(path);
        this.stream = IGVSeekableStreamFactory.getInstance().getStreamFor(path);

        if (this.stream != null) {
            masterIndex = new HashMap<String, IndexEntry>();
            dataset = new Dataset22(this);
        }
        compressionUtils = new CompressionUtils();
        blockIndexMap = new HashMap<String, Map<Integer, IndexEntry>>();
    }

    public static String getMagicString(String path) throws IOException {

        SeekableStream stream = null;
        LittleEndianInputStream dis = null;

        try {
            stream = new SeekableHTTPStream(new URL(path));
            dis = new LittleEndianInputStream(new BufferedInputStream(stream));
        } catch (MalformedURLException e) {
            try {
                dis = new LittleEndianInputStream(new FileInputStream(path));
            }
            catch (Exception e2){
                e2.printStackTrace();
            }
        } finally {
            if (stream != null) stream.close();

        }
        if(dis != null) {
            return dis.readString();
        }
        return null;
    }

    private MatrixZoomData2 readMatrixZoomData2(Chromosome chr1, Chromosome chr2, int[] chr1Sites, int[] chr2Sites,
                                                LittleEndianInputStream dis) throws IOException {

        HiCZoom2.Unit unit = HiCZoom2.Unit.valueOf(dis.readString());
        dis.readInt();                // Old "zoom" index -- not used

        // Stats.  Not used yet, but we need to read them anyway
        double sumCounts = (double) dis.readFloat();
        dis.readFloat();
        dis.readFloat();
        dis.readFloat();

        int binSize = dis.readInt();
        HiCZoom2 zoom = new HiCZoom2(unit, binSize);
        // todo: Default binSize value for "ALL" is 6197...
        // We need to make sure our maps hold a valid binSize value as default.

        int blockBinCount = dis.readInt();
        int blockColumnCount = dis.readInt();

        MatrixZoomData2 zd = new MatrixZoomData2(chr1, chr2, zoom, blockBinCount, blockColumnCount, this);

        int nBlocks = dis.readInt();
        HashMap<Integer, IndexEntry> blockIndex = new HashMap<Integer, IndexEntry>(nBlocks);

        for (int b = 0; b < nBlocks; b++) {
            int blockNumber = dis.readInt();
            long filePosition = dis.readLong();
            int blockSizeInBytes = dis.readInt();
            blockIndex.put(blockNumber, new IndexEntry(filePosition, blockSizeInBytes));
        }
        blockIndexMap.put(zd.getKey(), blockIndex);

        return zd;
    }

    @Override
    public Dataset22 read() throws IOException {

        try {
            long position = stream.position();

            // Read the header
            LittleEndianInputStream dis = new LittleEndianInputStream(new BufferedInputStream(stream));

            String magicString = dis.readString();
            position += magicString.length() + 1;
            if (!magicString.equals("HIC")) {
                throw new IOException("Magic string is not HIC, this does not appear to be a hic file.");
            }

            version = dis.readInt();
            position += 4;

            System.out.println("HiC file version: " + version);

            masterIndexPos = dis.readLong();
            position += 8;

            String genomeId = dis.readString();
            position += genomeId.length() + 1;

            //dataset.setGenomeId(genomeId);

            Map<String, String> attributes = new HashMap<String, String>();
            // Attributes  (key-value pairs)
            if (version > 4) {
                int nAttributes = dis.readInt();
                position += 4;

                for (int i = 0; i < nAttributes; i++) {
                    String key = dis.readString();
                    position += key.length() + 1;

                    String value = dis.readString();
                    position += value.length() + 1;
                    attributes.put(key, value);
                }
            }

            //dataset.setAttributes(attributes);

            // Read chromosome dictionary
            int nchrs = dis.readInt();
            position += 4;

            List<Chromosome> chromosomes = new ArrayList<Chromosome>(nchrs);
            for (int i = 0; i < nchrs; i++) {
                String name = dis.readString();
                position += name.length() + 1;

                int size = dis.readInt();
                position += 4;

                chromosomes.add(new Chromosome(i, name, size));
            }
            dataset.setChromosomes(chromosomes);

            int nBpResolutions = dis.readInt();
            position += 4;

            int[] bpBinSizes = new int[nBpResolutions];
            for (int i = 0; i < nBpResolutions; i++) {
                bpBinSizes[i] = dis.readInt();
                position += 4;
            }
            dataset.setBpZooms(bpBinSizes);

            // note that these still should be read, we'll just ignore the actual usage of frag res maps
            int nFragResolutions = dis.readInt();
            position += 4;

            int[] fragBinSizes = new int[nFragResolutions];
            for (int i = 0; i < nFragResolutions; i++) {
                fragBinSizes[i] = dis.readInt();
                position += 4;
            }
            //dataset.setFragZooms(fragBinSizes);

            // Now we need to skip  through stream reading # fragments, stream on buffer is not needed so null it to
            // prevent accidental use
            if (nFragResolutions > 0) {
                stream.seek(position);
                fragmentSitesIndex = new HashMap<String, FragIndexEntry>();
                Map<String, Integer> map = new HashMap<String, Integer>();
                for (int i = 0; i < nchrs; i++) {
                    String chr = chromosomes.get(i).getName();

                    byte[] buffer = new byte[4];
                    stream.readFully(buffer);
                    int nSites = (new LittleEndianInputStream(new ByteArrayInputStream(buffer))).readInt();
                    position += 4;

                    FragIndexEntry entry = new FragIndexEntry(position, nSites);
                    fragmentSitesIndex.put(chr, entry);
                    map.put(chr, nSites);

                    stream.skip(nSites * 4);
                    position += nSites * 4;
                }
                //dataset.setRestrictionEnzyme(map.get(chromosomes.get(1).getName()));
                //dataset.setFragmentCounts(map);
            }


            readFooter(masterIndexPos);


        } catch (IOException e) {
            System.err.println("Error reading dataset");
            throw e;
        }


        return dataset;

    }

    private int[] readSites(long location, int nSites) throws IOException {

        stream.seek(location);
        byte[] buffer = new byte[4 + nSites * 4];
        stream.readFully(buffer);
        LittleEndianInputStream les = new LittleEndianInputStream(new ByteArrayInputStream(buffer));
        int[] sites = new int[nSites];
        for (int s = 0; s < nSites; s++) {
            sites[s] = les.readInt();
        }
        return sites;

    }


    @Override
    public int getVersion() {
        return version;
    }

    private void readFooter(long position) throws IOException {

        stream.seek(position);

        //Get the size in bytes of the v5 footer, that is the footer up to normalization and normalized expected values
        byte[] buffer = new byte[4];
        stream.read(buffer);
        LittleEndianInputStream dis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));
        int nBytes = dis.readInt();

        buffer = new byte[nBytes];
        stream.read(buffer);
        dis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));

        int nEntries = dis.readInt();
        for (int i = 0; i < nEntries; i++) {
            String key = dis.readString();
            long filePosition = dis.readLong();
            int sizeInBytes = dis.readInt();
            masterIndex.put(key, new IndexEntry(filePosition, sizeInBytes));
        }

        Map<String, ExpectedValueFunction2> expectedValuesMap = new LinkedHashMap<String, ExpectedValueFunction2>();

        // Expected values from non-normalized matrix
        int nExpectedValues = dis.readInt();
        for (int i = 0; i < nExpectedValues; i++) {

            NormalizationType2 no = NormalizationType2.NONE;
            String unit = dis.readString();
            int binSize = dis.readInt();
            String key = unit + "_" + binSize + "_" + no;

            int nValues = dis.readInt();
            double[] values = new double[nValues];
            for (int j = 0; j < nValues; j++) {
                values[j] = dis.readDouble();
            }

            int nNormalizationFactors = dis.readInt();
            Map<Integer, Double> normFactors = new LinkedHashMap<Integer, Double>();
            for (int j = 0; j < nNormalizationFactors; j++) {
                Integer chrIdx = dis.readInt();
                Double normFactor = dis.readDouble();
                normFactors.put(chrIdx, normFactor);
            }

            ExpectedValueFunction2 df = new ExpectedValueFunctionImpl2(unit);
            expectedValuesMap.put(key, df);
            //dataset.setExpectedValueFunctionMap(expectedValuesMap);

        }

        // Normalized expected values (v6 and greater only)

        if (version >= 6) {

            //dis = new LittleEndianInputStream(new BufferedInputStream(stream, 512000));
            dis = new LittleEndianInputStream(new BufferedInputStream(stream, MyGlobals.bufferSize));

            try {
                nExpectedValues = dis.readInt();
            } catch (EOFException e) {
                System.out.println("No normalization vectors");
                return;
            }

            for (int i = 0; i < nExpectedValues; i++) {

                String typeString = dis.readString();
                String unit = dis.readString();
                int binSize = dis.readInt();
                String key = unit + "_" + binSize + "_" + typeString;

                int nValues = dis.readInt();
                double[] values = new double[nValues];
                for (int j = 0; j < nValues; j++) {
                    values[j] = dis.readDouble();
                }

                int nNormalizationFactors = dis.readInt();
                Map<Integer, Double> normFactors = new LinkedHashMap<Integer, Double>();
                for (int j = 0; j < nNormalizationFactors; j++) {
                    Integer chrIdx = dis.readInt();
                    Double normFactor = dis.readDouble();
                    normFactors.put(chrIdx, normFactor);
                }

                NormalizationType2 type = NormalizationType2.valueOf(typeString);
                ExpectedValueFunction2 df = new ExpectedValueFunctionImpl2(unit);
                expectedValuesMap.put(key, df);

            }

            // Normalization vectors (indexed)

            nEntries = dis.readInt();
            normVectorIndex = new HashMap<String, IndexEntry>(nEntries * 2);
            for (int i = 0; i < nEntries; i++) {

                NormalizationType2 type = NormalizationType2.valueOf(dis.readString());
                int chrIdx = dis.readInt();
                String unit = dis.readString();
                int resolution = dis.readInt();
                long filePosition = dis.readLong();
                int sizeInBytes = dis.readInt();

                String key = NormalizationVector2.getKey(type, chrIdx, unit, resolution);

                dataset.addNormalizationType(type);

                normVectorIndex.put(key, new IndexEntry(filePosition, sizeInBytes));
            }


        }
    }

    @Override
    public Matrix2 readMatrix(String key) throws IOException {
        IndexEntry idx = masterIndex.get(key);
        if (idx == null) {
            return null;
        }

        byte[] buffer = new byte[idx.size];
        stream.seek(idx.position);
        stream.readFully(buffer);
        LittleEndianInputStream dis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));

        int c1 = dis.readInt();
        int c2 = dis.readInt();
        Chromosome chr1 = dataset.getChromosomes().get(c1);
        Chromosome chr2 = dataset.getChromosomes().get(c2);

        // # of resolution levels (bp and frags)
        int nResolutions = dis.readInt();

        List<MatrixZoomData2> zdList = new ArrayList<MatrixZoomData2>();

        int[] chr1Sites = fragmentSitesCache.get(chr1.getName());
        if (chr1Sites == null && fragmentSitesIndex != null) {
            FragIndexEntry entry = fragmentSitesIndex.get(chr1.getName());
            if (entry != null && entry.nSites > 0) {
                chr1Sites = readSites(entry.position, entry.nSites);
            }
            fragmentSitesCache.put(chr1.getName(), chr1Sites);
        }
        int[] chr2Sites = fragmentSitesCache.get(chr2.getName());
        if (chr2Sites == null && fragmentSitesIndex != null) {
            FragIndexEntry entry = fragmentSitesIndex.get(chr2.getName());
            if (entry != null && entry.nSites > 0) {
                chr2Sites = readSites(entry.position, entry.nSites);
            }
            fragmentSitesCache.put(chr2.getName(), chr2Sites);
        }

        for (int i = 0; i < nResolutions; i++) {
            MatrixZoomData2 zd = readMatrixZoomData2(chr1, chr2, chr1Sites, chr2Sites, dis);
            zdList.add(zd);
        }

        return new Matrix2(zdList);
    }

    public int getFragCount(Chromosome chromosome) {
        FragIndexEntry entry = null;
        if (fragmentSitesIndex != null)
            entry = fragmentSitesIndex.get(chromosome.getName());

        if (entry != null) {
            return entry.nSites;
        } else return -1;
    }

    @Override
    synchronized public Block2 readBlock(int blockNumber, MatrixZoomData2 zd) throws IOException {

        Block2 b = null;
        Map<Integer, IndexEntry> blockIndex = blockIndexMap.get(zd.getKey());
        if (blockIndex != null) {

            IndexEntry idx = blockIndex.get(blockNumber);
            if (idx != null) {

                //System.out.println(" blockIndexPosition:" + idx.position);

                byte[] compressedBytes = new byte[idx.size];
                stream.seek(idx.position);
                stream.readFully(compressedBytes);
//                System.out.println();
//                System.out.print("ID: ");
//                System.out.print(idx.id);
//                System.out.print(" Pos: ");
//                System.out.print(idx.position);
//                System.out.print(" Size: ");
//                System.out.println(idx.size);
                byte[] buffer;

                try {
                    buffer = compressionUtils.decompress(compressedBytes);

                } catch (Exception e) {
                    throw new RuntimeException("Block2 read error: " + e.getMessage());
                }

                LittleEndianInputStream dis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));
                int nRecords = dis.readInt();
                List<ContactRecord2> records = new ArrayList<ContactRecord2>(nRecords);

                if (version < 7) {
                    for (int i = 0; i < nRecords; i++) {
                        int binX = dis.readInt();
                        int binY = dis.readInt();
                        float counts = dis.readFloat();
                        records.add(new ContactRecord2(binX, binY, counts));
                    }
                } else {

                    int binXOffset = dis.readInt();
                    int binYOffset = dis.readInt();

                    boolean useShort = dis.readByte() == 0;

                    byte type = dis.readByte();

                    if (type == 1) {
                        // List-of-rows representation
                        int rowCount = dis.readShort();

                        for (int i = 0; i < rowCount; i++) {

                            int binY = binYOffset + dis.readShort();
                            int colCount = dis.readShort();

                            for (int j = 0; j < colCount; j++) {

                                int binX = binXOffset + dis.readShort();
                                float counts = useShort ? dis.readShort() : dis.readFloat();
                                records.add(new ContactRecord2(binX, binY, counts));
                            }
                        }
                    } else if (type == 2) {

                        int nPts = dis.readInt();
                        int w = dis.readShort();

                        for (int i = 0; i < nPts; i++) {
                            //int idx = (p.y - binOffset2) * w + (p.x - binOffset1);
                            int row = i / w;
                            int col = i - row * w;
                            int bin1 = binXOffset + col;
                            int bin2 = binYOffset + row;

                            if (useShort) {
                                short counts = dis.readShort();
                                if (counts != Short.MIN_VALUE) {
                                    records.add(new ContactRecord2(bin1, bin2, counts));
                                }
                            } else {
                                float counts = dis.readFloat();
                                if (!Float.isNaN(counts)) {
                                    records.add(new ContactRecord2(bin1, bin2, counts));
                                }
                            }


                        }

                    } else {
                        throw new RuntimeException("Unknown Block2 type: " + type);
                    }
                }
                b = new Block2(records);
            }
        }

        // If no Block2 exists, mark with an "empty block" to prevent further attempts
        if (b == null) {
            b = new Block2();
        }
        return b;
    }

    @Override
    public Block2 readNormalizedBlock(int blockNumber, MatrixZoomData2 zd, NormalizationType2 no) throws IOException {


        if (no == null) {
            throw new IOException("Normalization type is null");
        } else if (no == NormalizationType2.NONE) {
            return readBlock(blockNumber, zd);
        } else {
            NormalizationVector2 nv1 = dataset.getNormalizationVector(zd.getChr1Idx(), zd.getZoom(), no);
            NormalizationVector2 nv2 = dataset.getNormalizationVector(zd.getChr2Idx(), zd.getZoom(), no);
            if (nv1 == null || nv2 == null) {
                throw new IOException("Normalization missing for: " + zd.getKey());
            }
            double[] nv1Data = nv1.getData();
            double[] nv2Data = nv2.getData();
            Block2 rawBlock = readBlock(blockNumber, zd);
            if (rawBlock == null) return null;

            Collection<ContactRecord2> records = rawBlock.getContactRecords();
            List<ContactRecord2> normRecords = new ArrayList<ContactRecord2>(records.size());
            for (ContactRecord2 rec : records) {
                int x = rec.getBinX();
                int y = rec.getBinY();
                float counts;
                if (nv1Data[x] != 0 && nv2Data[y] != 0 && !Double.isNaN(nv1Data[x]) && !Double.isNaN(nv2Data[y])) {
                    counts = (float) (rec.getCounts() / (nv1Data[x] * nv2Data[y]));
                } else {
                    counts = Float.NaN;
                }
                normRecords.add(new ContactRecord2(x, y, counts));
            }

            //double sparsity = (normRecords.size() * 100) / (Preprocessor.BLOCK_SIZE * Preprocessor.BLOCK_SIZE);
            //System.out.println(sparsity);

            return new Block2(normRecords);
        }
    }

    @Override
    public List<Integer> getBlockNumbers(MatrixZoomData2 zd) {
        Map<Integer, IndexEntry> blockIndex = blockIndexMap.get(zd.getKey());
        return blockIndex == null ? null : new ArrayList<Integer>(blockIndex.keySet());
    }

    @Override
    public synchronized NormalizationVector2 readNormalizationVector(NormalizationType2 type, int chrIdx, HiCZoom2.Unit unit, int binSize) throws IOException {

        String key = NormalizationVector2.getKey(type, chrIdx, unit.toString(), binSize);
        if (normVectorIndex == null) return null;
        IndexEntry idx = normVectorIndex.get(key);
        if (idx == null) return null;

        byte[] buffer = new byte[idx.size];
        stream.seek(idx.position);
        stream.readFully(buffer);
        LittleEndianInputStream dis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));

        int nValues = dis.readInt();
        double[] values = new double[nValues];
        boolean allNaN = true;
        for (int i = 0; i < nValues; i++) {
            values[i] = dis.readDouble();
            if (!Double.isNaN(values[i])) {
                allNaN = false;
            }
        }
        if (allNaN) return null;
        else return new NormalizationVector2(values);


    }


    static class FragIndexEntry {
        final long position;
        final int nSites;

        FragIndexEntry(long position, int nSites) {
            this.position = position;
            this.nSites = nSites;
        }
    }

    public static class IndexEntry {
        public final long position;
        public final int size;

        public IndexEntry(long position, int size) {
            this.position = position;
            this.size = size;
        }
    }
}
