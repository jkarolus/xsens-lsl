//  Copyright (c) 2003-2020 Xsens Technologies B.V. or subsidiaries worldwide.
//  All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without modification,
//  are permitted provided that the following conditions are met:
//
//  1.      Redistributions of source code must retain the above copyright notice,
//           this list of conditions, and the following disclaimer.
//
//  2.      Redistributions in binary form must reproduce the above copyright notice,
//           this list of conditions, and the following disclaimer in the documentation
//           and/or other materials provided with the distribution.
//
//  3.      Neither the names of the copyright holders nor the names of their contributors
//           may be used to endorse or promote products derived from this software without
//           specific prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
//  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
//  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
//  THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//  SPECIAL, EXEMPLARY OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
//  OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
//  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY OR
//  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
//  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.THE LAWS OF THE NETHERLANDS
//  SHALL BE EXCLUSIVELY APPLICABLE AND ANY DISPUTES SHALL BE FINALLY SETTLED UNDER THE RULES
//  OF ARBITRATION OF THE INTERNATIONAL CHAMBER OF COMMERCE IN THE HAGUE BY ONE OR MORE
//  ARBITRATORS APPOINTED IN ACCORDANCE WITH SAID RULES.
//

package com.xsens.dot.android.lslstreamer.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xsens.dot.android.lslstreamer.BuildConfig;
import com.xsens.dot.android.lslstreamer.R;
import com.xsens.dot.android.lslstreamer.adapters.DataAdapter;
import com.xsens.dot.android.lslstreamer.databinding.FragmentDataBinding;
import com.xsens.dot.android.lslstreamer.interfaces.DataChangeInterface;
import com.xsens.dot.android.lslstreamer.interfaces.StreamingClickInterface;
import com.xsens.dot.android.lslstreamer.model.LSLManager;
import com.xsens.dot.android.lslstreamer.viewmodels.SensorViewModel;
import com.xsens.dot.android.sdk.events.XsensDotData;
import com.xsens.dot.android.sdk.interfaces.XsensDotMeasurementCallback;
import com.xsens.dot.android.sdk.interfaces.XsensDotSyncCallback;
import com.xsens.dot.android.sdk.models.FilterProfileInfo;
import com.xsens.dot.android.sdk.models.XsensDotDevice;
import com.xsens.dot.android.sdk.models.XsensDotSyncManager;
import com.xsens.dot.android.sdk.utils.XsensDotLogger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.xsens.dot.android.lslstreamer.adapters.DataAdapter.KEY_ADDRESS;
import static com.xsens.dot.android.lslstreamer.adapters.DataAdapter.KEY_DATA;
import static com.xsens.dot.android.lslstreamer.adapters.DataAdapter.KEY_TAG;
import static com.xsens.dot.android.lslstreamer.views.MainActivity.FRAGMENT_TAG_DATA;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.HEADING_FAILURE;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.HEADING_STATUS_XRM_DEFAULT_ALIGNMENT;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.HEADING_STATUS_XRM_HEADING;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.HEADING_STATUS_XRM_NONE;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.HEADING_SUCCESS;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.LOG_STATE_ON;
import static com.xsens.dot.android.sdk.models.XsensDotDevice.PLOT_STATE_ON;
import static com.xsens.dot.android.sdk.models.XsensDotPayload.PAYLOAD_TYPE_COMPLETE_EULER;
import static com.xsens.dot.android.sdk.models.XsensDotPayload.PAYLOAD_TYPE_COMPLETE_QUATERNION;

/**
 * A fragment for presenting the data and storing to file.
 */
public class DataFragment extends Fragment implements StreamingClickInterface, DataChangeInterface, XsensDotSyncCallback, XsensDotMeasurementCallback {

    private static final String TAG = DataFragment.class.getSimpleName();

    // The code of request
    private static final int SYNCING_REQUEST_CODE = 1001;

    // The keys of HashMap
    public static final String KEY_LOGGER = "logger";

    // The view binder of DataFragment
    private FragmentDataBinding mBinding;

    // The devices view model instance
    private SensorViewModel mSensorViewModel;

    // The adapter for data item
    private DataAdapter mDataAdapter;

    // A list contains tag and data from each sensor
    private ArrayList<HashMap<String, Object>> mDataList = new ArrayList<>();

    // A list contains mac address and XsensDotLogger object.
    private List<HashMap<String, Object>> mLoggerList = new ArrayList<>();

    // A variable for data logging flag
    private boolean mIsLogging = false;

    // A dialog during the synchronization
    private AlertDialog mSyncingDialog;

    private LSLManager mlslManager;

    private PowerManager.WakeLock mWakeLock;

    /**
     * Get the instance of DataFragment
     *
     * @return The instance of DataFragment
     */
    public static DataFragment newInstance() {

        return new DataFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {

        super.onAttach(context);

        bindViewModel();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        mBinding = FragmentDataBinding.inflate(LayoutInflater.from(getContext()));
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        mSensorViewModel.setStates(PLOT_STATE_ON, LOG_STATE_ON);

        mDataAdapter = new DataAdapter(getContext(), mDataList);
        mBinding.dataRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mBinding.dataRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mBinding.dataRecyclerView.setAdapter(mDataAdapter);

        AlertDialog.Builder syncingDialogBuilder = new AlertDialog.Builder(getActivity());
        syncingDialogBuilder.setView(R.layout.dialog_syncing);
        syncingDialogBuilder.setCancelable(false);
        mSyncingDialog = syncingDialogBuilder.create();
        mSyncingDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

                ProgressBar bar = mSyncingDialog.findViewById(R.id.syncing_progress);
                // Reset progress to 0 for next time to use.
                if (bar != null) bar.setProgress(0);
            }
        });

        // Set the StreamingClickInterface instance to main activity.
        if (getActivity() != null) ((MainActivity) getActivity()).setStreamingTriggerListener(this);

        mlslManager = new LSLManager();

        PowerManager powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "XSensLSLStreamer::Streaming");
    }

    @Override
    public void onResume() {

        super.onResume();

        // Notify main activity to refresh menu.
        MainActivity.sCurrentFragment = FRAGMENT_TAG_DATA;
        if (getActivity() != null) getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onDetach() {

        super.onDetach();

        // Stop measurement for each sensor when exiting this page.
        mSensorViewModel.setMeasurement(false);
        // It's necessary to update this status, because user may enter this page again.
        mSensorViewModel.updateStreamingStatus(false);

        if(mWakeLock.isHeld())
            mWakeLock.release();
        mlslManager.reset();
        //closeFiles();
    }

    @Override
    public void onStreamingTriggered() {

        if (mSensorViewModel.isStreaming().getValue()) {
            // To stop.
            mSensorViewModel.setMeasurement(false);
            mSensorViewModel.updateStreamingStatus(false);

            XsensDotSyncManager.getInstance(this).stopSyncing();

            if(mWakeLock.isHeld())
                mlslManager.reset();
            mWakeLock.release();

            //closeFiles();

        } else {
            // To start.
            resetPage();

            if (!mSensorViewModel.checkConnection()) {

                Toast.makeText(getContext(), getString(R.string.hint_check_connection), Toast.LENGTH_LONG).show();
                return;
            }

            // Set first device to root.
            mSensorViewModel.setRootDevice(true);
            final ArrayList<XsensDotDevice> devices = mSensorViewModel.getAllSensors();
            // Devices will disconnect during the syncing, and do reconnection automatically.
            XsensDotSyncManager.getInstance(this).startSyncing(devices, SYNCING_REQUEST_CODE);

            if (!mSyncingDialog.isShowing()) mSyncingDialog.show();
        }
    }

    @Override
    public void onHeadingResetTriggered() {
        mBinding.headingsList.setText("");

        for (XsensDotDevice device : mSensorViewModel.getAllSensors()) {
            String headingStatus = "";
            switch (device.getHeadingStatus()) {
                case HEADING_STATUS_XRM_HEADING:
                    headingStatus = "Reset";
                    break;
                case HEADING_STATUS_XRM_DEFAULT_ALIGNMENT:
                    headingStatus = "Reverted";
                    break;
                case HEADING_STATUS_XRM_NONE:
                    headingStatus = "Default";
                    break;
                default:
                    headingStatus = "Unknown";
                    break;
            }
            Log.d("HEADING", "Heading status: " + headingStatus);
            device.resetHeading();
        }
    }

    /**
     * Initialize and observe view models.
     */
    private void bindViewModel() {

        if (getActivity() != null) {

            mSensorViewModel = SensorViewModel.getInstance(getActivity());
            // Implement DataChangeInterface and override onDataChanged() function to receive data.
            mSensorViewModel.setDataChangeCallback(this);
        }
    }

    /**
     * Reset page UI to default.
     */
    private void resetPage() {

        mBinding.syncResult.setText("-");
        mDataList.clear();
        mDataAdapter.notifyDataSetChanged();
        if(mWakeLock.isHeld())
            mWakeLock.release();
        mlslManager.reset();
    }


    @Override
    public void onSyncingStarted(String address, boolean isSuccess, int requestCode) {

        Log.i(TAG, "onSyncingStarted() - address = " + address + ", isSuccess = " + isSuccess + ", requestCode = " + requestCode);
    }

    @Override
    public void onSyncingProgress(final int progress, final int requestCode) {

        Log.i(TAG, "onSyncingProgress() - progress = " + progress + ", requestCode = " + requestCode);

        if (requestCode == SYNCING_REQUEST_CODE) {

            if (mSyncingDialog.isShowing()) {

                if (getActivity() != null) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Find the view of progress bar in dialog layout and update.
                            ProgressBar bar = mSyncingDialog.findViewById(R.id.syncing_progress);
                            if (bar != null) bar.setProgress(progress);
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onSyncingResult(String address, boolean isSuccess, int requestCode) {

        Log.i(TAG, "onSyncingResult() - address = " + address + ", isSuccess = " + isSuccess + ", requestCode = " + requestCode);
    }

    @Override
    public void onSyncingDone(final HashMap<String, Boolean> syncingResultMap, final boolean isSuccess, final int requestCode) {

        Log.i(TAG, "onSyncingDone() - isSuccess = " + isSuccess + ", requestCode = " + requestCode);

        if (requestCode == SYNCING_REQUEST_CODE) {

            if (getActivity() != null) {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (mSyncingDialog.isShowing()) mSyncingDialog.dismiss();

                        mSensorViewModel.setRootDevice(false);

                        if (isSuccess) {

                            mBinding.syncResult.setText(R.string.sync_result_success);

                            // Syncing precess is success, choose one measurement mode to start measuring.
                            mSensorViewModel.setMeasurementMode(PAYLOAD_TYPE_COMPLETE_QUATERNION);

                            //createFiles();

                            mSensorViewModel.setMeasurement(true);
                            // Notify the current streaming status to MainActivity to refresh the menu.
                            mSensorViewModel.updateStreamingStatus(true);

                            //all sensors are in syncingResultsMaps -> if success is true -> all syncing is true
                            try {

                                //acquire wakelock
                                if(!mWakeLock.isHeld()) {
                                    mWakeLock.acquire();
                                    Toast.makeText(getContext(), getString(R.string.wake_lock_acquired), Toast.LENGTH_LONG).show();
                                } else{
                                    Toast.makeText(getContext(), getString(R.string.wake_lock_already_acquired), Toast.LENGTH_LONG).show();
                                }
                                mlslManager.reset();

                                for (XsensDotDevice device : mSensorViewModel.getAllSensors()) {
                                    mlslManager.addXSensLSLStream(device.getAddress(), device.getTag(), device.getCurrentOutputRate());
                                    //Add heading listener
                                    device.setXsensDotMeasurementCallback(DataFragment.this);
                                }
                            }catch(IOException ex){
                                Toast.makeText(getContext(), getString(R.string.lsl_io_exception), Toast.LENGTH_LONG).show();
                                if(mWakeLock.isHeld())
                                    mWakeLock.release();
                            }



                        } else {

                            mBinding.syncResult.setText(R.string.sync_result_fail);

                            // If the syncing result is fail, show a message to user
                            Toast.makeText(getContext(), getString(R.string.hint_syncing_failed), Toast.LENGTH_LONG).show();

                            for (Map.Entry<String, Boolean> result : syncingResultMap.entrySet()) {

                                if (!result.getValue()) {
                                    // Get the key of this failed device.
                                    String address = result.getKey();
                                    // It's preferred to stop measurement of all sensors.
                                    mSensorViewModel.setMeasurement(false);
                                    // Notify the current streaming status to MainActivity to refresh the menu.
                                    mSensorViewModel.updateStreamingStatus(false);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    public void onSyncingStopped(String address, boolean isSuccess, int requestCode) {

        Log.i(TAG, "onSyncingStopped() - address = " + address + ", isSuccess = " + isSuccess + ", requestCode = " + requestCode);
    }

    @Override
    public void onDataChanged(String address, XsensDotData data) {

        Log.i(TAG, "onDataChanged() - address = " + address);

        mlslManager.pushSample(address, data);
        boolean isExist = false;

        for (HashMap<String, Object> map : mDataList) {

            String _address = (String) map.get(KEY_ADDRESS);
            if (_address.equals(address)) {
                // If the data is exist, try to update it.
                map.put(KEY_DATA, data);
                isExist = true;
                break;
            }
        }

        if (!isExist) {
            // It's the first data of this sensor, create a new set and add it.
            HashMap<String, Object> map = new HashMap<>();
            map.put(KEY_ADDRESS, address);
            map.put(KEY_TAG, mSensorViewModel.getTag(address));
            map.put(KEY_DATA, data);
            mDataList.add(map);
        }

        //updateFiles(address, data);

        if (getActivity() != null) {

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // The data is coming from background thread, change to UI thread for updating.
                    mDataAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public void onXsensDotHeadingChanged(String address, int status, int results) {
        Log.i("HEADING", "Heading reset: " + address + ", " + (status==HEADING_SUCCESS ? "SUCCESS": "FAILURE"));
        final String addr = address;
        final String stat = (status==HEADING_SUCCESS ? "SUCCESS": "FAILURE");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBinding.headingsList.setText(mBinding.headingsList.getText() + addr + ": " + stat + "\n");
            }
        });
    }

    @Override
    public void onXsensDotRotLocalRead(String address, float[] quaternions) {
        //we dont care about the resulting matrix
    }
}
