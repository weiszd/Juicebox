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

import htsjdk.samtools.seekablestream.SeekableHTTPStream;
import juicebox.HiCGlobals;
import juicebox.data.feature.GenomeWideList;
import org.broad.igv.feature.Chromosome;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by muhammadsaadshamim on 8/3/16.
 */
public class GeneTools {

    public static GenomeWideList<GeneLocation> parseGenome(String genomeID, List<Chromosome> chromosomes) {
        GenomeWideList<GeneLocation> geneLocationList = null;
        try {
            BufferedReader reader = getReaderToGeneFile(genomeID);
            geneLocationList = new GenomeWideList<GeneLocation>(chromosomes, getAllGenes(reader, chromosomes));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return geneLocationList;
    }

    public static BufferedReader getReaderToGeneFile(String genomeID) throws MalformedURLException {
        // Custom format parsed from ref Gene file.
        // Name1 Name2 chromosome position (where position is midpoint of transcription start and end)
        String path = "http://hicfiles.s3.amazonaws.com/internal/" + genomeID + "_refGene.txt";
        SeekableHTTPStream stream = new SeekableHTTPStream(new URL(path));
        return new BufferedReader(new InputStreamReader(stream), HiCGlobals.bufferSize);
    }

    public static List<GeneLocation> getAllGenes(BufferedReader reader, List<Chromosome> chromosomes) throws IOException {

        List<GeneLocation> allGenes = new ArrayList<GeneLocation>();

        Map<String, Integer> chrNameToIndex = new HashMap<String, Integer>();
        for (Chromosome chromosome : chromosomes) {
            chrNameToIndex.put(chromosome.getName().toLowerCase().replaceAll("chr", ""), chromosome.getIndex());
        }

        String nextLine;
        while ((nextLine = reader.readLine()) != null) {
            String[] values = nextLine.split(" ");
            String chrKey = values[2].trim().replaceAll("chr", "");
            int chrIndex = chrNameToIndex.get(chrKey);
            int location = Integer.valueOf(values[3].trim());

            // both gene names
            allGenes.add(new GeneLocation(values[0].trim(), chrIndex, location));
            allGenes.add(new GeneLocation(values[1].trim(), chrIndex, location));
        }

        return allGenes;
    }

    public static Map<String, GeneLocation> readGeneFileToLocationMap(BufferedReader reader,
                                                                      List<Chromosome> chromosomes) throws IOException {

        List<GeneLocation> allGenes = getAllGenes(reader, chromosomes);

        Map<String, GeneLocation> geneLocationHashMap = new HashMap<String, GeneLocation>();
        for (GeneLocation gene : allGenes) {
            geneLocationHashMap.put(gene.getName().toLowerCase(), gene);
        }

        return geneLocationHashMap;
    }
}
