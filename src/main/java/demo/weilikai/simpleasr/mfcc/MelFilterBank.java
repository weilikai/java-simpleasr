package demo.weilikai.simpleasr.mfcc;

final class MelFilterBank {
    private final int numFilters;
    private final double lowerFrequency;
    private final double upperFrequency;
    private final int numFftBins;
    private final double sampleRate;

    public MelFilterBank() {
        this(26, 0, 8000, 512, 16000);
    }

    public MelFilterBank(int numFilters, double lowerFrequency, double upperFrequency, int numFftBins, double sampleRate) {
        this.numFilters = numFilters;
        this.lowerFrequency = lowerFrequency;
        this.upperFrequency = upperFrequency;
        this.numFftBins = numFftBins;
        this.sampleRate = sampleRate;
    }

    static MelFilterBank DEFAULT_MEL_FILTER_BACK = new MelFilterBank();

    public static double[] processing(double[] s3) {
        return DEFAULT_MEL_FILTER_BACK.process(s3);
    }

    public double[] process(double[] energySpectrum) {
        double[] melEnergy = new double[numFilters];
        double[] melFrequencies = melSpace(lowerFrequency, upperFrequency, numFilters + 2);
        double[] fftFrequencies = fftSpace(numFftBins, sampleRate);

        for (int i = 1; i <= numFilters; i++) {
            double[] filter = createFilter(fftFrequencies, melFrequencies, i);
            for (int j = 0; j < numFftBins; j++) {
                melEnergy[i - 1] += energySpectrum[j] * filter[j];
            }
        }

        return melEnergy;
    }

    private double[] createFilter(double[] fftFrequencies, double[] melFrequencies, int filterIndex) {
        double[] filter = new double[numFftBins];
        for (int i = 0; i < numFftBins; i++) {
            double m = (fftFrequencies[i] - melFrequencies[filterIndex - 1]) / (melFrequencies[filterIndex] - melFrequencies[filterIndex - 1]);
            double n = (melFrequencies[filterIndex + 1] - fftFrequencies[i]) / (melFrequencies[filterIndex + 1] - melFrequencies[filterIndex]);
            filter[i] = Math.max(0, Math.min(m, n));
        }
        return filter;
    }

    private double[] melSpace(double lowerFrequency, double upperFrequency, int numPoints) {
        double[] melPoints = new double[numPoints];
        double lowerMel = frequencyToMel(lowerFrequency);
        double upperMel = frequencyToMel(upperFrequency);
        double melIncrement = (upperMel - lowerMel) / (numPoints - 1);
        for (int i = 0; i < numPoints; i++) {
            melPoints[i] = melToFrequency(lowerMel + i * melIncrement);
        }
        return melPoints;
    }

    private double[] fftSpace(int numFftBins, double sampleRate) {
        double[] fftFrequencies = new double[numFftBins];
        for (int i = 0; i < numFftBins; i++) {
            fftFrequencies[i] = i * sampleRate / (2 * numFftBins);
        }
        return fftFrequencies;
    }

    private double frequencyToMel(double frequency) {
        return 2595.0 * Math.log10(1.0 + frequency / 700.0);
    }

    private double melToFrequency(double mel) {
        return 700.0 * (Math.pow(10, mel / 2595.0) - 1.0);
    }
}

