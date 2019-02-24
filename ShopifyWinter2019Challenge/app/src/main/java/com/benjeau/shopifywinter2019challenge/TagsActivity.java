package com.benjeau.shopifywinter2019challenge;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.chip.Chip;
import android.support.design.chip.ChipGroup;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import retrofit2.converter.gson.GsonConverterFactory;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TagsActivity extends AppCompatActivity {

    /**
     * The root layout of the TagsActivity
     */
    private ConstraintLayout rootLayout;

    /**
     * The ChipGroup found in the TagsActivity used to store the tags
     */
    private ChipGroup tags;

    /**
     * The formatted data after receiving from Retrofit call, where each key
     * is a key and the value is a Set of product ids
     */
    private Map<String, HashSet<String>> data;

    /**
     * The shared preferences of the application
     */
    private SharedPreferences prefs;

    /**
     * The base of the url used for the Retrofit call
     */
    private static final String BASE_URL = "https://shopicruit.myshopify.com";

    /**
     * The shared preferences key containing a string set of the names of the tags
     */
    private static final String PREF_KEY_TAGS = "nameOfTags";

    /**
     * The intent key used to send the selected tag name
     */
    public static final String INTENT_KEY = "data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tags);

        // Gets the shared preferences manager
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Gets the views
        rootLayout = findViewById(R.id.root);
        tags = findViewById(R.id.chip_group);

        // Sets the content to the app bar
        TextView tagsTitle = findViewById(R.id.app_bar_title);
        TextView tagsDescription = findViewById(R.id.app_bar_description);

        tagsTitle.setText(getResources().getText(R.string.tags_title));
        tagsDescription.setText(getResources().getText(R.string.tags_description));

        tagsTitle.setAlpha(1);
        tagsDescription.setAlpha(0.7f);

        // Adds padding to the scroll view for it to start after
        // the app bar while also being able to go under it
        ConstraintLayout constraintLayout = findViewById(R.id.app_bar);
        final NestedScrollView scrollView = findViewById(R.id.scroll_view);
        constraintLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                scrollView.setPadding(0, bottom - top, 0, scrollView.getPaddingBottom());
            }
        });

        // Adds the padding to the root layout or the chip group for the onscreen navigation bar
        // if there is one and adds it depending on the orientation of the device
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = display.getRotation();
        if (orientation == 0) {
            tags.setPadding(tags.getPaddingLeft(),
                    tags.getPaddingTop(),
                    tags.getPaddingRight(),
                    tags.getPaddingBottom() + getNavigationBarSize(getApplicationContext()).y);
        } else if (orientation == 1) {
            rootLayout.setPadding(rootLayout.getPaddingLeft(),
                    rootLayout.getPaddingTop(),
                    rootLayout.getPaddingRight() + getNavigationBarSize(getApplicationContext()).x,
                    rootLayout.getPaddingBottom());
        } else if (orientation == 3) {
            rootLayout.setPadding(rootLayout.getPaddingLeft() + TagsActivity.getNavigationBarSize(getApplicationContext()).x,
                    rootLayout.getPaddingTop(),
                    rootLayout.getPaddingRight(),
                    rootLayout.getPaddingBottom());
        }

        // Checks if the data has already been loaded
        Set<String> nameOfTags = prefs.getStringSet(PREF_KEY_TAGS, null);
        if (nameOfTags == null) {
            // Calls the Shopify API to get the data
            loadData();
        } else {
            // Sorts the Set and populates the ChipGroup with the data in Shared Preferences
            nameOfTags = new TreeSet<String>(nameOfTags);
            populateChipGroup(nameOfTags.toArray(new String[nameOfTags.size()]));
        }
    }

    /**
     * Populates the ChipGroup with Chips, each containing the name of a tag
     *
     * @param listOfTags A string array containing the names of the tags
     */
    private void populateChipGroup(final String[] listOfTags) {

        // Iterates through the array of tags
        for (int i = 0; i < listOfTags.length; i++) {
            final String currentTag = listOfTags[i];

            // Creates and formats each chip in the chip group
            final Chip te = new Chip(tags.getContext());
            te.setAlpha(0.0f);
            te.setClickable(true);
            te.setElevation(15.0f);
            te.setText(listOfTags[i]);
            te.setTypeface(ResourcesCompat.getFont(this, R.font.raleway));
            te.setChipBackgroundColor(ColorStateList.valueOf(Color.WHITE));
            te.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Vibrator vibrator = (Vibrator) getApplicationContext()
                            .getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(50);
                    Intent intent = new Intent(TagsActivity.this, ProductsActivity.class);
                    intent.putExtra(INTENT_KEY, currentTag);

                    startActivity(intent);
                }
            });

            tags.addView(te);

            // Creates the chip delayed fade in animation
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    te.animate().alpha(1.0f);
                }
            }, (tags.getChildCount() * 2 + 50 - i * 2) * i);
        }
    }

    /**
     * Gets the information for the products and the tags from the Shopify API
     */
    private void loadData() {
        // Uses Retrofit to get JSON, to create a requestInterface and to call that website
        Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();
        ShopifyService requestInterfaceMain = retrofit.create(ShopifyService.class);

        Call<Products> call = requestInterfaceMain.getJSON();
        call.enqueue(new Callback<Products>() {
            @Override
            public void onResponse(Call<Products> call, Response<Products> response) {
                // Gets data from JSON and formats it for easier tag manipulation
                Product[] jsonResponseMain = response.body().products;
                getTags(jsonResponseMain);

                SharedPreferences.Editor prefsEdit = prefs.edit();

                // Adds the information about the tags and product ids to the shared preferences
                // for faster app loading after already opening it once
                for (Map.Entry<String, HashSet<String>> i : data.entrySet()) {
                    prefsEdit.putStringSet(i.getKey(), i.getValue());
                }

                // Adds the 'raw' information about the products to the shared preferences
                for (Product i : jsonResponseMain) {
                    Gson gson = new Gson();
                    prefsEdit.putString(i.id, gson.toJson(i));
                }

                Set<String> nameOfTags = new TreeSet<String>(data.keySet());
                prefsEdit.putStringSet(PREF_KEY_TAGS, nameOfTags);
                prefsEdit.apply();

                // Adds the chips to the chip group with the name of the tags sorted
                populateChipGroup(nameOfTags.toArray(new String[nameOfTags.size()]));
            }

            @Override
            public void onFailure(Call<Products> call, Throwable t) {
                Log.d("Error", t.getMessage());
            }
        });
    }

    /**
     * Creates a HashMap containing the tags as the keys and a set
     * of product ids that have that specified tag
     *
     * @param data The data that has been received from Retrofit call
     */
    private void getTags(Product[] data) {
        this.data = new HashMap<String, HashSet<String>>();

        for (Product product : data) {
            // Gets the tags of the current product
            String[] tags = product.tags.split(",");
            for (int j = 0; j < tags.length; j++) {
                tags[j] = tags[j].trim();
            }

            // Iterates through the tags, creates a set (if not already created
            // for the specified tag), and adds the product ids to the set
            for (String tag : tags) {
                HashSet<String> ids;
                if (this.data.containsKey(tag)) {
                    ids = this.data.get(tag);
                } else {
                    ids = new HashSet<String>();
                }
                ids.add(product.id);
                this.data.put(tag, ids);
            }
        }
    }

    /**
     * Returns the size of the navigation bar, if there's one
     *
     * @param context The context of the application to get the size of the display
     * @return A point representing the width and height of the navigation bar
     */
    public static Point getNavigationBarSize(Context context) {
        Point navbar = new Point();

        // Gets the display
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

        // Gets the usable display size
        Point appUsableSize = new Point();
        display.getSize(appUsableSize);

        // Gets the display size
        Point realScreenSize = new Point();
        display.getRealSize(realScreenSize);

        // Checks if the navigation bar is at the bottom/side
        // by comparing the y/x coordinates of size
        if (appUsableSize.y < realScreenSize.y) {
            navbar = new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
        } else if (appUsableSize.x < realScreenSize.x) {
            navbar = new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
        }

        // If there's no navigation bar, return an empty one
        return navbar;
    }
}
