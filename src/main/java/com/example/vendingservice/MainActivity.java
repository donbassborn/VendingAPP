package com.example.vendingservice;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.json.JSONArray;
import org.json.JSONObject;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;



public class MainActivity extends AppCompatActivity {

    private ListView machinesListWidget;
    private ArrayList<HashMap<String, String>> machinesListArray;
    private ArrayList<VendingDisplacement> machinesList;
    private SimpleAdapter machinesListAdapter;


    final private String mqtt_broker = "tcp://broker.hivemq.com:1883";
    final private String mqtt_instock_broadcast_topic = "vending/instock/+";
    final private String mqtt_instock_topic = "vending/instock/";
    final private String mqtt_instock_request_topic = "vending/request_instock"; // ????????
    private String mqtt_publisherId = UUID.randomUUID().toString();
    private MqttAsyncClient mqtt;
    private Handler handleListUpdate;

    public class mqtt_actions implements MqttCallback {

        public void connectionLost(Throwable arg0) {
            System.err.println("connection lost");
        }

        public void deliveryComplete(IMqttDeliveryToken arg0) {
            System.err.println("delivery complete");
        }


        public void messageArrived(String topic, MqttMessage message) throws Exception {

            String msg  = new String(message.getPayload());
            System.out.println("topic: " + topic);
            System.out.println("message: " + msg);

            if (topic.startsWith(mqtt_instock_topic)) {
                try {
                    JSONObject jsonObject = new JSONObject(msg);
                    VendingDisplacement machine = new VendingDisplacement(jsonObject.getString("name"), jsonObject.getString("addr"));
                    JSONArray lines = jsonObject.getJSONArray("lines");

                    for(int i = 0; i < lines.length(); ++i) {
                        JSONObject jsonLine = lines.getJSONObject(i);
                        ProductLine line = new ProductLine(jsonLine.getInt("height"), jsonLine.getInt("width"));
                        JSONArray jsonLineProducts = jsonLine.getJSONArray("products");

                        System.out.println(machine.name);

                        for(int k = 0; k < jsonLineProducts.length(); k++) {

                            JSONObject jsonProduct = jsonLineProducts.getJSONObject(k);

                            Product product = new Product(jsonProduct.getInt("id"),
                                    jsonProduct.getString("name"),
                                    jsonProduct.getDouble("price"),
                                    jsonProduct.getInt("count"));
                            line.products.add(product);
                        }
                        machine.lines.add(line);
                    }
                    machinesList.add(machine);
                    System.out.println("machine: " + machine.name);
                    HashMap<String, String> machineListRepr = new HashMap<>();
                    machineListRepr.put("Title", machine.name);
                    machineListRepr.put("Addr", machine.addr);
                    machinesListArray.add(machineListRepr);
                    handleListUpdate.sendEmptyMessage(0);
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }


            }
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        try {
            machinesList.clear();
            machinesListArray.clear();
            machinesListAdapter.notifyDataSetChanged();
            mqtt.publish(mqtt_instock_request_topic, new MqttMessage("req".getBytes()));
        } catch (MqttException e) {
            e.printStackTrace();
            Toast msg = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            msg.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //add adapter
        machinesListWidget = findViewById(R.id.machinesList);
        machinesListArray = new ArrayList<>();
        machinesList  = new ArrayList<>();

        FloatingActionButton updButton = findViewById(R.id.updateButton);
        updButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            public void onClick(View v) {
                System.out.println("Update btn");
                onStart();
            }
        });

        machinesListAdapter = new SimpleAdapter(this, machinesListArray, android.R.layout.simple_list_item_2,
                new String[]{"Title", "Addr"},
                new int[]{android.R.id.text1, android.R.id.text2});
        machinesListWidget.setAdapter(machinesListAdapter);
        machinesListWidget.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println("Must move to another intent");
                Intent i = new Intent(getApplicationContext(), MachineView.class);
                i.putExtra("VendingDisplacement", machinesList.get((int) id));
                startActivity(i);
            }
        });

        handleListUpdate = new Handler() {
            public void handleMessage(android.os.Message msg) {
                machinesListAdapter.notifyDataSetChanged();
            };
        };

        MemoryPersistence mqtt_persistence = new MemoryPersistence();
        try {
            mqtt = new MqttAsyncClient(mqtt_broker,mqtt_publisherId,mqtt_persistence);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(20);
            mqtt.setCallback(new mqtt_actions());
            IMqttToken mqttToken = mqtt.connect(options);
            mqttToken.waitForCompletion();
            mqtt.subscribe(mqtt_instock_broadcast_topic, 0);
            //mqtt.publish(mqtt_instock_request_topic, new MqttMessage("req".getBytes()));
        } catch (MqttException e) {
            e.printStackTrace();

            Toast msg = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            msg.show();
        }

    }
}