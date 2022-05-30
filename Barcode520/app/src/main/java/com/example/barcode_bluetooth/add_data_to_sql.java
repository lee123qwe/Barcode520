package com.example.barcode_bluetooth;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class add_data_to_sql extends AppCompatActivity {
    Button btnadd, btnback, btnshow;
    EditText ETitemname, ETbarcode;
    ImageView imgCamera;
    String TargetString;

    ActivityResultLauncher<Intent> cameraResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Bundle bundle = result.getData().getExtras();
                    if (bundle != null) {
                        TargetString = bundle.getString("SCAN_RESULT");
                    }

                    if (TargetString != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Stuff that updates the UI
                                try {
                                    ETbarcode.setText(TargetString);
                                } catch (Exception exception) {

                                }
                            }
                        });
                    } else {
                        // When result content is null, display toast
                        showToast("Scan something!!!");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_data_to_sql);

        btnadd=findViewById(R.id.addp);
        btnback=findViewById(R.id.back);
        btnshow=findViewById(R.id.btn_require_a_table);
        ETitemname=findViewById(R.id.item_name);
        ETbarcode=findViewById(R.id.barcode);
        TextView showfrom_db_a=findViewById(R.id.show_from_table_a);
        imgCamera = findViewById(R.id.imgCamera);
        imgCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator intentIntegrator = new IntentIntegrator(add_data_to_sql.this);
                intentIntegrator.setBarcodeImageEnabled(true);
                intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                //intentIntegrator.setCameraId(0);
                intentIntegrator.setOrientationLocked(true);
                intentIntegrator.setPrompt("For flash use volume up key");
                cameraResult.launch(intentIntegrator.createScanIntent());
            }
        });

        btnback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent back=new Intent();
                back.setClass(add_data_to_sql.this,MainActivity.class);
                startActivity(back);
            }
        });
        btnadd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String item=ETitemname.getText().toString();
                String bar=ETbarcode.getText().toString();
                if (item.length() == 0 || bar.length() == 0) {
                    showToast("不得留空");
                    return;
                }
                new Thread(new Runnable(){
                    @Override
                    public void run()
                    {
                        MsSqlConnect con = new MsSqlConnect();
                        con.run();
                        String check=con.pushData_a_table(bar,item);;
                        String show;
                        if(check=="OK")
                        {
                            show="新增成功";
                        }
                        else
                        {
                            show="新增失敗";
                        }
                        if(check=="OK")
                        {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    new AlertDialog.Builder(add_data_to_sql.this)
                                            .setIcon(R.drawable.ic_baseline_adb_24)
                                            .setTitle(show)
                                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                }
                                            }).show();
                                    }
                            });
                        }


                    }

                }).start();
            }
        });
        btnshow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable(){
                    @Override
                    public void run()
                    {
                        MsSqlConnect con = new MsSqlConnect();
                        con.run();
                        String data=con.getData_a_table();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    showfrom_db_a.setText(data);
                                }
                            });
                    }

                }).start();
            }
        });
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}