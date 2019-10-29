package com.iotcodelab.voicecommand;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.pixplicity.easyprefs.library.Prefs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import javax.crypto.Mac;

import static com.iotcodelab.voicecommand.Constant.DeviceAddress;
import static com.iotcodelab.voicecommand.Constant.DeviceName;

public class MainActivity extends AppCompatActivity {
    Button connect_arduino, connect, disconnect;
    ImageButton record_voice;
    TextView text_value;
    String MacAddress ;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        record_voice = findViewById(R.id.record_voice);
        connect_arduino = findViewById(R.id.connect_arduino);
        connect = findViewById(R.id.connect);
        disconnect = findViewById(R.id.disconnect);
        text_value = findViewById(R.id.text_value);

        disconnect.setVisibility(View.INVISIBLE);
        if (!Prefs.getString(DeviceAddress,"null").equalsIgnoreCase("null")){
            connect.setText("connect with ".concat(Prefs.getString(DeviceName,"null")));
            MacAddress = Prefs.getString(DeviceAddress,"null");
        }

        connect_arduino.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,ConnectBluetooth.class));
            }
        });

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(MacAddress!=null){
                    new ConnectBT().execute();
                }else{
                    Toast.makeText(getApplicationContext(), "No device connected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Disconnect();
                disconnect.setVisibility(View.INVISIBLE);
                connect.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
            }
        });

        record_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
                startActivityForResult(intent, 10);
            }
        });
    }

    public void refresh() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data!=null){
            text_value.setText(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0));
            sendData(text_value.getText().toString());
        }else {
            Toast.makeText(getApplicationContext(), "Failed to recognize speech!", Toast.LENGTH_LONG).show();
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
    }

    private void sendData(String message) {
        if ( btSocket != null ) {
            try {
                btSocket.getOutputStream().write(message.toString().getBytes());
            } catch (IOException e) {
                errorExit("Error","Unable to Send Data");
            }
        }
    }

    private void Disconnect () {
        if ( btSocket!=null ) {
            try { btSocket.close();}
            catch(IOException e) { errorExit("Error", "Disconnecting"); }
        }
        refresh();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected  void onPreExecute () {
            progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please Wait!!!");
        }

        @Override
        protected Void doInBackground (Void... devices) {
            try {
                if ( btSocket==null || !isBtConnected ) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(MacAddress);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute (Void result) {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                errorExit("Error","Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                connect.setVisibility(View.INVISIBLE);
                disconnect.setVisibility(View.VISIBLE);
                errorExit("Success","Connected");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        Disconnect();
        super.onDestroy();
    }
}
