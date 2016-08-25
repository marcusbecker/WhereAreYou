package br.com.mvbos.way;

import android.telephony.PhoneStateListener;

import java.lang.reflect.Field;

/**
 * Created by Marcus Becker on 24/08/2016.
 */
public class MultiSimListener extends PhoneStateListener {

    private Field subscriptionField;
    private int simSlot = -1;

    public MultiSimListener(int simSlot) {
        super();
        try {
            // Get the protected field mSubscription of PhoneStateListener and set it
            subscriptionField = this.getClass().getSuperclass().getDeclaredField("mSubscription");
            subscriptionField.setAccessible(true);
            subscriptionField.set(this, simSlot);
            this.simSlot = simSlot;
        } catch (NoSuchFieldException e) {

        } catch (IllegalAccessException e) {

        } catch (IllegalArgumentException e) {

        }
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        // Handle the event here, with state, incomingNumber and simSlot
    }

}
