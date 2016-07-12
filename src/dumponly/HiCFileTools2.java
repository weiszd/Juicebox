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

import java.util.List;

/**
 * Created by muhammadsaadshamim on 7/12/16.
 */
class HiCFileTools2 {
    public static Dataset22 extractDatasetForCLT(List<String> files) {
        Dataset22 dataset = null;
        try {
            AbstractDatasetReader2 reader = null;
            if (files.size() == 1) {
                String magicString = DatasetReader2V3.getMagicString(files.get(0));
                if (magicString.equals("HIC")) {
                    reader = new DatasetReader2V3(files.get(0));
                } else {
                    System.err.println("This version of HIC is no longer supported");
                    System.exit(32);
                }
                dataset = reader.read();

            } else {
                if (false)
                    System.out.println("Reading summed files: " + files);
                reader = DatasetReaderFactory2.getReader(files);
                if (reader == null) {
                    System.err.println("Error while reading files");
                    System.exit(33);
                } else {
                    dataset = reader.read();
                }
            }
            MyGlobals.verifySupportedHiCFileVersion(reader.getVersion());
        } catch (Exception e) {
            System.err.println("Could not read hic file: " + e.getMessage());
            System.exit(34);
        }
        return dataset;
    }
}
