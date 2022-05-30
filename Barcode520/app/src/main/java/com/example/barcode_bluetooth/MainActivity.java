package com.example.barcode_bluetooth;

// https://www.youtube.com/watch?v=u2pgSu9RhYo
/*
1) Finish the app.
2) Connect to server.
3) Connect to system.
 */

/*
    301 303 304 402 403 506 512
    AA:AA:AA:AA:AA:AA
    31:31:31:00:00:00 31:31:31:01:01:01
*/

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private Button btnScan, btnSQL, btnStopScanning, btnBluetoothSwitch, btnPermissions,btnpush;
    private TextView txtResult, txtError;
    private ImageView ivBluetooth;

    // Copy text
    private ClipboardManager clipBoard;
    private ClipData clipData;

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;

    String TargetString = null;
    String returnBarcode;
    String returnRoomCode;
    /*
        map: 單位時間內強度紀錄
            key: MAC, value: RSSI
        knownBeacons: 教室名下的信標
            key: 信標, value: 教室
        roomVote: 最有可能出現的教室
            key: 教室, value: 票數
    */



    Map<String, ArrayList<Double>> map = new HashMap<String, ArrayList<Double>>();
    Map<String, String> knownBeacons = new HashMap<String, String>();
    Map<String, Integer> roomVote = new HashMap<String, Integer>();

    Boolean stillScanning = false;
    String nextAction;
    String roomText;
    ArrayList<String> strMaxRoom = new ArrayList<String>();
    Integer intMaxRoom;
    Integer newValue;

    ScanSettings scanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(10)
            .build();

    HandlerThread mHandlerThread;
    Handler mainHandler, workHandler;

    ActivityResultLauncher<Intent> shity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    showToast("shity");
                    if (result.getResultCode() == Activity.RESULT_OK){
                        Intent intentAAA = result.getData();
                    }
                }
            });

    ActivityResultLauncher<Intent> cameraResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Bundle bundle = result.getData().getExtras();
                    if (bundle != null) {
                        TargetString = bundle.getString("SCAN_RESULT");
                    }

                    if (TargetString != null) {
                        returnBarcode = TargetString;
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                // Stuff that updates the UI
                                try {
                                    btnScan.setText(R.string.stopScan);
                                } catch (Exception exception) {

                                }
                            }
                        });

                        // When result content is not null
                        nextAction = "";
                        // Initialize alert dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                MainActivity.this
                        );
                        // Set title
                        builder.setTitle("Result");
                        // Set message
                        builder.setMessage(TargetString);
                        // Set positive button
                        builder.setPositiveButton("清點財產", new DialogInterface.OnClickListener() {
                            @Override//清點財產
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new Thread(new Runnable(){
                                    @Override
                                    public void run()
                                    {
                                        MsSqlConnect con = new MsSqlConnect();
                                        con.run();
                                        String item_name=con.getspecificdata(returnBarcode);//得到該條碼的對應物品名稱
                                        //String check=con.pushData_b_table("item", returnBarcode, roomText);;
                                        //con.pushData_b_table()
                                        String check=con.pushData_b_table(item_name,roomText,returnBarcode);//上傳資料庫b
                                        String show;
                                        if(check=="OK")
                                        {
                                            show="清點成功，清點紀錄已經上傳";
                                        }
                                        else
                                        {
                                            show="上傳失敗，請檢察網路，或者確定已經新增物品至資料庫內";
                                        }
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                                        .setIcon(R.drawable.ic_baseline_adb_24)
                                                        .setTitle(show)
                                                        .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                            }
                                                        }).show();
                                            }
                                        });
                                    }
                                }).start();
                            }
                        });
                        // Open in explorer
                        builder.setNeutralButton("Open", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Uri uriExplorer = Uri.parse(TargetString);
                                Intent intentExplorer = new Intent(Intent.ACTION_VIEW, uriExplorer);
                                startActivity(intentExplorer);
                            }
                        });
                        // Save result
                        builder.setNegativeButton("Save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                txtResult.setText(txtResult.getText() + TargetString + "\n");

                                DateFormat df = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");
                                String date = df.format(Calendar.getInstance().getTime());
                                txtError.setText(txtError.getText() + date + "\n");

                                Message message = Message.obtain();
                                message.what = 2;
                                message.obj = "B";
                                workHandler.sendMessage(message);
                                //showToast("startScan work sent");
                            }
                        });
                        // Show alert dialog
                        builder.show();
                    } else {
                        // When result content is null, display toast
                        showToast("Scan something!!!");
                    }

                    switch (nextAction) {
                        case "NotFound":
                            MsSqlConnect NotFoundCon = new MsSqlConnect();
                            NotFoundCon.run();
                            String NotFoundData = NotFoundCon.getspecificdata(TargetString);
                            AlertDialog.Builder NotFoundBuilder = new AlertDialog.Builder(
                                    MainActivity.this
                            );

                            NotFoundBuilder.setTitle("Barcode not in sql");
                            NotFoundBuilder.setMessage(TargetString);
                            NotFoundBuilder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            NotFoundBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            });
                            break;

                        case "Found":
                            MsSqlConnect FoundCon = new MsSqlConnect();
                            FoundCon.run();
                            String FoundData = FoundCon.getspecificdata(TargetString);
                            AlertDialog.Builder builder = new AlertDialog.Builder(
                                    MainActivity.this
                            );

                            builder.setTitle("Barcode found in sql");
                            builder.setMessage(FoundData);
                            builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            break;
                        default:
                            break;
                    }
                }

            });

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            showToast("BLE// onScanResult");
            super.onScanResult(callbackType, result);
            showToast("callback success");
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            ArrayList<Double> tempAL;
            //showToast("BLE// onBatchScanResults");
            txtResult.setText("");
            //peripheralTextView.setMovementMethod(new ScrollingMovementMethod());
            if (results.size() == 0) {
                showToast("No BLE");
                return;
            }
            try {
                txtResult.append("ScanResults: " + results.size() + '\n');
                for (ScanResult sr : results) {
                    tempAL = new ArrayList<>();
                    Double calVal = (double) sr.getRssi();
                    if (map.containsKey(sr.getDevice())) {
                        tempAL = map.get(sr.getDevice().toString());
                        tempAL.add(calVal);
                    } else {
                        tempAL.add(calVal);
                    }
                    map.put(sr.getDevice().toString(), tempAL);
                }
            } catch (Exception e){
                txtError.setText(e.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            showToast("Scan Failed" + "Error Code: " + errorCode);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bluetooth initialize
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        // HandlerThread
        mainHandler = new Handler();
        mHandlerThread = new HandlerThread("handlerThread");
        mHandlerThread.start();

        workHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 2:
                        //showToast("Start work");
                        try {
                            stillScanning = true;
                            bluetoothLeScanner.startScan(null, scanSettings, leScanCallback);
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            showToast(e.toString());
                            txtError.setText(e.toString());
                        }
                        break;
                    case 3:
                        //showToast("Stop work");
                        try {
                            bluetoothLeScanner.stopScan(leScanCallback);
                            stillScanning = false;
                            //peripheralTextView.setText("");
                        } catch (Exception e) {
                            showToast(e.toString());
                            txtError.setText(e.toString());
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        if (bluetoothAdapter == null) {
            showToast("No bluetooth adapter");
            finish();
        } else {
            setContentView(R.layout.activity_main);

            knownBeacons.put("EA:4A:60:8C:A3:22", "SEC402");
            knownBeacons.put("F0:E3:65:45:98:55", "SEC403");
            knownBeacons.put("D6:8B:34:AF:9A:CD", "SEC403");

            roomVote.put("SEC402", 0);
            roomVote.put("SEC403", 0);

            // Assign
            btnScan = findViewById(R.id.btnScan);
            txtResult = findViewById(R.id.txtResult);
            txtError = findViewById(R.id.txtError);

            btnPermissions = findViewById(R.id.btnPermissions);
            btnSQL = findViewById(R.id.btnSQL);
            btnBluetoothSwitch = findViewById(R.id.btnBluetoothSwitch);
            btnpush=findViewById(R.id.pushbton);
            //btnStopScanning = findViewById(R.id.btnStopScanning);
            ivBluetooth = findViewById(R.id.ivBluetooth);

            // Buttons on-click
            btnScan.setOnClickListener(btnScanListener);
            btnSQL.setOnClickListener(btnSQLListener);
            btnBluetoothSwitch.setOnClickListener(btnBluetoothSwitchListener);
            btnpush.setOnClickListener(btnpushListener);
            //btnStopScanning.setOnClickListener(btnStopScanningListener);
            btnPermissions.setOnClickListener(btnPermissionsListener);

            if (bluetoothAdapter.isEnabled()) {
                ivBluetooth.setImageResource(R.drawable.ic_bluetooth_on);
                btnBluetoothSwitch.setText("TURN OFF");
            } else {
                ivBluetooth.setImageResource(R.drawable.ic_bluetooth_off);
                btnBluetoothSwitch.setText("TURN ON");
            }
        }
    }

    // Barcode scanner setup
    private View.OnClickListener btnScanListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (stillScanning) {
                Message message = Message.obtain();
                message.what = 3;
                message.obj = "C";
                workHandler.sendMessage(message);
                //showToast("stopScan work sent");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Stuff that updates the UI
                        /*
                            map: 單位時間內強度紀錄
                                key: MAC, value: RSSI
                            knownBeacons: 教室名下的信標
                                key: 信標, value: 教室
                            roomVote: 最有可能出現的教室
                                key: 教室, value: 票數
                        */
                        try {
                            btnScan.setText(R.string.scan);

                            for (Map.Entry<String, ArrayList<Double>> entry: map.entrySet()) {
                                roomText = "";
                                if (knownBeacons.containsKey(entry.getKey())) {
                                    roomText = knownBeacons.get(entry.getKey());
                                    newValue = roomVote.get(roomText);
                                    roomVote.put(roomText, newValue+1);
                                }
                                //showToast(entry.getKey() + '\n' + entry.getValue());
                                Double rssiTotal = 0.0;
                                for (Double rssi: entry.getValue()) {
                                    rssiTotal += rssi;
                                }
                                Double rssiAverage = rssiTotal/entry.getValue().size();
                                txtResult.setText(txtResult.getText() + entry.getKey() + ", rssi: " + rssiAverage + roomText + '\n');
                            }

                            strMaxRoom.clear();
                            intMaxRoom = 0;

                            for (Map.Entry<String, Integer> entry: roomVote.entrySet()) {
                                //showToast(entry.getKey() + ", value: " + String.valueOf(entry.getValue()));
                                if (entry.getValue() > intMaxRoom) {
                                    strMaxRoom.clear();
                                    strMaxRoom.add(entry.getKey());
                                    intMaxRoom = entry.getValue();
                                } else if (entry.getValue() == intMaxRoom) {
                                    strMaxRoom.add(entry.getKey());
                                } else {
                                    showToast("WTF");
                                }
                            }
                            showToast(String.valueOf(strMaxRoom.size()));
                            if (strMaxRoom.size() > 1) {
                                roomText = "";
                                for (int i=0; i<strMaxRoom.size(); i++) {
                                    roomText += strMaxRoom.get(i);
                                    if (i != strMaxRoom.size()-1)
                                        roomText += " or ";
                                }
                            } else if (strMaxRoom.size() > 0) {
                                roomText = strMaxRoom.get(0);
                            } else {
                                roomText = "Unknown";
                            }
                            txtResult.setText(txtResult.getText() + roomText + "\n");
                        } catch (Exception exception) {
                        }
                    }
                });
                return;
            }
            if (!bluetoothAdapter.isEnabled()) {
                showToast("Turn on bluetooth to scan.");
                return;
            }
            IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
            intentIntegrator.setBarcodeImageEnabled(true);
            intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            //intentIntegrator.setCameraId(0);
            intentIntegrator.setOrientationLocked(true);
            intentIntegrator.setPrompt("For flash use volume up key");
            cameraResult.launch(intentIntegrator.createScanIntent());
/*
            // Initialize intent integrator
            IntentIntegrator intentIntegrator = new IntentIntegrator(
                    MainActivity.this
            );

            // Set prompt text
            intentIntegrator.setPrompt("For flash use volume up key");

            intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            intentIntegrator.setBarcodeImageEnabled(true);
            // Set beep
            intentIntegrator.setBeepEnabled(true);
            // Locked orientation
            intentIntegrator.setOrientationLocked(true);
            // Set capture activity
            intentIntegrator.setCaptureActivity(Capture.class);
            // Initiate scan
            intentIntegrator.initiateScan();
*/
        }
    };

    // Copy text
    public View.OnClickListener txtCopyListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Vibrator myVibrator = (Vibrator) getApplication()
                    .getSystemService(Service.VIBRATOR_SERVICE);
            myVibrator.vibrate(50);
            clipBoard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipData = ClipData.newPlainText(null, txtResult.getText().toString());
            clipBoard.setPrimaryClip(clipData);
            showToast("Copied: " + txtResult.getText().toString());
        }
    };

    private View.OnClickListener btnStopScanningListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (stillScanning) {
                Message message = Message.obtain();
                message.what = 3;
                message.obj = "C";
                workHandler.sendMessage(message);
                //showToast("stopScan work sent");

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // Stuff that updates the UI
                        try {
                            for (Map.Entry<String, ArrayList<Double>> entry: map.entrySet()) {
                                showToast(entry.getKey() + '\n' + entry.getValue());
                                Double rssiTotal = 0.0;
                                for (Double rssi: entry.getValue()) {
                                    rssiTotal += rssi;
                                }
                                Double rssiAverage = rssiTotal/entry.getValue().size();
                                txtResult.setText(txtResult.getText() + entry.getKey() + ", rssi: " + rssiAverage + '\n');
                            }
                        } catch (Exception exception) {

                        }
                    }
                });
            } else {
                showToast("Not scanning");
            }
        }
    };

    private  View.OnClickListener btnSQLListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    MsSqlConnect con = new MsSqlConnect();
                    con.run();
                    String data=con.getData_c_table();
                    String[] dataList = data.split("\n");
                    //Map<String, String> dataMap = new HashMap<String, String>();
                    runOnUiThread(new Runnable() {
                        public void run() {
                     //       txtResult.setText(data);
                            for (String dataLine: dataList) {
                                txtResult.setText(dataLine +'\n' + txtResult.getText());
                            }
                        }
                    });
                }
            }).start();
        }
    };

    // Bluetooth switch
    private View.OnClickListener btnBluetoothSwitchListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (bluetoothAdapter.isEnabled()) {
                // Turn off
                btnBluetoothSwitch.setText("TURN ON");
                ivBluetooth.setImageResource(R.drawable.ic_bluetooth_off);

                showToast("Turning bluetooth off");
                bluetoothAdapter.disable();
            } else {
                // Turn on
                btnBluetoothSwitch.setText("TURN OFF");
                ivBluetooth.setImageResource(R.drawable.ic_bluetooth_on);
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                try {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
                    shity.launch(intent);
                    showToast("Turning bluetooth on");
                } catch (Exception e) {
                    txtError.setText(e.toString());
                    showToast(e.toString());
                }
            }
        }
    };

    private  View.OnClickListener btnpushListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent swtpushpage = new Intent();
            swtpushpage.setClass(MainActivity.this,add_data_to_sql.class);
            startActivity(swtpushpage);
        }
    };

    private View.OnClickListener btnPermissionsListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
        }
    };

    // Reply result
/*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        showToast("Fucking onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        // Initialize intent result
        IntentResult intentResult = IntentIntegrator.parseActivityResult(
                requestCode, resultCode, data
        );
        // Check condition
        if (intentResult.getContents() != null) {
            // When result content is not null
            // Initialize alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this
            );
            // Set title
            builder.setTitle("Result");
            // Set message
            builder.setMessage(intentResult.getContents());
            // Set positive button
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Dismiss dialog
                    dialogInterface.dismiss();
                }
            });
            // Open in explorer
            builder.setNeutralButton("Open", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Uri uriExplorer = Uri.parse(intentResult.getContents());
                    Intent intentExplorer = new Intent(Intent.ACTION_VIEW, uriExplorer);
                    startActivity(intentExplorer);
                }
            });
            // Save result
            builder.setNegativeButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    txtResult.setText(txtResult.getText() + intentResult.getContents() + "\n");

                    DateFormat df = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");
                    String date = df.format(Calendar.getInstance().getTime());
                    txtError.setText(txtError.getText() + date + "\n");
                }
            });
            // Show alert dialog
            builder.show();
        } else {
            // When result content is null
            // Display toast
            showToast("Scan something!!!");
        }

    }
*/
    // Copy text
    public void txtCopyListener(View view) {
        Vibrator myVibrator = (Vibrator) getApplication()
                .getSystemService(Service.VIBRATOR_SERVICE);
        myVibrator.vibrate(50);
        clipBoard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipData = ClipData.newPlainText(null, txtResult.getText().toString().replace("\n", ""));
        clipBoard.setPrimaryClip(clipData);
        showToast("Copied: " + txtResult.getText().toString());
    }

    // Distance calculation
    protected static double calculateAccuracy(int txPower, double rssi) {
        // If we cannot determine accuracy, return -1
        if (rssi == 0) {
            return -1.0;
        }

        double ratio = rssi / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Short targetRssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                BluetoothDevice targetDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String targetName = targetDevice.getAddress();

                Double doubleTargetRssi = Double.valueOf(targetRssi);

            }
        }
    };

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}