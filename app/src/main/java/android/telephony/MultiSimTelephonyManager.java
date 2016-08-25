package android.telephony;

/**
 * Created by Marcus Becker on 24/08/2016.
 */
public interface MultiSimTelephonyManager {
    public void listen(PhoneStateListener listener, int events);
}
