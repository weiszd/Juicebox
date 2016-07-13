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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class LittleEndianOutputStream extends FilterOutputStream {
    protected long written;

    public LittleEndianOutputStream(OutputStream out) {
        super(out);
    }

    public void write(int b) throws IOException {
        this.out.write(b);
        ++this.written;
    }

    public void write(byte[] data, int offset, int length) throws IOException {
        this.out.write(data, offset, length);
        this.written += (long) length;
    }

    public void writeBoolean(boolean b) throws IOException {
        if (b) {
            this.write(1);
        } else {
            this.write(0);
        }

    }

    public void writeByte(int b) throws IOException {
        this.out.write(b);
        ++this.written;
    }

    public void writeShort(int s) throws IOException {
        this.out.write(s & 255);
        this.out.write(s >>> 8 & 255);
        this.written += 2L;
    }

    public void writeChar(int c) throws IOException {
        this.out.write(c & 255);
        this.out.write(c >>> 8 & 255);
        this.written += 2L;
    }

    public void writeInt(int i) throws IOException {
        this.out.write(i & 255);
        this.out.write(i >>> 8 & 255);
        this.out.write(i >>> 16 & 255);
        this.out.write(i >>> 24 & 255);
        this.written += 4L;
    }

    public void writeLong(long l) throws IOException {
        this.out.write((int) l & 255);
        this.out.write((int) (l >>> 8) & 255);
        this.out.write((int) (l >>> 16) & 255);
        this.out.write((int) (l >>> 24) & 255);
        this.out.write((int) (l >>> 32) & 255);
        this.out.write((int) (l >>> 40) & 255);
        this.out.write((int) (l >>> 48) & 255);
        this.out.write((int) (l >>> 56) & 255);
        this.written += 8L;
    }

    public final void writeFloat(float f) throws IOException {
        this.writeInt(Float.floatToIntBits(f));
    }

    public final void writeDouble(double d) throws IOException {
        this.writeLong(Double.doubleToLongBits(d));
    }

    public void writeBytes(String s) throws IOException {
        int length = s.length();

        for (int i = 0; i < length; ++i) {
            this.out.write((byte) s.charAt(i));
        }

        this.written += (long) length;
    }

    public void writeString(String s) throws IOException {
        this.writeBytes(s);
        this.write(0);
    }

    public long getWrittenCount() {
        return this.written;
    }

    public void setWrittenCount(long count) {
        this.written = count;
    }
}
