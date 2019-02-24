package com.benjeau.shopifywinter2019challenge;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProductsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        // Sets the description for the app bar
        final TextView tagDescription = findViewById(R.id.app_bar_description);
        tagDescription.setText(getResources().getText(R.string.product_description));

        // Adds padding to the scroll view for it to start after
        // the app bar while also being able to go under it
        final NestedScrollView scrollView = findViewById(R.id.scroll_view);
        ConstraintLayout constraintLayout = findViewById(R.id.app_bar);
        constraintLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                scrollView.setPadding(0, bottom - top, 0, 0);
            }
        });

        // Locks the orientation of the screen
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        // Gets the name of the tag that has been clicked
        String info = getIntent().getExtras().getString(TagsActivity.INTENT_KEY, "Products");

        // Sets the title of the activity to the name of the tag
        final TextView tagTitle = findViewById(R.id.app_bar_title);
        tagTitle.setText(info);

        // Initializes the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.list_recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);

        // Gets the product ids that has the specified tag
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Set<String> productIds = prefs.getStringSet(info, null);

        if (productIds != null) {
            List<String> list = new ArrayList<String>(productIds);
            ArrayList<Product> data = new ArrayList<Product>();
            Gson gson = new Gson();

            // Gets the information of every Product with the specified product if
            for (String i : list) {
                String t = prefs.getString(i, null);
                data.add(gson.fromJson(t, Product.class));
            }

            // Sets up RecyclerView's adapter for it to be able to manipulate the data
            DataAdapter adapter = new DataAdapter(data, recyclerView, this);
            recyclerView.setAdapter(adapter);

            // Animates the content in the app bar
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    tagTitle.animate().alpha(1.0f);
                    tagDescription.animate().alpha(0.7f);
                }
            }, 500);
        }

        // Adds the padding to the recycler view or the root layout for the onscreen navigation
        // bar if there is one and adds it depending on the orientation of the device
        ConstraintLayout rootLayout = findViewById(R.id.root);
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = display.getRotation();
        if (orientation == 0) {
            recyclerView.setPadding(recyclerView.getPaddingLeft(),
                    recyclerView.getPaddingTop(),
                    recyclerView.getPaddingRight(),
                    recyclerView.getPaddingBottom() + TagsActivity.getNavigationBarSize(getApplicationContext()).y);
        } else if (orientation == 1) {
            rootLayout.setPadding(rootLayout.getPaddingLeft(),
                    rootLayout.getPaddingTop(),
                    rootLayout.getPaddingRight() + TagsActivity.getNavigationBarSize(getApplicationContext()).x,
                    rootLayout.getPaddingBottom());
        } else if (orientation == 3) {
            rootLayout.setPadding(rootLayout.getPaddingLeft() + TagsActivity.getNavigationBarSize(getApplicationContext()).x,
                    rootLayout.getPaddingTop(),
                    rootLayout.getPaddingRight(),
                    rootLayout.getPaddingBottom());
        }
    }
}