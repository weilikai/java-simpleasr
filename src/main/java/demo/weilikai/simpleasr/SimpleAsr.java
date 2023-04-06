package demo.weilikai.simpleasr;

import demo.weilikai.simpleasr.mfcc.MFCC;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SimpleAsr {

    int sessionIndex = 1;
    SimpleDec decoder = new SimpleDec();

    public void start() throws IOException {
        SimpleVad vad = new SimpleVad();
        Queue<short[]> queues = new LinkedList<>();

        try (SimpleMic sr = new SimpleMic()) {
            SimpleAsr.AsrSession asrSession = null;
            while (true) {
                short[] signals = sr.read();

                int vadState = vad.detectVoiceActivityFSM(signals);
                switch (vadState) {
                    case 0:
                        asrSession = null;
                        queues.offer(signals);
                        while (queues.size() > 10) {
                            queues.poll();
                        }
                        break;
                    case 1:
                        if (null == asrSession) {
                            asrSession = this.session();

                            while (queues.size() > 0) {
                                asrSession.feed(queues.poll());
                            }
                        }
                        asrSession.feed(signals);
                        break;
                    case 2:
                        asrSession.detect();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public AsrSession session() {
        String datetime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd_HHmm_ss_SSS"));
        String sessionId = String.format("s%08x_%06d_%s", hashCode(), sessionIndex++, datetime);
        System.out.println("new session: " + sessionId);
        return new AsrSession(sessionId);
    }

    protected void init(String modelFile) throws IOException {
        decoder.loadModel(modelFile);
    }

    class AsrSession {

        DataOutputStream outfile;

        List<short[]> frames = new ArrayList<>();

        public AsrSession(String sessionId) {
            try {
                outfile = new DataOutputStream(new FileOutputStream(sessionId + ".pcm"));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        public void feed(short[] signals) throws IOException {
            for (short signal : signals) {
                outfile.writeByte(signal >> 0);
                outfile.writeByte(signal >> 8);
            }
            frames.add(signals);
        }

        public void detect() throws IOException {
            outfile.close();
            System.out.println("detecting");
            MFCC m = new MFCC();
            List<double[]> mfccs = new ArrayList<>();
            for (short[] f : frames) {
                mfccs.addAll(m.mfccWithStride(f, 10));
            }
            double[][] inputMFCC = mfccs.toArray(new double[0][]);

            String ret = decoder.recognize(inputMFCC);
            System.out.println("识别结果：" + ret);
        }
    }
}
