package com.example.vendingservice;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.telecom.Call;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import static java.lang.Thread.sleep;



public class MachineView extends AppCompatActivity {
    ListView productsListWidget;
    ArrayList<HashMap<String, String>> productsListArray;
    SimpleAdapter productsListAdapter;
    VendingDisplacement selectedMachine;
    Handler handleListUpdate;

    private class CallAPI extends AsyncTask<String, Void, Integer> {
        private int productLine;
        private int productLineIndex;
        private String productName;

        public CallAPI(int productLine, int productLineIndex, String productName){
            this.productLine = productLine;
            this.productLineIndex = productLineIndex;
            this.productName = productName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            System.out.println(result);
            if (result == 200) {
                selectedMachine.lines.get(productLine).products.get(productLineIndex).count--;
                handleListUpdate.sendEmptyMessage(0);
                Toast msg = Toast.makeText(getApplicationContext(), "Pick up your " + productName, Toast.LENGTH_LONG);
                msg.show();
            }
            else {
                Toast msg = Toast.makeText(getApplicationContext(), "Response code: " + result, Toast.LENGTH_LONG);
                msg.show();
            }
        }

        @Override
        protected Integer doInBackground(String... params) {
            String urlString = params[0]; // URL to call
            String data = params[1]; //data to post
            OutputStream out = null;
            String response = "";
            int responseCode = -1;
            try {
                URL url = new URL(urlString);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                //conn.setRequestProperty("Content-Length", String.valueOf(data.getBytes("UTF-8").length));
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(data);

                writer.flush();
                writer.close();
                os.close();

                responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line=br.readLine()) != null) {
                       System.out.println(line);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast msg = Toast.makeText(getApplicationContext(), e.getStackTrace()[0].getMethodName(), Toast.LENGTH_LONG);
                msg.show();
            }
            return responseCode;
        }

    }

    @Override
    protected void onStop() {
        super.onStop();

        System.out.println("meme");
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machine_view);

        productsListWidget = findViewById(R.id.productsList);
        productsListArray = new ArrayList<>();
        productsListAdapter = new SimpleAdapter(this, productsListArray, android.R.layout.simple_list_item_2,
                new String[]{"Name", "PriceCount"},
                new int[]{android.R.id.text1, android.R.id.text2});


        productsListWidget.setAdapter(productsListAdapter);
        selectedMachine = (VendingDisplacement) getIntent().getSerializableExtra("VendingDisplacement");
        productsListWidget.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //// api call
                int productLine = Integer.parseInt(productsListArray.get((int) id).get("Line"));
                int productLineIndex = Integer.parseInt(productsListArray.get((int) id).get("LineIndex"));

                if (selectedMachine.lines.get(productLine).products.get(productLineIndex).count > 0) {
                    try {
                        String API_URL = "https://demo.thingsboard.io/api/v1/71wi2fRgIvswwc7TuoMp/telemetry";

                        String body = "{ \"drop\":" + id +  ", \"machineID\":\"" + selectedMachine.name + "\"}";
                        CallAPI api = new CallAPI(productLine, productLineIndex, selectedMachine.lines.get(productLine).products.get(productLineIndex).name);
                        api.execute(API_URL, body);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Toast msg = Toast.makeText(getApplicationContext(),  selectedMachine.lines.get(productLine).products.get(productLineIndex).name + " is out of stock!", Toast.LENGTH_LONG);
                    msg.show();
                }
            }
        });

        handleListUpdate = new Handler() {
            public void handleMessage(android.os.Message msg) {
                updateList();
            };
        };

        handleListUpdate.sendEmptyMessage(0);
    }
    protected void updateList() {
        productsListArray.clear();
        for (int i = 0; i < selectedMachine.lines.size(); i++) {
            for (int k = 0; k < selectedMachine.lines.get(i).products.size(); k++) {
                HashMap<String, String> product = new HashMap<>();
                product.put("Name", selectedMachine.lines.get(i).products.get(k).name);
                product.put("PriceCount",
                        "Price: " + String.valueOf(selectedMachine.lines.get(i).products.get(k).price) +
                                " In stock: " +  String.valueOf(selectedMachine.lines.get(i).products.get(k).count)
                );
                product.put("Line", String.valueOf(i));
                product.put("LineIndex", String.valueOf(k));
                product.put("Id", String.valueOf(selectedMachine.lines.get(i).products.get(k).id));
                productsListArray.add(product);
                productsListAdapter.notifyDataSetChanged();
            }
        }
    }
}