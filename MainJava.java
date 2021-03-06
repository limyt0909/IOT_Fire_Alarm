package com.example.dectecting_fire;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView val;
    Button detect, stop, connect;
    ProgressBar progress;
    Handler handler;
    String address = null;
    ListView devicelist;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");/*다른 블루투스 모듈 사용시 UUID변경*/
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    BluetoothSocket btSocket = null;
    InputStream Ins = null;
    int readBufferPosition = 0;
    byte[] readBuffer;
    String mStrDelimiter = "\n";
    char mCharDelimiter = '\n';
    String n_state = "false";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    String l_msg;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        val = findViewById(R.id.sensor_val);
        detect = findViewById(R.id.Detecting);
        stop = findViewById(R.id.stop);
        connect = findViewById(R.id.connect);
        progress = findViewById(R.id.progress);
        devicelist = findViewById(R.id.devicelist);
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        handler = new Handler() {
            //반복적인 일을 코딩
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                myBluetooth = BluetoothAdapter.getDefaultAdapter();
                /*아래에 각각경우에 반복적으로 하고싶은 것을 코딩*/
                try {
                    int byteAvailable = Ins.available();   // 수신 데이터 확인
                    if (byteAvailable > 0) {                        // 데이터가 수신된 경우.
                        byte[] packetBytes = new byte[byteAvailable];
                        Ins.read(packetBytes);
                        for (int i = 0; i < byteAvailable; i++) {
                            byte b = packetBytes[i];
                            if (b == mCharDelimiter) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;

                                n_state = data;
                                Log.e("data", "" + data);


                                if (data.equals("true")) {

                                    Log.e("data","s_true");
                                    Toast.makeText(getApplicationContext(), "불났어 튀어", Toast.LENGTH_LONG).show();


                                    Log.e("new",""+l_msg);
                                    
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                                    builder.setTitle("신고").setMessage("신고하실건가요?");
                                    builder.setNegativeButton("아니오", new DialogInterface.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog, int id)
                                        {
                                           // Intent intent = new Intent(getApplicationContext(),SubActivity.class);
                                            //startActivityForResult(intent,sub);//액티비티 띄우기
                                        }
                                    });
                                    builder.setPositiveButton("예", new DialogInterface.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog, int id)
                                        {

                                            Intent intent = new Intent(Intent.ACTION_SENDTO);
                                            Uri uri = Uri.parse("sms:");

                                            intent.setData(uri);
                                            intent.putExtra("sms_body", "a");

                                            startActivity(intent);
                                        }
                                    });
                                    AlertDialog alertDialog = builder.create();

                                    alertDialog.show();
                                }
                                else{
                                    Log.e("data","s_false");
                                }
                            }
                            else {
                                readBuffer[readBufferPosition++] = b;

                            }
                        }

                    }
                } catch (Exception e) {    // 데이터 수신 중 오류 발생.
                    Toast.makeText(getApplicationContext(), "데이터 수신 중 오류가 발생 했습니다.", Toast.LENGTH_LONG).show();
                    finish();            // App 종료.
                }

                this.sendEmptyMessageDelayed(0, 100); //자기자신 호출해서 반복작업
            }

        };
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pairedDevicesList();
            }
        });

        detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progress.setVisibility(View.VISIBLE);
                handler.sendEmptyMessage(0);
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progress.setVisibility(View.INVISIBLE);
                handler.removeMessages(0);
            }
        });
    }
    void pairedDevicesList() {
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                list.add(bt.getName().toString() + "\n" + bt.getAddress().toString());
            }
        } else {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener);
    }
    void beginListenForData() {
        final Handler handler = new Handler();

        readBufferPosition = 0;                 // 버퍼 내 수신 문자 저장 위치.
        readBuffer = new byte[1024];            // 수신 버퍼.

        // 문자열 수신 쓰레드.
                    try {
                        int byteAvailable = Ins.available();   // 수신 데이터 확인
                        if(byteAvailable > 0) {                        // 데이터가 수신된 경우.
                            byte[] packetBytes = new byte[byteAvailable];
                            Ins.read(packetBytes);
                            for (int i = 0; i < byteAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == mCharDelimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    Log.e("test",""+data);
                                    n_state = data;
                                }
                                else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (Exception e) {    // 데이터 수신 중 오류 발생.
                        Toast.makeText(getApplicationContext(), "데이터 수신 중 오류가 발생 했습니다.", Toast.LENGTH_LONG).show();
                        finish();            // App 종료.
                    }

            }


    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String info = ((TextView) view).getText().toString();
            address = info.substring(info.length() - 17);
            Log.d("test",""+address);
            readBufferPosition = 0;
            readBufferPosition = 0;                 // 버퍼 내 수신 문자 저장 위치.
            readBuffer = new byte[1024];


            try {
                if (btSocket == null) {
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                    Ins = btSocket.getInputStream();
                    beginListenForData();

                    Log.d("data", n_state);

                } else {
                    Ins = btSocket.getInputStream();
                    beginListenForData();
                }
            } catch (Exception e) {
                Log.d("test", "connecting_error");
                Toast.makeText(getApplicationContext(), "connecting fail", Toast.LENGTH_LONG).show();
            }

        }
    };
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startLocationService() {

        // get manager instance
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // set listener
        GPSListener gpsListener = new GPSListener();
        long minTime = 100;
        float minDistance = 0;

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTime,
                minDistance,
                gpsListener);

    }


    private class GPSListener implements LocationListener {

        public void onLocationChanged(Location location) {
            //capture location data sent by current provider
            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();

            String msg = "Latitude : "+ latitude + "\nLongitude:"+ longitude;
            Log.i("GPSLocationService", msg);


        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }

}