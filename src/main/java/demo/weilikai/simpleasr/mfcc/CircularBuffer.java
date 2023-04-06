package demo.weilikai.simpleasr.mfcc;

public class CircularBuffer {

    private double[] buffer;
    private int capacity;
    private int readPosition;
    private int writePosition;
    private int markedReadPosition;
    private int markedWritePosition;

    public CircularBuffer(int capacity) {
        this.capacity = capacity;
        buffer = new double[capacity];
        readPosition = 0;
        writePosition = 0;
        markedReadPosition = -1;
        markedWritePosition = -1;
    }

    public int write(double[] data) {
        int length = data.length;
        for (int i = 0; i < length; i++) {
            buffer[writePosition] = data[i];
            writePosition = (writePosition + 1) % capacity;
            if (writePosition == readPosition) {
                readPosition = (readPosition + 1) % capacity;
            }
        }
        return writePosition;
    }

    public int read(double[] destination) {
        int length = Math.min(destination.length, available());
        for (int i = 0; i < length; i++) {
            destination[i] = buffer[readPosition];
            readPosition = (readPosition + 1) % capacity;
        }
        return length;
    }

    public void mark() {
        markedReadPosition = readPosition;
        markedWritePosition = writePosition;
    }

    public void reset() {
        if (markedReadPosition >= 0 && markedWritePosition >= 0) {
            readPosition = markedReadPosition;
            writePosition = markedWritePosition;
        } else {
            throw new IllegalStateException("Mark has not been set.");
        }
    }

    public int readPosition() {
        return readPosition;
    }

    public int writePosition() {
        return writePosition;
    }

    public int available() {
        return (writePosition - readPosition + capacity) % capacity;
    }

    public static void main(String[] args) {
        CircularBuffer circularBuffer = new CircularBuffer(5);

        double[] data = {1.0, 2.0, 3.0};
        int writePos = circularBuffer.write(data);
        System.out.println("Write Position: " + writePos);

        double[] readData = new double[2];
        int readCount = circularBuffer.read(readData);
        System.out.println("Read Count: " + readCount);
        System.out.println("Read Data: " + java.util.Arrays.toString(readData));

        circularBuffer.mark();

        double[] moreData = {4.0, 5.0, 6.0};
        circularBuffer.write(moreData);

        circularBuffer.reset();

        double[] resetReadData = new double[3];
        int resetReadCount = circularBuffer.read(resetReadData);
        System.out.println("Reset Read Count: " + resetReadCount);
        System.out.println("Reset Read Data: " + java.util.Arrays.toString(resetReadData));

        System.out.println("Read Position: " + circularBuffer.readPosition());
        System.out.println("Write Position: " + circularBuffer.writePosition());
        System.out.println("Available: " + circularBuffer.available());
    }

}
