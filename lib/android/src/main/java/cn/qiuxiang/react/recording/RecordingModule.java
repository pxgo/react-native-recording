package cn.qiuxiang.react.recording;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

class RecordingModule extends ReactContextBaseJavaModule {
    private static AudioRecord audioRecord;
    private final ReactApplicationContext reactContext;
    private DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;
    private boolean running;
    private int bufferSize;
    private Thread recordingThread;

    private int fftDataSize;
    int[] fftData;
    int fftDataIndex;


    RecordingModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "Recording";
    }

    @ReactMethod
    public void init(ReadableMap options) {
        if (eventEmitter == null) {
            eventEmitter = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        }

        if (running || (recordingThread != null && recordingThread.isAlive())) {
            return;
        }

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }

        // for parameter description, see
        // https://developer.android.com/reference/android/media/AudioRecord.html

        int sampleRateInHz = 44100;
        if (options.hasKey("sampleRate")) {
            sampleRateInHz = options.getInt("sampleRate");
        }

        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        if (options.hasKey("channelsPerFrame")) {
            int channelsPerFrame = options.getInt("channelsPerFrame");

            // every other case --> CHANNEL_IN_MONO
            if (channelsPerFrame == 2) {
                channelConfig = AudioFormat.CHANNEL_IN_STEREO;
            }
        }

        // we support only 8-bit and 16-bit PCM
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        if (options.hasKey("bitsPerChannel")) {
            int bitsPerChannel = options.getInt("bitsPerChannel");

            if (bitsPerChannel == 8) {
                audioFormat = AudioFormat.ENCODING_PCM_8BIT;
            }
        }

        if (options.hasKey("bufferSize")) {
            this.bufferSize = options.getInt("bufferSize");
        } else {
            this.bufferSize = 8192;
        }

        if(options.hasKey("fftBufferSize")) {
            this.fftDataSize = options.getInt("fftBufferSize");
            this.fftData = new int[this.fftDataSize];
            this.fftDataIndex = 0;
        } else {
            this.fftDataSize = 0;
            this.fftDataIndex = 0;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRateInHz,
                channelConfig,
                audioFormat,
                this.bufferSize * 2);

        recordingThread = new Thread(new Runnable() {
            public void run() {
                recording();
            }
        }, "RecordingThread");
    }

    @ReactMethod
    public void start() {
        if (!running && audioRecord != null && recordingThread != null) {
            running = true;
            audioRecord.startRecording();
            recordingThread.start();
        }
    }

    @ReactMethod
    public void stop() {
        if (audioRecord != null) {
            running = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    private WritableArray getTDData(short[] buffer) {
        WritableArray data = Arguments.createArray();
        for(int i = 0; i < buffer.length; i ++) {
            float value = buffer[i];
            data.pushInt((int) value);
        }
        return data;
    }

    private int getNewFFTDataIndex() {
        if(this.fftDataIndex == this.fftDataSize - 1) {
            this.fftDataIndex = 0;
        } else {
            this.fftDataIndex += 1;
        }
        return this.fftDataIndex;
    }

    private void insertFFTData(short[] buffer) {
        int bufferLength = buffer.length;
        for(int i = 0; i < bufferLength; i++) {
            float value = buffer[i];
            int newIndex = this.getNewFFTDataIndex();
            this.fftData[newIndex] = (int) value;
        }
    }

    private WritableArray getFDData(short[] buffer) {
        this.insertFFTData(buffer);
        WritableArray data = Arguments.createArray();
        for(int i = 0; i < this.fftDataSize; i++) {
            int targetIndex = this.fftDataIndex + 1 + i;
            if(targetIndex >= this.fftDataSize) {
                targetIndex = targetIndex - this.fftDataIndex;
            }
            data.pushInt(this.fftData[targetIndex]);
        }
        return this.calculateEnergy(data);
    }

    public static int[] calculateEnergy(int[] data) {
        int length = data.length;
        int halfLength = length / 2;
        int[] energy = new int[halfLength];

        // 将 int 数组转换为复数数组
        Complex[] complexArray = new Complex[length];
        for (int i = 0; i < length; i++) {
            complexArray[i] = new Complex(data[i], 0);
        }

        // 执行 FFT
        FastFourierTransformer transformer = new FastFourierTransformer();
        Complex[] transformedArray = transformer.transform(complexArray, TransformType.FORWARD);

        // 计算能量
        for (int i = 0; i < halfLength; i++) {
            double real = transformedArray[i].getReal();
            double imaginary = transformedArray[i].getImaginary();
            double energyValue = Math.sqrt(real * real + imaginary * imaginary);
            energy[i] = (int) Math.round(energyValue);
        }

        return energy;
    }

    private void recording() {
        short buffer[] = new short[bufferSize];
        while (running && !reactContext.getCatalystInstance().isDestroyed()) {
            audioRecord.read(buffer, 0, bufferSize);
            if(this.fftDataSize > 0) {
                WritableArray data = this.getFDData(buffer);
                eventEmitter.emit("recordingFFT", data);
            } else {
                WritableArray data = this.getTDData(buffer);
                eventEmitter.emit("recording", data);
            }
        }
    }
}
