package br.com.mvbos.way;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import br.com.mvbos.way.core.Core;
import br.com.mvbos.way.core.ItemRequestAdapter;
import br.com.mvbos.way.core.RequestData;

public class ViewLocationActivity extends AppCompatActivity implements LocationListener, HttpRequestHelperResult {

    private static final long MIN_SECONDS = 5 * 1;
    private static final float MIN_METERS = 1;
    private static final int RS_REQ_GET_CONTACT = 1;
    private static final int HTTP_SEND_ID = 1;
    private static final int HTTP_SEND_REQUEST_ID = 2;
    public static final String URL_GOOGLE_MAPS = "http://maps.google.com/?q=%s,%s";

    private LocationManager locationManager;
    private String preferred;

    private TextView textLocation;
    private boolean send;

    private ListView listView;
    private final List<RequestData> locationsList = new ArrayList<>(30);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_location);

        textLocation = (TextView) findViewById(R.id.txtLatLong);
        textLocation.setText("No location avaliable.");

        Button btn = (Button) findViewById(R.id.btnViewMyPosition);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationManager != null) {
                    Location location = locationManager.getLastKnownLocation(preferred);
                    openMap(location.getLatitude(), location.getLongitude());
                }
            }
        });

        btn = (Button) findViewById(R.id.btnSendMyPosition);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send = true;
                openContactList();
            }
        });

        btn = (Button) findViewById(R.id.btnRequestFriendPostition);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send = false;
                openContactList();
            }
        });


        final ItemRequestAdapter adapter = new ItemRequestAdapter(this, new ArrayList<RequestData>(10));
        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);

        registerForContextMenu(listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition = position;
                RequestData requestData = locationsList.get(position);
                if (requestData.isReady()) {
                    openMap(requestData.getLatitude(), requestData.getLongitude());
                }
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();


        if (locationsList.isEmpty()) {
            List<RequestData> temp = (List<RequestData>) Core.load("list.way", this);
            if (temp != null) {
                locationsList.addAll(temp);
                ItemRequestAdapter adapter = (ItemRequestAdapter) listView.getAdapter();
                adapter.clear();
                adapter.addAll(locationsList);
                adapter.notifyDataSetChanged();
            }
        }

        try {

            if (locationManager == null) {
                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    //Toast.makeText(MainActivity.this, "Please turn on your GPS.", Toast.LENGTH_SHORT).show();
                    //Log.i(MainActivity.class.getName(), "is not Provider Enabled");
                }

                for (String s : locationManager.getAllProviders()) {
                    //Log.i(MainActivity.class.getName(), "Provider " + s);
                }

                Criteria c = new Criteria();
                c.setAccuracy(Criteria.ACCURACY_FINE);
                preferred = locationManager.getBestProvider(c, true);

                if (preferred == null)
                    preferred = locationManager.GPS_PROVIDER;

                locationManager.requestLocationUpdates(preferred, MIN_SECONDS, MIN_METERS, ViewLocationActivity.this);

            } else {
                locationManager.requestLocationUpdates(preferred, MIN_SECONDS, MIN_METERS, ViewLocationActivity.this);
            }

        } catch (SecurityException e) {
            e.printStackTrace();
            Log.i(ViewLocationActivity.class.getName(), "is not Provider Enabled");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        try {

            Core.save(locationsList, "list.way", this);

            if (locationManager != null) {
                locationManager.removeUpdates(ViewLocationActivity.this);
            }

        } catch (SecurityException e) {
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (R.id.listView == v.getId()) {
            ListView lv = (ListView) v;
            AdapterView.AdapterContextMenuInfo contextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.add("Retry");
            menu.add("Remove");
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if("Remove".equals(item.getTitle())){
            locationsList.remove(item.getItemId());
            ItemRequestAdapter adapter = (ItemRequestAdapter) listView.getAdapter();
            adapter.clear();
            adapter.addAll(locationsList);
            adapter.notifyDataSetChanged();

            Core.save(locationsList, "list.way", this);
        }


        return super.onContextItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RS_REQ_GET_CONTACT) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    sendPhoneLocation(data);
                    break;
                case Activity.RESULT_CANCELED:
                    break;
                default:
                    Toast.makeText(this, "Unexpected resultCode: " + resultCode, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }


    private void openContactList() {
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        Intent intent = new Intent(Intent.ACTION_PICK, uri);
        startActivityForResult(intent, RS_REQ_GET_CONTACT);
    }


    private void openMap(double latitude, double longitude) {
        String url = String.format(URL_GOOGLE_MAPS, latitude, longitude);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    private List<RequestData> getSingleContactNumber(Intent data) {
        List<RequestData> lst = new ArrayList<>(1);

        TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = tel.getLine1Number();

        Uri contactUri = data.getData();
        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

        cursor.moveToFirst();

        //int column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        //String name = cursor.getString(column);

        int column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        String number = cursor.getString(column);

        cursor.close();

        RequestData req = new RequestData();

        //req.setToName(name);
        req.setToNumber(number);
        req.setFromNumber(phoneNumber);

        lst.add(req);


        return lst;
    }

    private List<RequestData> getAllContactNumber(Intent data) {
        List<RequestData> lst = new ArrayList<>(5);

        TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = tel.getLine1Number();

        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        //Uri uri = data.getData();
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER};

        //TODO filter contact by id
        data.getStringExtra(ContactsContract.CommonDataKinds.Phone._ID);

        Cursor contacts = getContentResolver().query(uri, projection, null, null, null);

        int indexName = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int indexNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        contacts.moveToFirst();
        do {
            String name = contacts.getString(indexName);
            String number = contacts.getString(indexNumber);

            RequestData req = new RequestData();

            req.setFromNumber(phoneNumber);
            req.setToName(name);
            req.setToNumber(number);

            lst.add(req);


        } while (contacts.moveToNext());

        return lst;
    }


    private List<RequestData> getContactNumber(Intent data) {
        Uri resultUri = data.getData();
        Cursor cont = getContentResolver().query(resultUri, null, null, null, null);

        TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = tel.getLine1Number();

        if (!cont.moveToNext()) {
            return Collections.EMPTY_LIST;
        }

        List<RequestData> lst = new ArrayList<>(5);

        String aNumber = null;

        int columnIndexForId = cont.getColumnIndex(ContactsContract.Contacts._ID);
        String contactId = cont.getString(columnIndexForId);

        columnIndexForId = cont.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        String name = cont.getString(columnIndexForId);

        int hasPhoneCol = cont.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
        boolean hasPhone = cont.getInt(hasPhoneCol) != 0;

        if (hasPhone) {

            Cursor numbers = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId, null, null);

            while (numbers.moveToNext()) {
                aNumber = numbers.getString(numbers.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                RequestData req = new RequestData();

                req.setToName(name);
                req.setToNumber(aNumber);
                req.setFromNumber(phoneNumber);

                lst.add(req);
            }

            numbers.close();
        }

        cont.close();

        return lst;
    }

    private void sendPhoneLocation(Intent data) {
        List<RequestData> contacts = getContactNumber(data);
        if (contacts.isEmpty()) {
            Toast.makeText(this, "Selected contact seems to have no phone numbers ", Toast.LENGTH_LONG).show();
            return;
        }


        /*int idx = locationsList.indexOf(contacts.get(0));

        if (idx == -1) {
            return;
        }*/

        try {
            addItemList(contacts.get(0));

            if (send) {
                sendMyLocation(contacts);
            } else {
                requestContactLocation(contacts);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addItemList(RequestData requestData) {
        locationsList.add(requestData);
        ItemRequestAdapter adapter = (ItemRequestAdapter) listView.getAdapter();
        //adapter.clear();
        //adapter.addAll(locationsList);
        adapter.add(requestData);
        adapter.notifyDataSetChanged();

        Core.save(locationsList, "list.way", this);
    }


    private String sendMyLocation(List<RequestData> numbers) throws Exception {

        final String path = "http://mvbos.com.br/ondetatu/list_location.php";

        HttpRequestHelper httpHelper = new HttpRequestHelper(HTTP_SEND_ID, path, null, this);
        httpHelper.execute();

        return null;
    }

    private String requestContactLocation(List<RequestData> contacts) throws Exception {
        final String path = "http://192.168.0.5/ondetatu/list_location.php";
        RequestData req = contacts.get(0);

        Map<String, String> param = new HashMap<>(5);
        param.put("str_0", "reqloc");
        param.put("str_12", req.getFromNumber());
        param.put("str_11", "2215");
        param.put("str_22", req.getToNumber());

        HttpRequestHelper httpHelper = new HttpRequestHelper(HTTP_SEND_REQUEST_ID, path, param, this);
        httpHelper.setExtraData(req);
        httpHelper.execute();

        return null;
    }

    private static String makeResponse(final String path, Map params) throws Exception {

        if (params == null)
            params = Collections.EMPTY_MAP;

        new AsyncTask<Map, Void, String>() {
            @Override
            protected String doInBackground(Map... param) {
                URL url;
                String response = "";

                try {
                    url = new URL(path);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(500);
                    conn.setConnectTimeout(500);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);


                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

                    //writer.write(getPostDataString(postDataParams));

                    writer.flush();
                    writer.close();
                    os.close();

                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line = br.readLine()) != null) {
                            response += line;
                        }

                        JSONObject jsonResp = new JSONObject(response);
                        JSONObject res = jsonResp.getJSONObject("response");
                        String from = res.getString("from");

                        String to = res.getString("to");
                        double latitude = res.getDouble("latitude");
                        double longitude = res.getDouble("longitude");


                    } else {
                        response = "";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


                return response.toString();
            }

        }.execute(params);

        return null;
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onLocationChanged(Location location) {
        String s = String.format("Your location: Latitude %.4f, Longitude %.4f.", location.getLatitude(), location.getLongitude());
        textLocation.setText(s);
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();


    }

    @Override
    public void recieveResult(int id, StringBuilder response, Object extraData) {
        JSONObject jsonResp = null;

        RequestData requestData = (RequestData) extraData;

        if (id == HTTP_SEND_ID) {
            try {
                jsonResp = new JSONObject(response.toString());
                JSONObject res = jsonResp.getJSONObject("response");
                String from = res.getString("from");

                String to = res.getString("to");
                double latitude = res.getDouble("latitude");
                double longitude = res.getDouble("longitude");

                /*locationsList.add(res.toString());
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView.getAdapter();
                adapter.clear();
                adapter.addAll(locationsList);
                adapter.notifyDataSetChanged();*/

            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else if (id == HTTP_SEND_REQUEST_ID) {
            try {
                jsonResp = new JSONObject(response.toString());
                JSONObject res = jsonResp.getJSONObject("response");
                boolean success = res.getBoolean("success");

                if (success) {
                    int idx = locationsList.indexOf(requestData);
                    if (idx > -1) {
                        locationsList.get(idx).setState(RequestData.State.SEND);
                        ((ItemRequestAdapter) listView.getAdapter()).notifyDataSetChanged();
                    }

                } else {
                    Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
