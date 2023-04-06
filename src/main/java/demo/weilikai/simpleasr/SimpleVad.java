package demo.weilikai.simpleasr;

/**
 * 用于检测语音活动的类
 */
public class SimpleVad {
    public static final int FRAME_LENGTH_MS = 25; // 每帧音频的长度（毫秒）
    public static final int SAMPLE_RATE = 16000; // 采样率（赫兹）
    public static final double ENERGY_THRESHOLD_DB = -40; // 能量阈值（分贝）

    private int state = 0;
    private int voiceActivityCounter = 0;
    private int silenceCounter = 0;

    /**
     * 检测语音活动的有限状态机（FSM）方法
     *
     * @param audioData 从麦克风读取的音频数据
     * @return 检测到的语音活动状态（0表示等待用户说话，1表示用户正在说话，2表示用户说话结束）
     */
    public int detectVoiceActivityFSM(short[] audioData) {
        double energyDB = calculateDecibel(audioData);
        boolean voiceActivity = energyDB > ENERGY_THRESHOLD_DB;

        switch (state) {
            case 0: // 等待用户说话
                if (voiceActivity) {
                    voiceActivityCounter++;
                    if (voiceActivityCounter > 4) { // 连续 4 帧语音活动，认为用户开始说话
                        state = 1;
                        voiceActivityCounter = 0;
                    }
                } else {
                    voiceActivityCounter = 0;
                }
                break;
            case 1: // 用户正在说话
                if (!voiceActivity) {
                    silenceCounter++;
                    if (silenceCounter > 8) { // 连续 8 帧无语音活动，认为用户说话结束
                        state = 2;
                        silenceCounter = 0;
                    }
                } else {
                    silenceCounter = 0;
                }
                break;
            case 2: // 用户说话结束
                // 该状态下不执行任何操作，你可以重置状态机以便于再次检测用户的说话，或者执行其他操作。
                state = 0;
                voiceActivityCounter = 0;
                break;
        }
        System.out.print("VAD State: " + state + "" + ", 音量分贝: " + energyDB + ", 人声：" + voiceActivity + ", \r");


        return state;
    }

    /**
     * 检测音频数据中是否存在语音活动的方法
     *
     * @param audioData 音频数据
     * @return 如果检测到语音活动则返回true，否则返回false
     */
    public static boolean detectVoiceActivity(short[] audioData) {
        int frameLengthSamples = FRAME_LENGTH_MS * SAMPLE_RATE / 1000;
        if (frameLengthSamples != audioData.length) {
            throw new IllegalArgumentException("audioData的长度必须是400，即25ms x 16");
        }
        double energyDB = calculateDecibel(audioData);
        return energyDB > ENERGY_THRESHOLD_DB;
    }

    /**
     * 计算PCM音频数据的分贝值
     *
     * @param samples PCM音频数据
     * @return 分贝值
     */
    public static double calculateDecibel(short[] samples) {
        double sum = 0;
        for (short s : samples) {
            sum += Math.pow(s, 2);
        }

        double rms = Math.sqrt(sum / samples.length);
        double db = 20 * Math.log10(rms / Short.MAX_VALUE);
        return db;
    }
}
