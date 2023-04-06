package demo.weilikai.simpleasr;

import java.io.IOException;

public class AsrDemo extends SimpleAsr {
    public static void main(String[] args) throws IOException {
        SimpleAsr asr = new SimpleAsr();
        asr.init("assets/simple_asr/asr_model");
        asr.start();
    }
}
