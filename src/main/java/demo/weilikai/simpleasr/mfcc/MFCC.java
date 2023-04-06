package demo.weilikai.simpleasr.mfcc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class MFCC {

    /**
     * 自动处理步长的MFCC函数
     */
    public List<double[]> mfccWithStride(short[] data, int stride) {
        double[] doubleData = IntStream.range(0, data.length).mapToDouble(i -> (double) (data[i])).toArray();
        return mfccWithStride(doubleData, stride);
    }

    CircularBuffer mBuffer = new CircularBuffer(1024 * 1024);

    private List<double[]> mfccWithStride(double[] data, int stride) {
        List<double[]> out = new ArrayList<>();
        mBuffer.write(data);
        while (mBuffer.available() > 400) {
            mBuffer.mark();
            double[] frame = new double[400];
            mBuffer.read(frame);
            double[] mfcc = mfcc(frame);
            out.add(mfcc);
            mBuffer.reset();
            mBuffer.read(new double[10 * 16]);
        }
        return out;
    }

    private double[] mfcc(double[] frame) {
        // 1. 预加重
        double[] preEmphasizedSignal = step1ApplyPreEmphasis(frame, 0.97f);

        // 2. 加窗
        double[] windowedSignal = step2ApplyWindowing(preEmphasizedSignal);

        // 3. FFT，原始信号的长度为400（每帧25ms在16k采样时有400个采样点)，故需要先将其扩至512的长度
        double[] windowedSignal512 = new double[512];
        System.arraycopy(windowedSignal, 0, windowedSignal512, 0, windowedSignal.length);
        double[] fftEnergies = step3ComputeFft(windowedSignal512);

        // 4. 使用梅尔滤波器进行处理
        double[] melSpectrum = step4MelFilterBankProcessing(fftEnergies);

        // 5. 取对数（使其更加符合人耳听觉结构）、执行离散余弦变换 (DCT)
        // 6. 保留所需数量的MFCC系数
        double[] logMelSpectrum = step5LogCompression(melSpectrum);
        double[] mfcc = step6DiscreteCosineTransform(logMelSpectrum, 13);
        return mfcc;
    }

    private double[] step1ApplyPreEmphasis(double[] signal, double alpha) {
        double[] preEmphasizedSignal = new double[signal.length];
        preEmphasizedSignal[0] = signal[0];

        for (int i = 1; i < signal.length; i++) {
            preEmphasizedSignal[i] = signal[i] - alpha * signal[i - 1];
        }

        return preEmphasizedSignal;
    }

    private double[] step2ApplyWindowing(double[] signal) {
        int frameSize = 400; // 25ms at 16kHz
        double[] windowedSignal = new double[frameSize];

        // 加汉明窗
        for (int n = 0; n < frameSize; n++) {
            windowedSignal[n] = signal[n] * (0.54 - 0.46 * Math.cos((2 * Math.PI * n) / (frameSize - 1)));
        }

        return windowedSignal;
    }

    private double[] step3ComputeFft(double[] s2512) {
        return FFT.computeFftEnergies(s2512);
    }

    private double[] step4MelFilterBankProcessing(double[] s3) {
        return MelFilterBank.processing(s3);
    }

    private double[] step5LogCompression(double[] melEnergy) {
        int numFilters = melEnergy.length;
        double[] logMelEnergy = new double[numFilters];

        for (int i = 0; i < numFilters; i++) {
            logMelEnergy[i] = Math.log10(melEnergy[i]);
        }

        return logMelEnergy;
    }

    private double[] step6DiscreteCosineTransform(double[] logMelEnergy, int numCoefficients) {
        int numFilters = logMelEnergy.length;
        double[] mfcc = new double[numCoefficients];

        for (int k = 0; k < numCoefficients; k++) {
            double sum = 0;
            for (int n = 0; n < numFilters; n++) {
                double angle = (Math.PI / numFilters) * (n + 0.5) * k;
                sum += logMelEnergy[n] * Math.cos(angle);
            }
            mfcc[k] = sum * Math.sqrt(2.0 / numFilters);
        }

        return mfcc;
    }

}