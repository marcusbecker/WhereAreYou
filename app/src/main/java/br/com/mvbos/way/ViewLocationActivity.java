package br.com.mvbos.way;

import android.app.Activity;
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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class ViewLocationActivity extends AppCompatActivity implements LocationListener, HttpRequestHelperResult {

    private static final long MIN_SECONDS = 5 * 1;
    private static final float MIN_METERS = 1;
    private static final int RS_REQ_GET_CONTACT = 1;
    private static final int HTTP_SEND_ID = 1;

    private LocationManager locationManager;
    private String preferred;

    private TextView textLocation;
    private boolean send;

    private ListView listView;
    private final List<String> locationsList = new ArrayList<>(30);

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

                    String url = String.format("http://maps.google.com/?q=%s,%s", location.getLatitude(), location.getLongitude());

                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
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


        listView = (ListView) findViewById(R.id.listView);
        String[] values = new String[]{"No data avaliable"};
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>(Arrays.asList(values)));
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition = position;
                String itemValue = (String) listView.getItemAtPosition(position);

                Toast.makeText(getApplicationContext(), "Position :" + itemPosition + "  ListItem : " + itemValue, Toast.LENGTH_LONG).show();
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();

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
            if (locationManager != null) {
                locationManager.removeUpdates(ViewLocationActivity.this);
            }

        } catch (SecurityException e) {
        }
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


    private List<String> getContactNumber(Intent data) {
        Uri resultUri = data.getData();
        Cursor cont = getContentResolver().query(resultUri, null, null, null, null);

        if (!cont.moveToNext()) {
            return Collections.EMPTY_LIST;
        }

        List<String> lst = new ArrayList<>(5);

        String aNumber = null;

        int columnIndexForId = cont.getColumnIndex(ContactsContract.Contacts._ID);
        String contactId = cont.getString(columnIndexForId);
        int hasPhoneCol = cont.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
        boolean hasPhone = cont.getInt(hasPhoneCol) != 0;

        if (hasPhone) {

            Cursor numbers = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId, null, null);

            while (numbers.moveToNext()) {
                aNumber = numbers.getString(numbers.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                lst.add(aNumber);
            }

            numbers.close();
        }

        cont.close();

        return lst;
    }

    private void sendPhoneLocation(Intent data) {
        List<String> numbers = getContactNumber(data);
        if (numbers.isEmpty()) {
            Toast.makeText(this, "Selected contact seems to have no phone numbers ", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            if (send) {
                makeRequest(numbers);
            } else {
                makeAsk(numbers);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String makeRequest(List<String> numbers) throws Exception {

        final String path = "http://mvbos.com.br/ondetatu/list_location.php";

        HttpRequestHelper httpHelper = new HttpRequestHelper(HTTP_SEND_ID, path, null, this);
        httpHelper.execute();

        return null;
    }

    private String makeAsk(List<String> numbers) throws Exception {
        final String path = "http://mvbos.com.br/ondetatu/list_location.php";

        HttpRequestHelper httpHelper = new HttpRequestHelper(HTTP_SEND_ID, path, null, this);
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
    public void recieveResult(int id, StringBuilder response) {
        JSONObject jsonResp = null;

        if (id == HTTP_SEND_ID) {
            try {
                jsonResp = new JSONObject(response.toString());
                JSONObject res = jsonResp.getJSONObject("response");
                String from = res.getString("from");

                String to = res.getString("to");
                double latitude = res.getDouble("latitude");
                double longitude = res.getDouble("longitude");

                locationsList.add(res.toString());
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView.getAdapter();
                adapter.clear();
                adapter.addAll(locationsList);
                adapter.notifyDataSetChanged();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}