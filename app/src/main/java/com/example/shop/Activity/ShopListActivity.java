package com.example.shop.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shop.Model.CarPart;
import com.example.shop.Notification.NotificationHelper;
import com.example.shop.Notification.NotificationJobService;
import com.example.shop.R;
import com.example.shop.Service.CarPartService;
import com.example.shop.ShoppingItemAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ShopListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String>  {
    private static final String LOG_TAG = ShopListActivity.class.getName();
    private FirebaseUser user;

    private FrameLayout redCircle;
    private TextView countTextView;
    private int cartItems = 0;
    private int gridNumber = 1;
    private Integer itemLimit = 5;

    // Member variables.
    private RecyclerView mRecyclerView;
    private ArrayList<CarPart> mItemsData;
    private ShoppingItemAdapter mAdapter;

    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;

    private NotificationHelper mNotificationHelper;
    private AlarmManager mAlarmManager;
    private JobScheduler mJobScheduler;
    private SharedPreferences preferences;

    private boolean viewRow = true;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_list);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d(LOG_TAG, "Unauthenticated user!");
            finish();
        }

        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, gridNumber));
        mItemsData = new ArrayList<>();
        mAdapter = new ShoppingItemAdapter(this, mItemsData);
        mRecyclerView.setAdapter(mAdapter);

        mFirestore = FirebaseFirestore.getInstance();
        mItems = mFirestore.collection("Items");
        initItemsFromFirebase();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        this.registerReceiver(powerReceiver, filter);

        mNotificationHelper = new NotificationHelper(this);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mJobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        // setAlarmManager();
        setJobScheduler();

        getSupportLoaderManager().restartLoader(0, null, this);
    }

    BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();

            if (intentAction == null)
                return;

            switch (intentAction) {
                case Intent.ACTION_POWER_CONNECTED:
                    itemLimit = 10;
                    initItemsFromFirebase();
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    itemLimit = 5;
                    initItemsFromFirebase();
                    break;
            }
        }
    };

    private void initItemsFromResources() {
        String[] itemsList = getResources().getStringArray(R.array.shopping_item_names);
        String[] itemsInfo = getResources().getStringArray(R.array.shopping_item_desc);
        String[] itemsPrice = getResources().getStringArray(R.array.shopping_item_price);
        TypedArray itemsImageResources = getResources().obtainTypedArray(R.array.shopping_item_images);
        TypedArray itemRate = getResources().obtainTypedArray(R.array.shopping_item_rates);

        for (int i = 0; i < itemsList.length; i++) {
            mItems.add(new CarPart(
                 itemsList[i],
                 itemsInfo[i],
                 itemsPrice[i],
                 itemRate.getFloat(i, 0),
                 itemsImageResources.getResourceId(i, 0),
                 0));
        }

        itemsImageResources.recycle();

        popToast("Termékek újratöltve!");
    }

    /**
     * Initialize data from firebase, ordered by name in ascending order.
     */
    private void initItemsFromFirebase() {
        mItemsData.clear();
        mItems.orderBy("name", Query.Direction.ASCENDING).limit(itemLimit).get()
              .addOnSuccessListener(queryDocumentSnapshots -> {
                  Log.d(LOG_TAG, "Init data!");
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        CarPart item = document.toObject(CarPart.class);
                        item.setId(document.getId());
                        mItemsData.add(item);
                    }

                    if (mItemsData.size() == 0) {
                        initItemsFromResources();
                        initItemsFromFirebase();
                    }

                    // Notify the adapter of the change.
                    mAdapter.notifyDataSetChanged();
                });
    }

    public void deleteItem(CarPart item) {
        DocumentReference ref = mItems.document(item._getId());
        ref.delete().addOnSuccessListener(success -> popToast(item.getName() + " sikeresen törölve!"))
                    .addOnFailureListener(fail -> popToast("HIBA " + item.getName() + " törlése során!"));

        initItemsFromFirebase();
        mNotificationHelper.cancel();
    }

    private void popToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.getView().setBackgroundColor(Color.parseColor("#D7FF6464"));
        toast.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.shop_list_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search_bar);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(LOG_TAG, s);
                mAdapter.getFilter().filter(s);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.log_out_button:
                Log.d(LOG_TAG, "Logout clicked!");
                FirebaseAuth.getInstance().signOut();
                finish();
                return true;
            case R.id.contacts:
                Log.d(LOG_TAG, "Contacts clicked!");
                Intent intent = new Intent(this, ContactsActivity.class);
                startActivity(intent);
                return true;
            case R.id.cart:
                Log.d(LOG_TAG, "Cart clicked!");
                return true;
            case R.id.view_selector:
                if (viewRow) {
                    changeSpanCount(item, R.drawable.ic_view_grid, 1);
                } else {
                    changeSpanCount(item, R.drawable.ic_view_row, 2);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void changeSpanCount(MenuItem item, int drawableId, int spanCount) {
        viewRow = !viewRow;
        item.setIcon(drawableId);
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.setSpanCount(spanCount);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem alertMenuItem = menu.findItem(R.id.cart);
        FrameLayout rootView = (FrameLayout) alertMenuItem.getActionView();

        redCircle = (FrameLayout) rootView.findViewById(R.id.view_alert_red_circle);
        countTextView = (TextView) rootView.findViewById(R.id.view_alert_count_textview);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(alertMenuItem);
            }
        });
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Update cart badge icon <br>
     * Send notification
     * @param item The item that we place to the cart
     */
    public void handleAddToCart(CarPart item) {
        cartItems += 1;

        // Update badge
        updateBadge(item);

        // Send notification
        mNotificationHelper.send(item.getName() + " a kosárba helyezve!");
    }

    private void updateBadge(CarPart item) {
        countTextView.setText(String.valueOf(cartItems));

        redCircle.setVisibility((cartItems > 0) ? VISIBLE : GONE);

        mItems.document(item._getId()).update("cartedCount", item.getCartedCount() + 1)
            .addOnFailureListener(fail -> Toast.makeText(this, item.getName() + " badge hiba!", Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(powerReceiver);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setJobScheduler() {
        // SeekBar, Switch, RadioButton
        int networkType = JobInfo.NETWORK_TYPE_UNMETERED;
        Boolean isDeviceCharging = true;
        int hardDeadline = 5000; // 5 * 1000 ms = 5 sec.

        ComponentName serviceName = new ComponentName(getPackageName(), NotificationJobService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceName)
                .setRequiredNetworkType(networkType)
                .setRequiresCharging(isDeviceCharging)
                .setOverrideDeadline(hardDeadline);

        JobInfo jobInfo = builder.build();
        mJobScheduler.schedule(jobInfo);
    }


    // AsyncTaskLoader

    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {
        return new CarPartService(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<String> loader, String data) {
        popToast(data);
        Log.d(LOG_TAG, "AsyncTaskLoader Finished!");
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String> loader) {

    }
}
