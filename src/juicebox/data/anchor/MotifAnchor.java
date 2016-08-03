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

import juicebox.data.feature.Feature;
import juicebox.track.feature.Feature2DWithMotif;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by muhammadsaadshamim on 9/28/15.
 */
public class MotifAnchor extends Locus {

    public static boolean uniquenessShouldSupercedeConvergentRule = true;
    private static int posCount = 0;
    private static int negCount = 0;
    // critical components of a motif anchor
    // references to original features if applicable
    private final List<Feature2DWithMotif> originalFeatures1 = new ArrayList<Feature2DWithMotif>();
    private final List<Feature2DWithMotif> originalFeatures2 = new ArrayList<Feature2DWithMotif>();
    private boolean strand;
    // fimo output loaded as attributes
    private boolean fimoAttributesHaveBeenInitialized = false;
    private double score = 0, pValue, qValue;
    private String sequence;

    /**
     *
     * @param chrIndex
     * @param x1
     * @param x2
     */
    public MotifAnchor(int chrIndex, int x1, int x2) {
        super(chrIndex, x1, x2);
    }

    /**
     * Inititalize anchor given parameters (e.g. from feature list)
     *
     * @param chrIndex
     * @param x1
     * @param x2
     * @param originalFeatures1
     * @param originalFeatures2
     */
    public MotifAnchor(int chrIndex, int x1, int x2, List<Feature2DWithMotif> originalFeatures1, List<Feature2DWithMotif> originalFeatures2) {
        super(chrIndex, x1, x2);
        this.originalFeatures1.addAll(originalFeatures1);
        this.originalFeatures2.addAll(originalFeatures2);
    }


    @Override
    public Feature deepClone() {
        MotifAnchor clone = new MotifAnchor(getChr(), getX1(), getX2(), originalFeatures1, originalFeatures2);

        if (fimoAttributesHaveBeenInitialized) {
            clone.setFIMOAttributes(score, pValue, qValue, strand, sequence);
        }

        return clone;
    }

    public void mergeWith(MotifAnchor anchor) {
        if (getChr() == anchor.getChr()) {
            setX1(Math.min(getX1(), anchor.getX1()));
            setX2(Math.max(getX2(), anchor.getX2()));
            addFeatureReferencesFrom(anchor);
        } else {
            System.err.println("Attempted to merge anchors on different chromosomes");
            System.err.println(this + " & " + anchor);
        }
    }

    @Override
    public int compareTo(Locus o) {
        int baseComparison = super.compareTo(o);
        if (o instanceof MotifAnchor) {
            if (baseComparison == 0 && sequence != null && ((MotifAnchor) o).sequence != null) {
                return sequence.compareTo(((MotifAnchor) o).sequence);
            }
        }

        return baseComparison;
    }

    public void setFIMOAttributes(double score, double pValue, double qValue, boolean strand, String sequence) {
        this.score = score;
        this.pValue = pValue;
        this.qValue = qValue;
        this.strand = strand;
        this.sequence = sequence;

        fimoAttributesHaveBeenInitialized = true;
    }

    public double getScore() {
        return score;
    }

    public boolean hasFIMOAttributes() {
        return fimoAttributesHaveBeenInitialized;
    }

    public void addFIMOAttributesFrom(MotifAnchor anchor) {
        setFIMOAttributes(anchor.score, anchor.pValue, anchor.qValue, anchor.strand, anchor.sequence);
    }

    public void addFeatureReferencesFrom(MotifAnchor anchor) {
        originalFeatures1.addAll(anchor.originalFeatures1);
        originalFeatures2.addAll(anchor.originalFeatures2);
    }

    public void updateOriginalFeatures(boolean uniqueStatus, int specificStatus) {
        if ((originalFeatures1.size() > 0 || originalFeatures2.size() > 0)) {
            if (fimoAttributesHaveBeenInitialized) {
                int x1 = getX1();
                int x2 = getX2();
                if (specificStatus == 1) {
                    for (Feature2DWithMotif feature : originalFeatures1) {
                        if (strand || uniqueStatus) {
                            posCount++;
                            feature.updateMotifData(strand, uniqueStatus, sequence, x1, x2, true, score);
                        }
                    }
                } else if (specificStatus == -1) {
                    for (Feature2DWithMotif feature : originalFeatures2) {
                        if (!strand || uniqueStatus) {
                            negCount++;
                            feature.updateMotifData(strand, uniqueStatus, sequence, x1, x2, false, score);
                        }
                    }
                } else {
                    for (Feature2DWithMotif feature : originalFeatures1) {
                        if (strand || uniqueStatus) {
                            posCount++;
                            feature.updateMotifData(strand, uniqueStatus, sequence, x1, x2, true, score);
                        }
                    }
                    for (Feature2DWithMotif feature : originalFeatures2) {
                        if (!strand || uniqueStatus) {
                            negCount++;
                            feature.updateMotifData(strand, uniqueStatus, sequence, x1, x2, false, score);
                        }
                    }
                }


            } else {
                System.err.println("Attempting to assign motifs on incomplete anchor");
            }
        }
    }

    public String getSequence() {
        return sequence;
    }

    public List<Feature2DWithMotif> getOriginalFeatures1() {
        return originalFeatures1;
    }

    public List<Feature2DWithMotif> getOriginalFeatures2() {
        return originalFeatures2;
    }

    public boolean isDirectionalAnchor(boolean direction) {
        if (direction) {
            return originalFeatures1.size() > 0 && originalFeatures2.size() == 0;
        } else {
            return originalFeatures2.size() > 0 && originalFeatures1.size() == 0;
        }
    }

    /**
     * @return true if positive strand, false if negative strand
     */
    public boolean getStrand() {
        return strand;
    }
}