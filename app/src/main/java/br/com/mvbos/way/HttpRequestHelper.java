package br.com.mvbos.way;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Marcus Becker on 13/08/2016.
 */
public class HttpRequestHelper {

    private final int id;
    private final String path;
    private final Map<String, String> params;
    private final HttpRequestHelperResult result;
    private String method = "POST";

    private Object extraData;
    private Exception error;

    private boolean assync = true;

    public HttpRequestHelper(int id, String path, Map<String, String> params, HttpRequestHelperResult result) {
        this.id = id;
        this.path = path;
        this.params = params == null ? new HashMap<String, String>(0) : params;
        this.result = result;
    }


    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void execute() {

        if (assync) {

            AsyncTask task = new AsyncTask<Object, Void, StringBuilder>() {
                @Override
                protected StringBuilder doInBackground(Object[] p) {
                    return makeRequest();
                }

                @Override
                protected void onPostExecute(StringBuilder response) {

                    if (result != null) {
                        result.recieveResult(id, response, extraData, error);
                    }
                }

            };

            task.execute(null);

        } else {
            StringBuilder response = makeRequest();
            if (result != null) {
                result.recieveResult(id, response, extraData, error);
            }
        }
    }

    @NonNull
    private StringBuilder makeRequest() {
        URL url;
        StringBuilder sb = new StringBuilder(100);

        try {
            url = new URL(path);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(3000);
            conn.setConnectTimeout(3000);
            conn.setRequestMethod(method);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

            writer.write(getPostDataString(params));

            writer.flush();
            writer.close();

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            os.close();
            conn.disconnect();

        } catch (Exception e) {
            this.error = e;
            e.printStackTrace();
        }

        return sb;
    }


    private String getPostDataString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (result.length() > 0)
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    public boolean isAssync() {
        return assync;
    }

    public void setAssync(boolean assync) {
        this.assync = assync;
    }

    public Object getExtraData() {
        return extraData;
    }

    public void setExtraData(Object extraData) {
        this.extraData = extraData;
    }
}
