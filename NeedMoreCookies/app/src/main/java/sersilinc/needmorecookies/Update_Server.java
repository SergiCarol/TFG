package sersilinc.needmorecookies;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.analytics.ecommerce.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;



public class Update_Server extends Service {

    static final int MSG_GET_DATA = 1;
    static final String url = "https://www.tfg.centrethailam.com";
    JSONEncoder jsonEncoderClass = new JSONEncoder();
    private final IBinder mBinder = new LocalBinder();
    IntentFilter filter;
    private MyReceiver receiver;
    private final String TAG = "Update_Server: ";

    private final String [] keys = {"Objective","Code","list_name","Hash","Update","GoogleAccount","status"};
    private String [] values = new String[7];
    private String [] items = new String[4];
    private final String [] objectives = {"new_name","new_price","new_quantity","new_item","delete_item","new_list","delete_list","set_public","add_usr_to_list","add_user"};

    public class LocalBinder extends Binder {
        public Update_Server getService() {
            // Return this instance of LocalService so clients can call public methods
            return Update_Server.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, " Binding");
        return mBinder;
    }

    @Override
    public void onCreate (){
        super.onCreate();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        filter = new IntentFilter("Update_Server_Thread");
        receiver = new MyReceiver();
        this.registerReceiver(receiver, filter);
        Log.v(TAG, " Update Server started");
        jsonEncoderClass.create_template();
        // Set values
        set_values(3,"private_sergi","Private Sergi","abc","True","0");
        set_items("Meat","Beef","6","1");
        // Create JSON
        set_json(keys, values, 0);
        set_json(keys, items, 1);
        Log.v(TAG, String.valueOf(jsonEncoderClass.return_json()));
        if (!is_network_available())
            Toast.makeText(getBaseContext(), "No network available", Toast.LENGTH_LONG).show();
        else
            send_post_request(jsonEncoderClass.return_json());
    }
    // Creates the apropiate JSON based if the values need to go in the main or in Values
    private boolean set_json(String [] key, String[] value,int update_main){

        if (jsonEncoderClass.return_json() == null) return false;
        if (update_main == 0) {
            if (key.length != value.length) return false;
            jsonEncoderClass.set_values(key, value, update_main);
        }
        else{
            jsonEncoderClass.set_values(key, value, update_main);
        }

        return true;
    }
    // Sets the values for the values array
    private boolean set_values(int objective_code,String list_code,String list_name,String hash,String update,String status){
        if (objective_code > 10) return false;
        values[0] = objectives[objective_code];
        values[1] = list_code;
        values[2] = list_name;
        values[3] = hash;
        values[4] = update;
        values[5] = User_Info.getInstance().getEmail();
        values[6] = status;

        return true;
    }
    // Sets values for the item array
    private void set_items(String Type, String Product_name, String Price, String Quantity){
        items[0] = Type;
        items[1] = Product_name;
        items[2] = Price;
        items[3] = Quantity;
    }

    private void send_post_request(final JSONObject o){

        Log.v(TAG, "full url = " + url);
        // Create new thread so not to block URL
        Log.v(TAG,"Creating thread to send data");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String response = "";
                HttpURLConnection connection = null;
                try {
                    URL link_url = new URL(url);
                    connection = (HttpURLConnection)link_url.openConnection();
                    //Set to POST
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setReadTimeout(10000);
                    Writer writer = new OutputStreamWriter(connection.getOutputStream());
                    writer.write(o.toString());
                    writer.flush();
                    writer.close();
                    Reader in = new InputStreamReader(connection.getInputStream(), "UTF-8");
                    Log.v("Thread", "Sended");
                    //in.close();
                    StringBuilder sb = new StringBuilder();
                    for (int c; (c = in.read()) >= 0;) {
                        sb.append((char) c);
                    }
                    in.close();
                    response = sb.toString();
                    Log.v("Thread response: ", response);
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    if (connection != null) connection.disconnect();
                    Intent intent = new Intent();
                    intent.putExtra("message",response);
                    intent.setAction("Update_Server_Thread");
                    Log.d("Thread ", "Sending response");
                    sendBroadcast(intent);
                }
            }
        });
        t.start();

    }

    private boolean is_network_available(){
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d(TAG, "Got message: " + message);
        }
    };
}




class JSONEncoder{
    String TAG = "JSONEncoder";
    JSONObject obj;

    public JSONObject create_template(){
        Log.v(TAG, " Started");
        try {
            obj = new JSONObject("{\"main\":{\"status\":\"0\",\"Code\":\"default\",\"list_name\":\"default\",\"Hash\":\"0000\",\"Update\":\"True\",\"GoogleAccount\":\"default\", \"Objective\":\"default\"},\"Values\":{}}");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.v(TAG," Done");
        return obj;
    }

    public void set_values(String key [], String value [],int update_main){
        switch (update_main){
            case 0:
                try {
                    for (int i = 0; i < key.length; i++)
                        obj.getJSONObject("main").put(key[i],value[i]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case 1:
                try {
                    JSONArray items = new JSONArray();
                    JSONArray a = new JSONArray();
                    JSONObject tmp = new JSONObject();
                    a.put(value[0]);
                    a.put(value[1]);
                    items.put(a);
                    items.put(value[2]);
                    items.put(value[3]);
                    tmp.put("Item",items);
                    Log.v(TAG, String.valueOf(a));
                    Log.v(TAG, String.valueOf(items));
                    Log.v(TAG, String.valueOf(tmp));
                    obj.put("Values",tmp);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public JSONObject return_json(){
        return obj;
    }
}
