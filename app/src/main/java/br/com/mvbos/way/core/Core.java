package br.com.mvbos.way.core;

import android.app.Activity;
import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Marcus Becker on 17/08/2016.
 */
public class Core {

    public static boolean save(Way obj, String fileName, Activity context) {
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(obj);
            os.close();
            fos.close();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static Way load(String fileName, Activity context) {
        try {
            FileInputStream fis = context.openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fis);
            Way simpleClass = (Way) is.readObject();
            is.close();
            fis.close();

            return simpleClass;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
