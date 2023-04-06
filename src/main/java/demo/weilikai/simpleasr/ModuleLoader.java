package demo.weilikai.simpleasr;

import demo.weilikai.simpleasr.util.MfccFeature;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ModuleLoader {

    /**
     * 从音频文件中提取特征
     *
     * @param file 音频文件
     * @return 返回每句话的 MFCC 特征，包含多个 MFCC 特征数组
     * @throws IOException 文件读取异常
     */
    public static List<double[][]> extractFeaturesFromAudioFile(File file) throws IOException {
        List<double[][]> mulFileFeatures = new ArrayList<>();
        SimpleVad vad = new SimpleVad();
        short[] samples = MfccFeature.loadSamples(file);
        List<short[]> frames = MfccFeature.sample2Group(samples, 400);
        Queue<short[]> queues = new LinkedList<>();
        int vadConfidence = 0;
        int sentenceIndex = 1;
        List<short[]> samplesForSingleFile = null;
        for (short[] signals : frames) {
            int vadState = vad.detectVoiceActivityFSM(signals);
            switch (vadState) {
                case 0:
                    queues.offer(signals);
                    while (queues.size() > 10) {
                        queues.poll();
                    }
                    break;
                case 1:
                    if (null == samplesForSingleFile) {
                        samplesForSingleFile = new ArrayList<>();
                        while (queues.size() > 0) {
                            samplesForSingleFile.add(queues.poll());
                        }
                    }
                    samplesForSingleFile.add(signals);
                    break;
                case 2:
                    try (DataOutputStream outfile = new DataOutputStream(new FileOutputStream("output/" + file.getName() + "" + sentenceIndex++ + ".pcm"))) {
                        for (short[] shorts : samplesForSingleFile) {
                            for (short signal : shorts) {
                                outfile.writeByte(signal >> 0);
                                outfile.writeByte(signal >> 8);
                            }
                        }
                    }

                    mulFileFeatures.add(MfccFeature.extractFeatures(samplesForSingleFile).toArray(new double[0][]));
                    samplesForSingleFile = null;
                    break;
                default:
                    break;
            }
        }
        return mulFileFeatures;
    }
}
