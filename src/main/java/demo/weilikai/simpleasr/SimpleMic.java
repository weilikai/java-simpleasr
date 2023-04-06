package demo.weilikai.simpleasr;

import javax.sound.sampled.*;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 从麦克风读取音频流的类
 */
public class SimpleMic implements Closeable {

    private TargetDataLine targetLine;

    /**
     * 打开麦克风并设置音频格式
     *
     * @throws IOException 如果无法打开麦克风，则抛出IOException异常
     */
    void open() throws IOException {
        int sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        try {
            AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();
        } catch (LineUnavailableException e) {
            throw new IOException(e);
        }
    }

    /**
     * 从麦克风读取音频流，并将其转换为short类型的数组
     *
     * @return short类型的数组，表示从麦克风读取的音频流
     * @throws IOException 如果读取麦克风音频流时出现错误，则抛出IOException异常
     */
    short[] read() throws IOException {
        if (null == targetLine) {
            open();
        }
        byte[] buffer = new byte[25 * 16 * 2]; // 缓冲区大小
        int total = 0;
        while (total < buffer.length) {
            total += targetLine.read(buffer, total, buffer.length - total); // 从麦克风读取音频流到缓冲区
        }
        short[] shorts = new short[buffer.length / 2]; // 初始化short类型的数组，将缓冲区转换为short类型的数组
        ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
    }

    /**
     * 关闭麦克风
     *
     * @throws IOException 如果关闭麦克风时出现错误，则抛出IOException异常
     */
    @Override
    public void close() throws IOException {
        try {
            if (null == targetLine) {
                return;
            }
            targetLine.close(); // 关闭麦克风
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}