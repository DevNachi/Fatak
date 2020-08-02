package dev.allsafeindia.fatak;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import dev.allsafeindia.fatak.Adapter.DeviceAdapter;
import dev.allsafeindia.fatak.Interface.DeviceListClick;
import dev.allsafeindia.fatak.Interface.UpdateUI;

import com.airbnb.lottie.LottieAnimationView;
import com.codekidlabs.storagechooser.StorageChooser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.DexterActivity;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import dev.allsafeindia.fatak.server.FileHandler;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity implements UpdateUI, DeviceListClick, WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener, ZXingScannerView.ResultHandler {
    public final static String TAG = "MainActivity";
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver receiver;
    IntentFilter intentFilter;
    List<WifiP2pDevice> p2pDevices = new ArrayList<>();
    Button send, receive;
    View customAlertView;
    DeviceAdapter deviceAdapter;
    WifiP2pConfig wifiP2pConfig;
    ServerClass serverClass;
    static FileHandler fileHandeler;
    ServerSocket serverSocket;
    ClientClass clientClass;
    AlertDialog alertDialog;
    boolean isClient = false;
    ImageView qrCodeData;
    LottieAnimationView lottieAnimationView;
    private static final int FILEPICKER_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        assert manager != null;
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WifiBroadcastReciver(manager, channel, this);
        deviceAdapter = new DeviceAdapter(this, p2pDevices);
        wifiP2pConfig = new WifiP2pConfig();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        send = findViewById(R.id.send);
        receive = findViewById(R.id.receive);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);

        send.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(final View view) {
                try {
                    //start server and sockets
                    serverSocket = new ServerSocket(8888);
                    serverClass = new ServerClass(serverSocket, MainActivity.this);
                    serverClass.start();
                    String[] PERMISSIONS = {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    };

                    //ask all permissions
                    if (hasPermissions(MainActivity.this, PERMISSIONS)) {
                        //permission granted
                        findPeer();
                        ShowFilepicker();
                    } else {
                        //denied
                        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, FILEPICKER_PERMISSIONS);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                customAlertView=LayoutInflater.from(MainActivity.this).inflate(R.layout.scanner, null);
                final ZXingScannerView scannerView=customAlertView.findViewById(R.id.scanner);
                Dexter.withActivity(MainActivity.this)
                        .withPermission(Manifest.permission.CAMERA)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {
                                scannerView.setResultHandler(MainActivity.this);
                                scannerView.startCamera();
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                Toast.makeText(MainActivity.this,"Permission denied for Scan",Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                            }
                        })
                        .check();
                alertDialog = new AlertDialog.Builder(MainActivity.this)
                        .setView(customAlertView)
                        .setNegativeButton("Done", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }).setCancelable(false)
                        .create();
                alertDialog.show();
            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void findPeer() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        customAlertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.devicelist, null);
        qrCodeData = customAlertView.findViewById(R.id.qrCodeData);
        lottieAnimationView = customAlertView.findViewById(R.id.device_detail_loading);
        alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setView(customAlertView)
                .setNegativeButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).setCancelable(false)
                .create();
        alertDialog.show();
        assert wifiManager != null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                Log.i(getLocalClassName(), "WifiOn");
                String ssid = Objects.requireNonNull(reservation.getWifiConfiguration()).SSID;
                String password = reservation.getWifiConfiguration().preSharedKey;
                MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                try {
                    BitMatrix bitMatrix = multiFormatWriter.encode(ssid + " " + password, BarcodeFormat.QR_CODE, 200, 200);
                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                    Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                    qrCodeData.setImageBitmap(bitmap);
                    qrCodeData.setVisibility(View.VISIBLE);
                    lottieAnimationView.setVisibility(View.GONE);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), reservation.getWifiConfiguration().SSID + " " + reservation.getWifiConfiguration().preSharedKey, Toast.LENGTH_SHORT).show();
                super.onStarted(reservation);
            }

            @Override
            public void onStopped() {
                super.onStopped();
            }

            @Override
            public void onFailed(int reason) {
                super.onFailed(reason);
            }
        }, new Handler());
    }

    public class ServerClass extends Thread {
        ServerSocket serverSocket;
        Socket socket;
        MainActivity activity;

        public ServerClass(ServerSocket serverSocket, MainActivity activity) {
            this.serverSocket = serverSocket;
            this.activity = activity;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    socket = serverSocket.accept();
                    Log.i(TAG, "CLIENT CONNECTED");
                    activity.onThreadWorkDone("CLIENT CONNECTED");
                    fileHandeler = new FileHandler(socket, MainActivity.this);
                    fileHandeler.run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public class ClientClass extends Thread {
        Socket socket;
        String inetAddress;
        MainActivity activity;

        public ClientClass(String inetAddress, MainActivity activity) {
            this.inetAddress = inetAddress;
            this.activity = activity;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    socket = new Socket(inetAddress, 8888);
                    Log.i(TAG, "CONNECTED TO HOST");
                    activity.onThreadWorkDone("CONNECTED TO HOST");
                    fileHandeler = new FileHandler(socket, MainActivity.this);
                    fileHandeler.run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(getLocalClassName(), ex.toString());
        }
        return null;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void ShowFilepicker() {
        final StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(MainActivity.this)
                .withFragmentManager(getFragmentManager())
                .withMemoryBar(true)
                .allowCustomPath(true)
                .setType(StorageChooser.FILE_PICKER)
                .build();

        // 2. Retrieve the selected path by the user and show in a toast !
        chooser.setOnMultipleSelectListener(new StorageChooser.OnMultipleSelectListener() {
            @Override
            public void onDone(ArrayList<String> selectedFilePaths) {
                if (selectedFilePaths.size() > 0) {
                    try {
                        fileHandeler.sentData(new File(selectedFilePaths.get(0)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // 3. Display File Picker !
        chooser.show();
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == FILEPICKER_PERMISSIONS) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                        MainActivity.this,
                        "Permission granted! Please click on pick a file once again.",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        MainActivity.this,
                        "Permission denied to read your External storage :(",
                        Toast.LENGTH_SHORT
                ).show();
            }

            return;
        }
    }

    @Override
    public void onThreadWorkDone(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {

    }

    @Override
    public void deviceOnClick(WifiP2pDevice device) {
        wifiP2pConfig.deviceAddress = device.deviceAddress;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.connect(channel, wifiP2pConfig, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                alertDialog.dismiss();
                Toast.makeText(MainActivity.this, "connected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(MainActivity.this, "Connection fail", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        Log.i(TAG, "" + wifiP2pInfo.groupOwnerAddress.getHostAddress());
        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            Log.i(TAG, "Owner");
            isClient = false;
            try {
                ServerSocket serverSocket = new ServerSocket(8888);
                serverClass = new ServerClass(serverSocket, this);
                serverClass.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Log.i(TAG, "Client");
            isClient = true;
            clientClass = new ClientClass(wifiP2pInfo.groupOwnerAddress.getHostAddress(), this);
            clientClass.start();
        }
    }

    @Override
    public void handleResult(Result rawResult) {
        getSupportFragmentManager().popBackStack();
        String[] rawDatas = rawResult.getText().split(" ");
        String ssid = rawDatas[0];
        String key = rawDatas[1];
        Toast.makeText(this, ""+rawResult.getText(), Toast.LENGTH_SHORT).show();
        Log.i(getLocalClassName(),ssid+" "+key);
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", key);
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);

        assert wifiManager != null;
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }
}