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

package dumponly.basics;

import java.io.*;

public class LittleEndianInputStream extends FilterInputStream {
    byte[] buffer = new byte[8];

    public LittleEndianInputStream(InputStream in) {
        super(in);
    }

    public byte readByte() throws IOException {
        int ch = this.in.read();
        if (ch < 0) {
            throw new EOFException();
        } else {
            return (byte) ch;
        }
    }

    public short readShort() throws IOException {
        int byte1 = this.in.read();
        int byte2 = this.in.read();
        if (byte2 < 0) {
            throw new EOFException();
        } else {
            return (short) ((byte2 << 24 >>> 16) + (byte1 << 24 >>> 24));
        }
    }

    public int readInt() throws IOException {
        int byte1 = this.in.read();
        int byte2 = this.in.read();
        int byte3 = this.in.read();
        int byte4 = this.in.read();
        if (byte4 < 0) {
            throw new EOFException();
        } else {
            return (byte4 << 24) + (byte3 << 24 >>> 8) + (byte2 << 24 >>> 16) + (byte1 << 24 >>> 24);
        }
    }

    public long readLong() throws IOException {
        this.readFully(this.buffer);
        long byte1 = (long) this.buffer[0];
        long byte2 = (long) this.buffer[1];
        long byte3 = (long) this.buffer[2];
        long byte4 = (long) this.buffer[3];
        long byte5 = (long) this.buffer[4];
        long byte6 = (long) this.buffer[5];
        long byte7 = (long) this.buffer[6];
        long byte8 = (long) this.buffer[7];
        return (byte8 << 56) + (byte7 << 56 >>> 8) + (byte6 << 56 >>> 16) + (byte5 << 56 >>> 24) + (byte4 << 56 >>> 32) + (byte3 << 56 >>> 40) + (byte2 << 56 >>> 48) + (byte1 << 56 >>> 56);
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(this.readLong());
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(this.readInt());
    }

    public String readString() throws IOException {
        ByteArrayOutputStream bis = new ByteArrayOutputStream(100);

        byte b;
        while ((b = (byte) this.in.read()) != 0) {
            if (b < 0) {
                throw new EOFException();
            }

            bis.write(b);
        }

        return new String(bis.toByteArray());
    }

    private void readFully(byte[] b) throws IOException {
        int len = b.length;
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        } else {
            int count;
            for (int n = 0; n < len; n += count) {
                count = this.read(b, n, len - n);
                if (count < 0) {
                    throw new EOFException();
                }
            }

        }
    }
}
