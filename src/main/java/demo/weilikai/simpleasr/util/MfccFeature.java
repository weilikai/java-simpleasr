package demo.weilikai.simpleasr.util;

import demo.weilikai.simpleasr.mfcc.MFCC;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MfccFeature {
    public static double[][] loadFeatures(File file) throws IOException {
        short[] samples = loadSamples(file);
        List<double[]> mfccs = extractFeatures(samples);
        return mfccs.toArray(new double[0][]);
    }

    public static List<double[]> extractFeatures(short[] samples) {
        MFCC m = new MFCC();
        return m.mfccWithStride(samples, 25);
    }

    public static List<double[]> extractFeatures(List<short[]> samples) {
        List<double[]> out = new ArrayList<>();
        MFCC m = new MFCC();
        for (short[] sample : samples) {
            out.addAll(m.mfccWithStride(sample, 25));
        }
        return out;
    }

    /**
     * 从文件中加载short[]数组
     *
     * @param file 要加载的文件
     * @return 读取的short[]数组
     * @throws IOException 如果无法读取文件
     */
    public static short[] loadSamples(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        short[] shortBuffer = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer);

        return shortBuffer;
    }

    /**
     * 将一个PCM音频的short数组按照每blockSize个数据一组进行分组，并将不足blockSize个的尾部数据用0进行补齐，最终返回分组后的short数组列表。
     *
     * @param pcmData 要分组的PCM音频short数组
     * @param blockSize 每组的大小
     * @return 分组后的PCM音频short数组列表
     */
    public static List<short[]> sample2Group(short[] pcmData, int blockSize) {
        final int numGroups = (pcmData.length + blockSize - 1) / blockSize; // 总共需要的组数
        final List<short[]> groups = new ArrayList<>(numGroups); // 创建存储每组数据的列表

        // 遍历每组数据
        for (int i = 0; i < numGroups; i++) {
            final int start = i * blockSize; // 计算起始索引
            final int end = Math.min(start + blockSize, pcmData.length); // 计算终止索引
            final short[] group = new short[blockSize]; // 创建一个大小为blockSize的数组

            // 将数据拷贝到group中
            System.arraycopy(pcmData, start, group, 0, end - start);

            // 不足blockSize的尾部数据补0
            if (end - start < blockSize) {
                Arrays.fill(group, end - start, blockSize, (short) 0);
            }

            groups.add(group); // 将group添加到列表中
        }

        return groups; // 返回分组后的数据列表
    }
}
