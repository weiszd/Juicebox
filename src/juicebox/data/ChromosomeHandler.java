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

import org.broad.igv.feature.Chromosome;

import java.util.*;

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
        internalChromosomes = HiCFileTools.stringToChromosomes(givenChromosomes,
                new ArrayList<Chromosome>(internalChromosomes));
        reset();
    }

    public Collection<Chromosome> getChromosomes() {
        return internalChromosomes;
    }

    public double size() {
        return internalChromosomes.size();
    }
}
