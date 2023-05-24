package com.xsens.dot.android.lslstreamer.model;

import com.xsens.dot.android.sdk.events.XsensDotData;

import java.io.IOException;

import edu.ucsd.sccn.LSL;

public class XSensLSLStream {

    private int sampleTimeOverflows;
    private long lastSampleTime = 0L;

    private static final String STREAM_TYPE_FREE_ACC = "ProperAcceleration[3], SensorTimeStamp[1]";
    private static final String STREAM_TYPE_QUATS = "Quaternions[4], SensorTimeStamp[1]";

    private static final String STREAM_UID_FREE_ACC = "-acc";
    private static final String STREAM_UID_QUATS = "-quats";

    private String deviceAddress;

    private LSL.StreamOutlet accOutlet;
    private LSL.StreamOutlet quatsOutlet;

    public XSensLSLStream(String deviceAddress, String tag, double samplingRate) throws IOException {

        this.deviceAddress = deviceAddress;

        String name = "XSensDot " + tag;

        LSL.StreamInfo infoAcc = new LSL.StreamInfo(name,STREAM_TYPE_FREE_ACC,4,samplingRate,LSL.ChannelFormat.double64,"xsensdot-" + tag + STREAM_UID_FREE_ACC);
        LSL.StreamInfo infoQuats = new LSL.StreamInfo(name,STREAM_TYPE_QUATS,5,samplingRate,LSL.ChannelFormat.double64,"xsensdot-" + tag + STREAM_UID_QUATS);

        this.accOutlet = new LSL.StreamOutlet(infoAcc);
        this.quatsOutlet = new LSL.StreamOutlet(infoQuats);
        this.sampleTimeOverflows = 0;

    }

    public void close() {
        this.accOutlet.close();
        this.quatsOutlet.close();
    }

    public void pushSample(XsensDotData xsData) {

        //calculate sensor sample time -> includes overflow (if applicable), but resets for new measurements
        //https://www.xsens.com/hubfs/Downloads/Manuals/Xsens%20DOT%20User%20Manual.pdf, page 25
        long sampleTimeFine = xsData.getSampleTimeFine(); //in microseconds since sensor starts, loop every 1.2 hours (0xFFFFFFFF)
        handleOverflow(sampleTimeFine);
        double sensorSampleTime = (this.sampleTimeOverflows*0xFFFFFFFFL + sampleTimeFine) / 1000.0; //in milliseconds

        //get values from sensor and include sensor time (last entry)
        double[] freeAccWithSensorTime = assignValues(xsData.getFreeAcc(), sensorSampleTime);
        double[] quatsWithSensorTime = assignValues(xsData.getQuat(), sensorSampleTime);

        //get lsl time and push samples
        double lsltime = LSL.local_clock();

        this.accOutlet.push_sample(freeAccWithSensorTime, lsltime);
        this.quatsOutlet.push_sample(quatsWithSensorTime, lsltime);
    }

    private double[] assignValues(float[] values, double sensorSampleTime) {

        double[] output = new double[values.length+1];

        for(int i=0; i < values.length; i++){
            output[i] = values[i];
        }
        output[values.length] = sensorSampleTime;
        return output;
    }

    private void handleOverflow(long sampleTimeFine) {

        if(this.lastSampleTime - sampleTimeFine > (0xFFFFFFFFL/2)){
            //we have a overflow (check for half the buffer size to catch sending delays to msgs)
            this.sampleTimeOverflows++;
        }
        this.lastSampleTime = sampleTimeFine;
    }
}
