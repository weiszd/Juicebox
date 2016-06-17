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

package juicebox.tools.clt;

import jargs.gnu.CmdLineParser;
import juicebox.tools.HiCTools;

import java.io.IOException;

/**
 * Created for testing multiple CLTs at once
 * Basically scratch space
 */
class AggregateProcessing {


    public static void main(String[] argv) throws IOException, CmdLineParser.UnknownOptionException, CmdLineParser.IllegalOptionValueException {

        String[] ll51231123 = {"arrowhead", "-c", "1",
                "/Users/muhammadsaadshamim/Desktop/LocalFiles/snyder/A/A_inter_30.hic",
                "/Users/muhammadsaadshamim/Desktop/LocalFiles/snyder/A/temp"};
/*
        ll51231123 = new String[]{"motifs","hg19",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/motif_finder2/k562/bed",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/motif_finder2/k562/combined_peaks.txt",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/yy1_fimo.txt"};

        HiCTools.main(ll51231123);

        ll51231123 = new String[]{"motifs","hg19",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/motif_finder2/gm12878/bed",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/motif_finder2/gm12878/combined_peaks.txt",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/yy1_fimo.txt"};

        HiCTools.main(ll51231123);

        ll51231123 = new String[]{"compare","2","hg19",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/motif_finder2/k562/combined_peaks_with_motifs.txt",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/motif_finder2/k562/combined_peaks_with_motifs.txt"};

        //HiCGlobals.printVerboseComments = true;
        HiCTools.main(ll51231123);
*/
        ll51231123 = new String[]{"compare", "2", "hg19",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/geo_gm12878_combined_peaks_with_motifs.txt",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/geo_gm12878_combined_peaks_with_motifs.txt"};

        HiCTools.main(ll51231123);
/*
        ll51231123 = new String[]{"compare","2","hg19",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/motif_finder/k562/geok_common_yy1",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/motif_finder/k562/geok_common_yy1"};

        HiCTools.main(ll51231123);

        ll51231123 = new String[]{"compare","2","hg19",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/motif_finder2/k562/geok_common_yy1",
                "/Users/muhammadsaadshamim/Desktop/yy1_analysis/motif_finder2/k562/geok_common_yy1"};

        HiCTools.main(ll51231123);
        */
    }
}