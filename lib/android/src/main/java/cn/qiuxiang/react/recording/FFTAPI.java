package cn.qiuxiang.react.recording;
import java.util.Arrays;

public class FFTAPI {
    int fftDataSize = 4096;
    int[] fftData = new int[fftDataSize];
    int fftDataIndex = 0;

    public void setFFTDataSize(int size) {
        this.fftDataSize = size;
    }

    public int getNewFFTDataIndex() {
        if(this.fftDataIndex == this.fftDataSize - 1) {
            this.fftDataIndex = 0;
        } else {
            this.fftDataIndex += 1;
        }
        return this.fftDataIndex;
    }

    public void insertFFTData(int []arr) {
        int arrSize = arr.length;
        for(int i = 0; i < arrSize; i++) {
            int value = arr[i];
            int newIndex = this.getNewFFTDataIndex();
            this.fftData[newIndex] = value;
        }
    }

    public int[] getFFTData() {
        int[] resultData = new int[this.fftDataSize];
        for(int i = 0; i < this.fftDataSize; i++) {
            int targetIndex = this.fftDataIndex - i;
            if(targetIndex < 0) {
                targetIndex = this.fftDataSize + targetIndex;
            }
            resultData[this.fftDataSize - 1 - i] = this.fftData[targetIndex];
        }
        return resultData;
    }
}