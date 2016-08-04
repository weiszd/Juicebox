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

package juicebox.data;

import juicebox.HiCGlobals;
import juicebox.tools.chrom.sizes.ChromosomeSizes;
import org.broad.igv.Globals;
import org.broad.igv.feature.Chromosome;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by muhammadsaadshamim on 8/3/16.
 */
public class ChromosomeHandler {
    private Map<String, Chromosome> internalChrNameToChrMap = new HashMap<String, Chromosome>();
    private Collection<Chromosome> internalChromosomes;

    public ChromosomeHandler(Collection<Chromosome> chromosomes) {
        this.internalChromosomes = chromosomes;
        reset();
    }

    public ChromosomeHandler(String genomeID) {
        this(loadChromosomes(genomeID));
    }

    public ChromosomeHandler(String genomeID, Set<String> givenChromosomes) {
        this(genomeID);
        setToSpecifiedChromosomes(givenChromosomes);
    }

    public ChromosomeHandler(Collection<Chromosome> chromosomes, Set<String> givenChromosomes) {
        this(chromosomes);
        setToSpecifiedChromosomes(givenChromosomes);
    }

    /**
     * For each given chromosome name, find its equivalent Chromosome object
     *
     * @param chromosomesSpecified by strings
     * @param referenceChromosomes as Chromosome objects
     * @return the specified Chromosomes corresponding to the given strings
     */
    public static Set<Chromosome> stringToChromosomes(Set<String> chromosomesSpecified,
                                                      List<Chromosome> referenceChromosomes) {
        Set<Chromosome> chromosomes = new HashSet<Chromosome>();

        for (String strKey : chromosomesSpecified) {
            boolean chrFound = false;
            for (Chromosome chrKey : referenceChromosomes) {
                if (equivalentChromosome(strKey, chrKey)) {
                    chromosomes.add(chrKey);
                    chrFound = true;
                    break;
                }
            }
            if (!chrFound) {
                System.err.println("Chromosome " + strKey + " not found");
            }
        }
        return new HashSet<Chromosome>(chromosomes);
    }

    /**
     * Set intersection
     * http://stackoverflow.com/questions/7574311/efficiently-compute-intersection-of-two-sets-in-java
     *
     * @param collection1
     * @param collection2
     * @return intersection of set1 and set2
     */
    public static Set<Chromosome> getSetIntersection(Collection<Chromosome> collection1, Collection<Chromosome> collection2) {
        Set<Chromosome> set1 = new HashSet<Chromosome>(collection1);
        Set<Chromosome> set2 = new HashSet<Chromosome>(collection2);

        boolean set1IsLarger = set1.size() > set2.size();
        Set<Chromosome> cloneSet = new HashSet<Chromosome>(set1IsLarger ? set2 : set1);
        cloneSet.retainAll(set1IsLarger ? set1 : set2);
        return cloneSet;
    }

    /**
     * Evaluates whether the same chromosome is being referenced by the token
     *
     * @param token
     * @param chr
     * @return
     */
    public static boolean equivalentChromosome(String token, Chromosome chr) {
        String token2 = token.toLowerCase().replaceAll("chr", "");
        String chrName = chr.getName().toLowerCase().replaceAll("chr", "");
        return token2.equals(chrName);
    }

    public static Chromosome getChromosomeNamed(String chrName, List<Chromosome> chromosomes) {
        for (Chromosome chr : chromosomes) {
            if (equivalentChromosome(chrName, chr))
                return chr;
        }
        return null;
    }

    /**
     * Load the list of chromosomes based on given genome id or file
     *
     * @param idOrFile string
     * @return list of chromosomes
     */
    public static List<Chromosome> loadChromosomes(String idOrFile) {

        InputStream is = null;

        try {
            // Note: to get this to work, had to edit Intellij settings
            // so that "?*.sizes" are considered sources to be copied to class path
            is = ChromosomeSizes.class.getResourceAsStream(idOrFile + ".chrom.sizes");

            if (is == null) {
                // Not an ID,  see if its a file
                File file = new File(idOrFile);

                try {
                    if (file.exists()) {
                        is = new FileInputStream(file);
                    } else {
                        System.err.println("Could not find chromosome sizes file for: " + idOrFile);
                        System.exit(35);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            List<Chromosome> chromosomes = new ArrayList<Chromosome>();
            chromosomes.add(0, null);   // Index 0 reserved for "whole genome" pseudo-chromosome

            Pattern pattern = Pattern.compile("\t");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is), HiCGlobals.bufferSize);
            String nextLine;
            long genomeLength = 0;
            int idx = 1;

            try {
                while ((nextLine = reader.readLine()) != null) {
                    String[] tokens = pattern.split(nextLine);
                    if (tokens.length == 2) {
                        String name = tokens[0];
                        int length = Integer.parseInt(tokens[1]);
                        genomeLength += length;
                        chromosomes.add(idx, new Chromosome(idx, name, length));
                        idx++;
                    } else {
                        System.out.println("Skipping " + nextLine);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Add the "pseudo-chromosome" All, representing the whole genome.  Units are in kilo-bases
            chromosomes.set(0, new Chromosome(0, Globals.CHR_ALL, (int) (genomeLength / 1000)));

            return chromosomes;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void reset() {
        internalChrNameToChrMap.clear();

        for (Chromosome c : internalChromosomes) {
            internalChrNameToChrMap.put(c.getName().toLowerCase().replaceAll("chr", ""), c);
            if (c.getName().equalsIgnoreCase("MT")) {
                internalChrNameToChrMap.put("m", c); // special case for mitochondria
            }
        }
    }

    public Chromosome getChromosomeFromName(String name) {
        return internalChrNameToChrMap.get(name.trim().toLowerCase().replaceAll("chr", ""));
    }

    public void setToSpecifiedChromosomes(Set<String> givenChromosomes) {
        if (givenChromosomes != null && givenChromosomes.size() > 0) {
            internalChromosomes = stringToChromosomes(givenChromosomes,
                    new ArrayList<Chromosome>(internalChromosomes));
            reset();
        }
    }

    public List<Chromosome> getChromosomes() {
        return new ArrayList<Chromosome>(internalChromosomes);
    }

    public double size() {
        return internalChromosomes.size();
    }


}
