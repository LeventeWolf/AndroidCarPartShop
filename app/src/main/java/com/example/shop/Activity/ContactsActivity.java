package com.example.shop.Activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.shop.R;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {
    private static final String LOG_TAG = ContactsActivity.class.getName();
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        listView = (ListView) findViewById(R.id.contactsListView);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 1);

        Log.i(LOG_TAG, "onCreate");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                List<String> contacts = getContacts();

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contacts);

                Animation animation;
                animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.contacts_animation);
                animation.setDuration(500);
                listView.startAnimation(animation);

                listView.setAdapter(arrayAdapter);
            } else {
                popToast("Please grant contact permission!:)", Toast.LENGTH_LONG);
            }
        }
    }

    private List<String> getContacts() {
        List<String> contacts = new ArrayList<>();
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        Log.i(LOG_TAG, "Name: " + name);
                        Log.i(LOG_TAG, "Phone Number: " + phoneNo);
                        contacts.add(name + " : " + phoneNo);
                    }
                    pCur.close();
                }
            }
        }
        if (cur != null) {
            cur.close();
        }

        return contacts;
    }


    public void navigate_to_shopping_list(View view) {
        Intent intent = new Intent(this, ShopListActivity.class);
        startActivity(intent);
    }

    private void popToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.getView().setBackgroundColor(Color.parseColor("#D7FF6464"));
        toast.show();
    }

    private void popToast(String message, int duration) {
        Toast toast = Toast.makeText(this, message, duration);
        toast.getView().setBackgroundColor(Color.parseColor("#D7FF6464"));
        toast.show();
    }
}
