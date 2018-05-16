package com.example.facee.droneapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private static String TAG = "MainActivity";

    Socket socket;
    BufferedReader in;

    PrintWriter out;        //서버에 데이터를 전송한다.
    EditText input;         //화면구성
    Button button;          //화면구성
    TextView output;        //화면구성

    TextView TV_connect_state;
    TextView TV_gps_value;

    String data;

    //for gps
    LocationManager locationManager;
    LocationListener locationListener;
    boolean isGPSEnabled;
    boolean isNetworkEnabled; //gps와 network 확인
    Double latPoint = 0.0;
    Double lngPoint = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {   //앱 시작시  초기화설정
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context = this;

        //for gps
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER); //gps와 network 확인
        gpsSetting(context);//gps 값을 계속 수신받는다

        //화면꺼짐 방지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //start
        input = (EditText) findViewById(R.id.input); // 글자입력칸을 찾는다.
        button = (Button) findViewById(R.id.button); // 버튼을 찾는다.
        output = (TextView) findViewById(R.id.output); // 글자출력칸을 찾는다.
        TV_connect_state = (TextView) findViewById(R.id.TV_connect_state);
        TV_gps_value = (TextView) findViewById(R.id.TV_gps_value);

        //socket생성 thread
        Thread worker = new Thread() {    //worker 를 Thread 로 생성
            public void run() { //스레드 실행구문
                try {
                    //소켓을 생성하고 입출력 스트립을 소켓에 연결한다.
                    socket = new Socket("106.10.42.172", 4000); //소켓생성
                    out = new PrintWriter(socket.getOutputStream(), true); //데이터를 전송시 stream 형태로 변환하여                                                                                                                       //전송한다.
                    in = new BufferedReader(new InputStreamReader(
                            socket.getInputStream())); //데이터 수신시 stream을 받아들인다.
                    if (socket.isConnected()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TV_connect_state.setText("연결성공 : " + String.valueOf(socket.getInetAddress()));
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //타이머가 주기적으로 해야될 일
                TimerTask tt = new TimerTask() {
                    @Override
                    public void run() {
                        //버튼이 클릭되면 소켓에 데이터를 출력한다.
                        data = Double.toString(latPoint) + "," + Double.toString(lngPoint); //글자입력칸에 있는 글자를 String 형태로 받아서 data에 저장
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TV_gps_value.setText(data); //ui 변경
                            }
                        });
                        Log.w("NETWORK", " " + data);
                        if (data != null) { //만약 데이타가 아무것도 입력된 것이 아니라면
                            out.println(data); //data를   stream 형태로 변형하여 전송.  변환내용은 쓰레드에 담겨 있다.
                        }
                    }
                };
                //주기를 정한다
                Timer timer = new Timer();
                timer.schedule(tt, 0, 2000);
            }
        };
        worker.start();  //onResume()에서 실행.

        // 버튼을 누르는 이벤트 발생, 이벤트 제어문이기 때문에 이벤트 발생 때마다 발동된다. 시스템이 처리하는 부분이 무한루프문에
        //있더라도 이벤트가 발생하면 자동으로 실행된다.
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

            }
        });

        //sample code
        Thread getDataThread = new Thread() {
            public void run() { //스레드 실행구문
                //소켓에서 데이터를 읽어서 화면에 표시한다.
                try {
                    while (true) {
                        data = in.readLine(); // in으로 받은 데이타를 String 형태로 읽어 data 에 저장
                        output.post(new Runnable() {
                            public void run() {
                                output.setText(data); //글자출력칸에 서버가 보낸 메시지를 받는다.
                            }
                        });
                    }
                } catch (Exception e) {
                }
            }
        };
        //getDataThread.start();  //onResume()에서 실행.


    }// end onCreate

    //화면이 꺼졌다가 복귀할때
    @Override
    protected void onResume() {
        super.onResume();
    }

    public void gpsSetting(Context context) {
        final List<String> m_lstProviders = locationManager.getProviders(false);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.e("location", "[" + location.getProvider() + "] (" + location.getLatitude() + "," + location.getLongitude() + ")");
                latPoint = location.getLatitude();
                lngPoint = location.getLongitude();

                locationManager.removeUpdates(locationListener);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.e("onStatusChanged", "onStatusChanged");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.e("onProviderEnabled", "onProviderEnabled");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.e("onProviderDisabled", "onProviderDisabled");
            }
        };

        //permission check
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Don't have permission", Toast.LENGTH_LONG).show();
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, locationListener);

        Log.i(TAG, Double.toString(latPoint));
        Log.i(TAG, Double.toString(lngPoint));
    }


}




