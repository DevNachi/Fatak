package dev.allsafeindia.fatak.Adapter;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


import dev.allsafeindia.fatak.Interface.DeviceListClick;
import dev.allsafeindia.fatak.MainActivity;
import dev.allsafeindia.fatak.R;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {
    MainActivity activity;
    List<WifiP2pDevice> wifiP2pDevices;
    LinearLayout linearLayout;
    DeviceListClick deviceListClick;

    public DeviceAdapter(MainActivity activity, List<WifiP2pDevice> wifiP2pDevices) {
        this.activity = activity;
        this.wifiP2pDevices = wifiP2pDevices;
    }

    TextView deviceName;

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_details_layout,parent,false);
        return new MyViewHolder(view) ;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, final int position) {
        deviceName.setText(wifiP2pDevices.get(position).deviceName);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //activity.deviceOnClick(wifiP2pDevices.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return wifiP2pDevices.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_detail_name);
            linearLayout = itemView.findViewById(R.id.device_list_root);
        }
    }
}
