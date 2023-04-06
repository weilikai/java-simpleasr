package demo.weilikai.simpleasr.mfcc;

public class FFT {

    public static Complex[] fft(double[] x) {
        int n = x.length;

        // Base case: a single sample, mfcc.FFT is the sample itself
        if (n == 1) {
            return new Complex[]{new Complex(x[0], 0)};
        }

        // Check if the input length is a power of 2
        if (Integer.bitCount(n) != 1) {
            throw new IllegalArgumentException("Input length must be a power of 2");
        }

        // Split input into even and odd parts
        double[] even = new double[n / 2];
        double[] odd = new double[n / 2];
        for (int i = 0; i < n / 2; i++) {
            even[i] = x[2 * i];
            odd[i] = x[2 * i + 1];
        }

        // Compute mfcc.FFT of even and odd parts
        Complex[] evenFFT = fft(even);
        Complex[] oddFFT = fft(odd);

        // Combine even and odd mfcc.FFT results
        Complex[] result = new Complex[n];
        for (int i = 0; i < n / 2; i++) {
            double theta = -2 * Math.PI * i / n;
            Complex t = new Complex(Math.cos(theta), Math.sin(theta)).multiply(oddFFT[i]);
            result[i] = evenFFT[i].add(t);
            result[i + n / 2] = evenFFT[i].subtract(t);
        }

        return result;
    }

    public static double[] computeFftEnergies(double[] x) {
        return computeEnergies(fft(x));
    }

    public static double[] computeEnergies(Complex[] fftResult) {
        int n = fftResult.length;
        double[] energy = new double[n];

        for (int i = 0; i < n; i++) {
            Complex c = fftResult[i];
            energy[i] = c.energy();
        }

        return energy;
    }


    public static class Complex {
        private final double real;
        private final double imaginary;

        public Complex(double real, double imaginary) {
            this.real = real;
            this.imaginary = imaginary;
        }

        public Complex add(Complex other) {
            double newReal = this.real + other.real;
            double newImaginary = this.imaginary + other.imaginary;
            return new Complex(newReal, newImaginary);
        }

        public Complex subtract(Complex other) {
            double newReal = this.real - other.real;
            double newImaginary = this.imaginary - other.imaginary;
            return new Complex(newReal, newImaginary);
        }

        public Complex multiply(Complex other) {
            double newReal = this.real * other.real - this.imaginary * other.imaginary;
            double newImaginary = this.real * other.imaginary + this.imaginary * other.real;
            return new Complex(newReal, newImaginary);
        }

        public double energy() {
            Complex c = this;
            double energy = c.real * c.real + c.imaginary * c.imaginary;
            return energy;
        }
    }
}
