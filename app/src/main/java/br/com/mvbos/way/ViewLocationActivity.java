package br.com.mvbos.way;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.mvbos.way.core.Core;
import br.com.mvbos.way.core.ItemRequestAdapter;
import br.com.mvbos.way.core.RequestData;
import br.com.mvbos.way.core.Way;

public class ViewLocationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, HttpRequestHelperResult {

    private static final long MIN_SECONDS = 5 * 60 * 1000;
    private static final float MIN_METERS = 0; // 10000;
    private static final int RS_REQ_GET_CONTACT = 1;

    private static final int HTTP_ID_SEND = 1;
    private static final int HTTP_ID_UPDATE = 2;
    private static final int HTTP_ID_SEND_REQUEST = 3;
    private static final int HTTP_ID_ACCEPT = 4;

    public static final String URL_GOOGLE_MAPS = "http://maps.google.com/?q=%s,%s";
    public static final int DELAY_MILLIS = 75 * 1000;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "REQUESTING_LOCATION_UPDATES_KEY";
    private static final String LOCATION_KEY = "LOCATION_KEY";
    private static final String LAST_UPDATED_TIME_STRING_KEY = "LAST_UPDATED_TIME_STRING_KEY";

    private String preferred;

    private TextView textLocation;
    private boolean send;

    private Way way;

    private ListView listView;
    private final List<RequestData> locationsList = new ArrayList<>(30);

    //private Intent mServiceIntent;

    private Location myLocation;
    private Location oldLocation;
    private String phoneNumber;

    private RequestData requestDataSelected; //item selected on context menu

    private final String path = "http://mvbos.com.br/ondetatu/list_location.php";
    //private final String path = "http://192.168.0.7/ondetatu/list_location.php";

    //private long startTime = 0;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            Log.i(ViewLocationActivity.class.getName(), "run timer");


            Map<String, String> param = new HashMap<>(5);
            param.put("str_0", "upchkreqloc");
            param.put("str_12", phoneNumber);
            param.put("str_11", "2215");

            if (myLocation == null) {
                myLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            }

            if (myLocation != oldLocation) {
                oldLocation = myLocation;
                param.put("str_7", String.valueOf(myLocation.getLatitude()));
                param.put("str_9", String.valueOf(myLocation.getLongitude()));

            } else {
                param.put("str_7", "0");
                param.put("str_9", "0");
            }

            HttpRequestHelper httpHelper = new HttpRequestHelper(HTTP_ID_UPDATE, path, param, ViewLocationActivity.this);
            httpHelper.execute();

            timerHandler.postDelayed(this, DELAY_MILLIS);
        }
    };

    private boolean mRequestingLocationUpdates;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;

    private void startLocationUpdates() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(MIN_SECONDS)
                .setFastestInterval(1 * 1000);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void updateUI() {
        oldLocation = null;
        if (myLocation != null) {
            String s = String.format("Your location: Latitude %.4f, Longitude %.4f.", myLocation.getLatitude(), myLocation.getLongitude());
            textLocation.setText(s);
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
            }

            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                myLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }

            updateUI();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_location);


        updateValuesFromBundle(savedInstanceState);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        way = (Way) getIntent().getSerializableExtra("way");
        phoneNumber = way.getNumber();

        textLocation = (TextView) findViewById(R.id.txtLatLong);
        textLocation.setText("No location avaliable.");

        Button btn = (Button) findViewById(R.id.btnViewMyPosition);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myLocation != null) {
                    openMap(myLocation.getLatitude(), myLocation.getLongitude());
                } else {
                    Toast.makeText(ViewLocationActivity.this, "Sorry, location is unknown.", Toast.LENGTH_SHORT).show();
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
                } else {
                    ViewLocationActivity.this.openContextMenu(view);
                }
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();

        } else if (!mGoogleApiClient.isConnected()) {
            //Toast.makeText(ViewLocationActivity.this, "Please turn on your GPS.", Toast.LENGTH_SHORT).show();
        }

        if (locationsList.isEmpty() || way == null) {
            if (way == null)
                way = Core.load("list.way", this);

            List<RequestData> temp = way.getRequestDatas();

            phoneNumber = way.getNumber();

            if (temp != null) {
                locationsList.addAll(temp);
                updteItemListView(true);
            }
        }

        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState, PersistableBundle
            outPersistentState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, myLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);

        super.onSaveInstanceState(savedInstanceState, outPersistentState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo
            menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (R.id.listView == v.getId()) {
            ListView lv = (ListView) v;

            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            requestDataSelected = locationsList.get(info.position);

            if (requestDataSelected.getState() == RequestData.State.PENDING) {
                menu.add("Accept");

            } else if (requestDataSelected.getState() == RequestData.State.ERROR) {
                menu.add("Retry");
            }

            menu.add("Remove");
        }

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        startLocationUpdates();
        //myLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        myLocation = location;
        oldLocation = null;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if ("Remove".equals(item.getTitle())) {
            locationsList.remove(requestDataSelected);
            updteItemListView(true);

            way.setRequestDatas(locationsList);
            Core.save(way, "list.way", this);

        } else if ("Accept".equals(item.getTitle())) {

            Map<String, String> param = new HashMap<>(5);
            param.put("str_0", "acploc");
            param.put("str_12", String.valueOf(requestDataSelected.getFromNumber()));
            param.put("str_22", String.valueOf(requestDataSelected.getToNumber()));
            param.put("str_11", requestDataSelected.getKey());

            HttpRequestHelper httpHelper = new HttpRequestHelper(HTTP_ID_ACCEPT, path, param, this);
            httpHelper.setExtraData(requestDataSelected);
            httpHelper.execute();

        } else if ("Retry".equals(item.getTitle())) {

            if (requestDataSelected.getState() == RequestData.State.ERROR) {

                try {
                    if (requestDataSelected.getType() == RequestData.Type.REQUEST) {
                        requestDataSelected.setState(RequestData.State.WAITING);
                        updteItemListView(false);

                        RequestData[] arr = {requestDataSelected};
                        sendRequestContactLocation(Arrays.asList(arr));

                    } else if (requestDataSelected.getType() == RequestData.Type.SEND) {
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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

                req.setType(send ? RequestData.Type.SEND : RequestData.Type.REQUEST);

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

        try {
            addItemList(contacts.get(0));

            if (send) {
                sendMyLocation(contacts);
            } else {
                sendRequestContactLocation(contacts);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addItemList(RequestData requestData) {
        if (!locationsList.contains(requestData)) {

            locationsList.add(requestData);
            ItemRequestAdapter adapter = (ItemRequestAdapter) listView.getAdapter();
            adapter.add(requestData);
            adapter.notifyDataSetChanged();

            way.setRequestDatas(locationsList);
            Core.save(way, "list.way", this);
        }
    }


    private String sendMyLocation(List<RequestData> numbers) throws Exception {
        HttpRequestHelper httpHelper = new HttpRequestHelper(HTTP_ID_SEND, path, null, this);
        httpHelper.execute();

        return null;
    }

    private String sendRequestContactLocation(List<RequestData> contacts) throws Exception {
        RequestData req = contacts.get(0);

        Map<String, String> param = new HashMap<>(5);
        param.put("str_0", "reqloc");
        param.put("str_12", String.valueOf(req.getFromNumber()));
        param.put("str_11", "2215");
        param.put("str_22", String.valueOf(req.getToNumber()));

        HttpRequestHelper httpHelper = new HttpRequestHelper(HTTP_ID_SEND_REQUEST, path, param, this);
        httpHelper.setExtraData(req);
        httpHelper.execute();

        return null;
    }

    @Override
    public void onStart() {
        super.onStart();

        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
        way.setRequestDatas(locationsList);
        Core.save(way, "list.way", this);
    }

    @Override
    public void recieveResult(int id, StringBuilder response, Object extraData, Exception error) {
        JSONObject jsonResp = null;

        RequestData requestData = (RequestData) extraData;


        if (error != null) {
            if (extraData != null) {
                ((RequestData) extraData).setState(RequestData.State.ERROR);
                updteItemListView(false);
            }

            return;
        }

        if (id == HTTP_ID_SEND) {
            try {
                jsonResp = new JSONObject(response.toString());
                JSONObject res = jsonResp.getJSONObject("response");
                String from = res.getString("from");

                String to = res.getString("to");
                double latitude = res.getDouble("latitude");
                double longitude = res.getDouble("longitude");

                //updteItemListView(true);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else if (id == HTTP_ID_SEND_REQUEST) {
            try {
                jsonResp = new JSONObject(response.toString());
                JSONObject res = jsonResp.getJSONObject("response");
                boolean success = res.getBoolean("success");

                if (success) {
                    requestData.setState(RequestData.State.SEND);

                } else {
                    requestData.setState(RequestData.State.ERROR);
                    Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
                }

                updteItemListView(false);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else if (id == HTTP_ID_UPDATE) {
            //Log.i(ViewLocationActivity.class.getName(), response.toString());

            boolean update = false;

            try {
                jsonResp = new JSONObject(response.toString());
                JSONArray resp = jsonResp.getJSONArray("response");

                for (int i = 0; i < resp.length(); i++) {
                    JSONObject line = resp.getJSONObject(i);

                    for (RequestData r : locationsList) {
                        if (r.getToNumber() == line.getLong("from")) {

                            update = true;

                            r.setLatitude(line.getDouble("latitude"));
                            r.setLongitude(line.getDouble("longitude"));
                            r.setState(RequestData.State.SYNC);

                            break;
                        }
                    }
                }


                JSONArray reqs = jsonResp.getJSONArray("requets");

                for (int i = 0; i < reqs.length(); i++) {
                    update = true;
                    JSONObject line = reqs.getJSONObject(i);

                    RequestData temp = new RequestData();
                    temp.setForeign(true);
                    temp.setState(RequestData.State.PENDING);
                    temp.setKey(line.getString("key"));
                    temp.setFromNumber(line.getString("from"));
                    temp.setToNumber(phoneNumber);

                    int idx = locationsList.indexOf(temp);

                    if (idx == -1) {
                        locationsList.add(temp);
                    } else {
                        temp = locationsList.get(idx);
                        temp.setLastUpdate(new Date());
                    }
                }


                if (update) {
                    updteItemListView(true);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else if (id == HTTP_ID_ACCEPT) {
            //echo '{"response": {"success": true, "key":' . $key . ', "message": ""}}';

            try {
                jsonResp = new JSONObject(response.toString());
                JSONObject res = jsonResp.getJSONObject("response");
                boolean success = res.getBoolean("success");

                if (success) {
                    int idx = locationsList.indexOf(requestData);
                    if (idx > -1) {
                        locationsList.get(idx).setState(RequestData.State.ACCEPTED);
                        updteItemListView(false);

                        oldLocation = null;
                    }

                } else {
                    Toast.makeText(this, "Error to accept request.", Toast.LENGTH_LONG).show();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    private void updteItemListView(boolean full) {
        if (full) {
            ItemRequestAdapter adapter = (ItemRequestAdapter) listView.getAdapter();
            adapter.clear();
            adapter.addAll(locationsList);
            adapter.notifyDataSetChanged();

        } else {
            ((ItemRequestAdapter) listView.getAdapter()).notifyDataSetChanged();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //System.out.println(connectionResult);
    }
}
