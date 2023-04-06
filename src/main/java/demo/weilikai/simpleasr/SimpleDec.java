package demo.weilikai.simpleasr;

import demo.weilikai.simpleasr.util.MfccFeature;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class SimpleDec {

    Map<Integer, String> mId2Word = new HashMap<>();
    List<double[][]> mModelFeatures = new ArrayList<>();

    void loadModel(String modelFile) throws IOException {
        System.out.println("正在加载模型资源...");
        File modelDir = new File(modelFile);

        ArrayList<String> words = new ArrayList<>();
        for (File file : Objects.requireNonNull(modelDir.listFiles())) {
            String word = file.getName().replaceAll("\\..*$", "");
            List<double[][]> features = ModuleLoader.extractFeaturesFromAudioFile(file);
            for (double[][] feature : features) {
                mModelFeatures.add(feature);
                mId2Word.put(mId2Word.size(), word);
            }
            System.out.println("  " + word + ", 特征数：" + features.size());
            words.add(word);
        }
        System.out.println("加载完毕, 支持说法：" + words);
    }


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
    /**
     * 计算两个向量之间的欧几里得距离
     *
     * @param a 向量a（x1,y1,z1）
     * @param b 向量b (x2,y2,z2)
     * @return 向量a和向量b之间的欧几里得距离
     */
    private double calculateEuclideanDistance(double[] a, double[] b) {
        double distance = 0;
        for (int i = 0; i < a.length; i++) {
            distance += (a[i] - b[i]) * (a[i] - b[i]); // 计算两个向量在每个维度上的差值的平方
        }
        return Math.sqrt(distance); // 返回差值平方和的平方根
    }

    /**
     * 计算两个MFCC序列之间的DTW距离
     *
     * @param mfcc1 第一个MFCC序列
     * @param mfcc2 第二个MFCC序列
     * @return 两个MFCC序列之间的DTW距离
     */
    private double dtwDistance(double[][] mfcc1, double[][] mfcc2) {
        int len1 = mfcc1.length; // 获取mfcc1序列的长度
        int len2 = mfcc2.length; // 获取mfcc2序列的长度
        double[][] dtw = new double[len1][len2]; // 初始化DTW矩阵

        // 初始化DTW矩阵的第一个元素，它等于mfcc1和mfcc2的第一个MFCC向量之间的欧几里得距离
        dtw[0][0] = calculateEuclideanDistance(mfcc1[0], mfcc2[0]);

        // 初始化DTW矩阵的第一列
        for (int i = 1; i < len1; i++) {
            dtw[i][0] = dtw[i - 1][0] + calculateEuclideanDistance(mfcc1[i], mfcc2[0]);
        }

        // 初始化DTW矩阵的第一行
        for (int j = 1; j < len2; j++) {
            dtw[0][j] = dtw[0][j - 1] + calculateEuclideanDistance(mfcc1[0], mfcc2[j]);
        }

        // 使用动态规划填充DTW矩阵的剩余部分
        for (int i = 1; i < len1; i++) {
            for (int j = 1; j < len2; j++) {
                // 计算当前MFCC向量之间的欧几里得距离
                double cost = calculateEuclideanDistance(mfcc1[i], mfcc2[j]);
                // 当前单元格的值等于当前距离加上左上、上和左单元格中的最小值
                dtw[i][j] = cost + Math.min(Math.min(dtw[i - 1][j], dtw[i][j - 1]), dtw[i - 1][j - 1]);
            }
        }

        // DTW矩阵的右下角包含两个序列之间的DTW距离
        return dtw[len1 - 1][len2 - 1];
    }

    /**
     * 识别给定MFCC序列与一组模板序列中的哪一个模板最匹配
     *
     * @param inputFeatures 待识别的MFCC序列
     * @return 最匹配的模板序列名称，格式为“Word_1”、“Word_2”等
     */
    public String recognize(double[][] inputFeatures) {
        List<double[][]> templates = mModelFeatures;
        double[] distances = new double[templates.size()];

        // 遍历所有模板序列
        for (int i = 0; i < templates.size(); i++) {
            // 计算当前模板序列与待识别MFCC序列之间的DTW距离
            distances[i] = dtwDistance(templates.get(i), inputFeatures);
        }

        Map<String, Score> scores = new HashMap<>();
        for (int i = 0; i < distances.length; i++) {
            String word = mId2Word.get(i);
            scores.putIfAbsent(word, new Score(word));
            scores.get(word).scores.add(distances[i]);
        }
        List<Score> results = scores.values().stream().sorted().collect(Collectors.toList());
        for (Score result : results) {
            System.out.printf("score: %s, word: %s%n", result.getScore(), result.word);
        }
        ;
        if (results.size() > 0 && !reject(results.stream().mapToDouble(Score::getScore).toArray())) {
            Score best1 = results.get(0);
            System.out.printf("best1: score: %s, word: %s%n", best1.getScore(), best1.word);
            return best1.word;
        } else {
            System.out.printf("best1: score: %s, word: %s%n", null, null);
            return null;
        }
    }

    boolean reject(double[] scores) {
        // 计算标准差，根据标准差评估是否应当拒识
        double sum = Arrays.stream(scores).sum();
        double mean = sum / scores.length;
        double mu2 = Arrays.stream(scores).map(s -> Math.pow(s - mean, 2)).sum() / scores.length;
        double mu = Math.sqrt(mu2);
        double range = scores[scores.length - 1] - scores[0];
        boolean reject = mu < 50 || range < 100;
        System.out.println("是否拒识：" + reject + ", 标准差: " + mu + ", 极差: " + range);
        return reject;
    }

    class Score implements Comparable<Score> {
        String word;
        List<Double> scores = new ArrayList<>();

        public Score(String word) {
            this.word = word;
        }

        public double getScore() {
            double sum = 0;
            for (Double score : scores) {
                sum += score;
            }
            return sum / scores.size();
        }

        @Override
        public int compareTo(Score o) {
            return Double.compare(this.getScore(), o.getScore());
        }
    }
}

