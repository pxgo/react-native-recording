package cn.qiuxiang.react.recording;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.Arguments;


public class TDAPI {
    public WritableArray getData(short buffer[]) {
        WritableArray data = Arguments.createArray();
        for(int i = 0; i < buffer.length; i ++) {
            float value = buffer[i];
            data.pushInt((int) value);
        }
        return data;
    }
}