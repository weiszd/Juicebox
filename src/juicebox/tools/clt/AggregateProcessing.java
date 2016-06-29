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


        String[] ajkhsd = {"arrowhead", "-r", "5000", "-c", "chr1",
                "/Users/muhammadsaadshamim/Desktop/LocalFiles/IMR90/intra_nofrag_30.hic",
                "/Users/muhammadsaadshamim/Desktop/LocalFiles/IMR90/arrowhead_5000_2"};

        //HiCGlobals.printVerboseComments = true;
        HiCTools.main(ajkhsd);

        ajkhsd = new String[]{"arrowhead", "-r", "10000", "-c", "1",
                "/Users/muhammadsaadshamim/Desktop/LocalFiles/IMR90/intra_nofrag_30.hic",
                "/Users/muhammadsaadshamim/Desktop/LocalFiles/IMR90/arrowhead_10000"};

        //HiCGlobals.printVerboseComments = true;
    }
}