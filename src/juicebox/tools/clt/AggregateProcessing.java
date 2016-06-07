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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.clustering.Cluster;
import org.apache.mahout.clustering.classify.WeightedVectorWritable;
import org.apache.mahout.clustering.kmeans.KMeansDriver;
import org.apache.mahout.clustering.kmeans.Kluster;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created for testing multiple CLTs at once
 * Basically scratch space
 */
class AggregateProcessing {

/*
    public static void main(String[] argv) throws IOException, CmdLineParser.UnknownOptionException, CmdLineParser.IllegalOptionValueException {

        //input data
        double[][] points = { {1, 1}, {2, 1}, {1, 2}, {2, 2}, {3, 3}, {8, 8},{9, 8}, {8, 9}, {9, 9} };

        FileSystem fs = new RawLocalFileSystem();
        Configuration conf = fs.getConf();

        //vectorize the input data and prepare the input files.
        List<Vector> vectors = new ArrayList<Vector>();
        for (int i = 0; i < points.length; i++) {
            double[] fr = points[i];
            Vector vec = new NamedVector(new RandomAccessSparseVector(fr.length), "point"+ i);
            vec.assign(fr);
            vectors.add(vec);
        }
        SequenceFile.Writer writer = new SequenceFile.Writer(fs, conf, new Path("clustering/testdata/points/file1"),
                LongWritable.class, VectorWritable.class);
        long recNum = 0;
        VectorWritable vw = new VectorWritable();
        for (Vector point : vectors) {
            vw.set(point);
            writer.append(new LongWritable(recNum++), vw);
        }
        writer.close();

        //choose two random clusters to begin with
        Path path = new Path("clustering/testdata/clusters/part-00000");
        writer = new SequenceFile.Writer(fs, conf, path, Text.class, Kluster.class);
        int k = 2;
        for (int i = 0; i < k; i++) {
            Vector vec = vectors.get(7 - i);
            Kluster cluster = new Kluster(vec, i, new EuclideanDistanceMeasure());
            writer.append(new Text(cluster.getIdentifier()), cluster);
        }
        writer.close();

        //run the k-means algorithm
        try {
            KMeansDriver.run(conf,
                    new Path("clustering/testdata/points"),
                    new Path("clustering/testdata/clusters"),
                    new Path("clustering/output"),
                    0.001, 10, true, 0.001, true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        SequenceFile.Reader reader = new SequenceFile.Reader(fs,
                new Path("clustering/output/" + Cluster.CLUSTERED_POINTS_DIR + "/part-m-0"), conf);

        IntWritable key = new IntWritable();
        WeightedPropertyVectorWritable value = new WeightedPropertyVectorWritable();
        while (reader.next(key, value)) {
            System.out.println(value.toString() + " belongs to cluster " + key.toString());
        }
        reader.close();

    }
    */


    public static final double[][] points = {{1, 1}, {2, 1}, {1, 2},
            {2, 2}, {3, 3}, {8, 8},
            {9, 8}, {8, 9}, {9, 9}};

    public static void writePointsToFile(List<Vector> points,
                                         String fileName,
                                         FileSystem fs,
                                         Configuration conf) throws IOException {
        Path path = new Path(fileName);
        SequenceFile.Writer writer = new SequenceFile.Writer(fs, conf,
                path, LongWritable.class, VectorWritable.class);
        long recNum = 0;
        VectorWritable vec = new VectorWritable();
        for (Vector point : points) {
            vec.set(point);
            writer.append(new LongWritable(recNum++), vec);
        }
        writer.close();
    }

    public static List<Vector> getPoints(double[][] raw) {
        List<Vector> points = new ArrayList<Vector>();
        for (int i = 0; i < raw.length; i++) {
            double[] fr = raw[i];
            Vector vec = new RandomAccessSparseVector(fr.length);
            vec.assign(fr);
            points.add(vec);
        }
        return points;
    }

    public static void main(String args[]) throws Exception {

        int k = 2;

        List<Vector> vectors = getPoints(points);

        File testData = new File("testdata");
        if (!testData.exists()) {
            testData.mkdir();
        }

        testData = new File("testdata/points");
        if (!testData.exists()) {
            testData.mkdir();
        }

        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(conf);
        writePointsToFile(vectors, "testdata/points/file1", fs, conf);

        Path path = new Path("testdata/clusters/part-00000");
        SequenceFile.Writer writer = new SequenceFile.Writer(fs, conf,
                path, Text.class, Kluster.class);

        for (int i = 0; i < k; i++) {
            Vector vec = vectors.get(i);
            Kluster cluster = new Kluster(vec, i, new EuclideanDistanceMeasure());
            writer.append(new Text(cluster.getIdentifier()), cluster);
        }
        writer.close();

        KMeansDriver.run(conf, new Path("testdata/points"), new Path("testdata/clusters"),
                new Path("output"), 0.001, 10,
                true, 0.001, false);

        SequenceFile.Reader reader = new SequenceFile.Reader(fs,
                new Path("output/" + Cluster.CLUSTERED_POINTS_DIR
                        + "/part-m-00000"), conf);

        IntWritable key = new IntWritable();
        WeightedVectorWritable value = new WeightedVectorWritable();

        while (reader.next(key, value)) {
            System.out.println(value.toString() + " belongs to cluster "
                    + key.toString());
        }
        reader.close();
    }
}