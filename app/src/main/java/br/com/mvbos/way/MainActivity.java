package br.com.mvbos.way;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.Method;

import br.com.mvbos.way.core.Core;
import br.com.mvbos.way.core.Way;

public class MainActivity extends AppCompatActivity {

    private Way way;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = (Button) findViewById(R.id.btnStart);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView t = (TextView) findViewById(R.id.textPhoneNumber);
                way.setNumber(t.getText().toString());

                Intent i = new Intent(MainActivity.this, ViewLocationActivity.class);
                i.putExtra("way", way);

                Core.save(way, "list.way", MainActivity.this);

                startActivity(i);
            }
        });

        way = Core.load("list.way", this);
        if (way != null) {
            TextView t = (TextView) findViewById(R.id.textPhoneNumber);
            t.setText(way.getNumber());

        } else {
            way = new Way(getPhoneNumber());
        }

    }


    public String getPhoneNumber() {
        String phoneNumber;
        TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneNumber = tel.getLine1Number();

        if (phoneNumber.isEmpty()) {
            //String imsiSIM1 = getDeviceIdBySlot(this, 0);
            //String imsiSIM2 = getDeviceIdBySlot(this, 1);

            //phoneNumber = tel.getSubscriberId();
            phoneNumber = "55119";
        }

        return phoneNumber;
    }


    private static String getDeviceIdBySlot(Context context, int slotID) {
        final String methodName = "getSimStateDs";

        String imsi = null;

        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        try {
            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());
            for (Method m : telephonyClass.getMethods()) {
                System.out.println(m);
            }

            Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimID = telephonyClass.getMethod(methodName, parameter);

            Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            Object ob_phone = getSimID.invoke(telephony, obParameter);

            if (ob_phone != null) {
                TelephonyManager temp = (TelephonyManager) ob_phone;
                imsi = temp.getLine1Number();
                //imsi = ob_phone.toString();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return imsi;
    }
}
