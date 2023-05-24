package com.xsens.dot.android.lslstreamer.model;

import com.xsens.dot.android.sdk.events.XsensDotData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.ucsd.sccn.LSL;

public class LSLManager {
    private HashMap<String, XSensLSLStream> streams;

    public LSLManager(){
        this.streams = new HashMap<>();
    }

    public void reset(){
        for(String address : this.streams.keySet()){
            this.streams.get(address).close();
        }
        this.streams = new HashMap<>();
    }


    /**
     * adds two XSensDot Stream, one for IMU data (free acceleration) and one for orientation (quaternions)<br>
     * Will override already existing streams with the same address
     * @param deviceAddress
     * @param tag
     * @param samplingRate
     */
    public void addXSensLSLStream(String deviceAddress, String tag, double samplingRate) throws IOException {

        //first make sure this stream doesn't exist or is properly closed
        if(this.streams.containsKey(deviceAddress)){
            this.streams.get(deviceAddress).close();
        }

        //create and link new stream
        XSensLSLStream stream = new XSensLSLStream(deviceAddress, tag, samplingRate);
        this.streams.put(deviceAddress, stream);
    }

    public void pushSample(String deviceAddress, XsensDotData data){

        if(this.streams.containsKey(deviceAddress)){

            //streams from the same device, they have the same clock
            //double lslClock = LSL.local_clock();
            this.streams.get(deviceAddress).pushSample(data);
        }
    }

}
