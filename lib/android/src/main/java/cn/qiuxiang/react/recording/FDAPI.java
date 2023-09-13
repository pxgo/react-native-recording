package cn.qiuxiang.react.recording;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;

public class FDAPI {
    int fftDataSize = 4096;
    int[] fftData;
    int fftDataIndex = 0;

    public void setFFTDataSize(int size) {
        this.fftDataSize = size;
        this.fftData = new int[size];
    }

    public int getNewFFTDataIndex() {
        if(this.fftDataIndex == this.fftDataSize - 1) {
            this.fftDataIndex = 0;
        } else {
            this.fftDataIndex += 1;
        }
        return this.fftDataIndex;
    }

    public void insertFFTData(short[] buffer) {
        int bufferSize = buffer.length;
        for(int i = 0; i < bufferSize; i++) {
            float value = buffer[i];
            int newIndex = this.getNewFFTDataIndex();
            this.fftData[newIndex] = (int) value;
        }
    }

    public WritableArray getData(short[] buffer) {
//        this.insertFFTData(buffer);
        WritableArray data = Arguments.createArray();
        data.pushInt(123);
        /*for(int i = 0; i < this.fftDataSize; i++) {
            int targetIndex = this.fftDataIndex + 1 + i;
            if(targetIndex >= this.fftDataSize) {
                targetIndex = targetIndex - this.fftDataIndex;
            }
            data.pushInt(this.fftData[targetIndex]);
        }*/
        return data;
    }
}