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

public class FloatArrayList {
    private transient float[] elements;
    private int size;

    public FloatArrayList() {
        this(100);
    }

    public FloatArrayList(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        } else {
            this.elements = new float[initialCapacity];
        }
    }

    public void add(float e) {
        if (this.size + 1 >= this.elements.length) {
            this.grow();
        }

        this.elements[this.size++] = e;
    }

    private void grow() {
        int oldCapacity = this.elements.length;
        int newCapacity;
        if (oldCapacity < 10000000) {
            newCapacity = oldCapacity * 2;
        } else {
            newCapacity = oldCapacity * 3 / 2 + 1;
        }

        float[] tmp = new float[newCapacity];
        System.arraycopy(this.elements, 0, tmp, 0, this.elements.length);
        this.elements = tmp;
    }

    public float[] toArray() {
        this.trimToSize();
        return this.elements;
    }

    private void trimToSize() {
        int oldCapacity = this.elements.length;
        if (this.size < oldCapacity) {
            float[] tmp = new float[this.size];
            System.arraycopy(this.elements, 0, tmp, 0, this.size);
            this.elements = tmp;
        }

    }
}
