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

import java.io.IOException;
import java.util.List;

class HiCFileTools2 {
    public static Dataset22 extractDatasetForCLT(List<String> files) {
        Dataset22 dataset = null;
        try {
            DatasetReader2V3 reader = null;
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
                reader = getReader(files);
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

    public static DatasetReader2V3 getReader(List<String> fileList) throws IOException {

        if (fileList.size() == 1) {
            String file = fileList.get(0);
            return getReaderForFile(file);
        } else {
            System.err.println("This simplified reader does not support combined datasets." +
                    " Please merge the maps into a megamap first.");
            System.exit(100);
            return null;
        }
    }

    private static DatasetReader2V3 getReaderForFile(String file) throws IOException {
        String magicString = DatasetReader2V3.getMagicString(file);

        if (magicString != null) {
            if (magicString.equals("HIC")) {
                return new DatasetReader2V3(file);
            } else {
                System.err.println("This version is deprecated and is no longer supported.");
            }
        }
        return null;
    }
}
