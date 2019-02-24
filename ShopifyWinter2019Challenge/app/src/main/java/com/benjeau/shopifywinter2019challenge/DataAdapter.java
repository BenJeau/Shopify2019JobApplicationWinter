package com.benjeau.shopifywinter2019challenge;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Vibrator;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {

    /**
     * The data used to populate the RecyclerView
     */
    private ArrayList<Product> data;

    /**
     * Reference to the TagsActivity to be able to use the UI thread
     */
    private Activity activity;

    /**
     * Reference to the RecyclerView layout to create the Snackbar
     */
    private View snackbarParentLayout;

    /**
     * Keeps track of the position of the children when adding them
     * to alternate between between two RecyclerView children
     */
    private int position = 0;

    /**
     * Constructor for the RecyclerView adapter
     *
     * @param data The data used for the RecyclerView
     * @param snackbarParentLayout The view used for the Snackbar
     * @param activity The activity used for accessing the UI thread
     */
    public DataAdapter(ArrayList<Product> data, View snackbarParentLayout, Activity activity) {
        this.data = data;
        this.snackbarParentLayout = snackbarParentLayout;
        this.activity = activity;
    }

    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view;

        // Alternates the orientation of the content in the children views by changing layout
        if (position % 2 == 0) {
            view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.recycler_view_children_right, viewGroup, false);
        } else {
            view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.recycler_view_children_left, viewGroup, false);
        }

        position++;
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final DataAdapter.ViewHolder viewHolder, final int i) {
        // Sets the text for the title and the vendor of the product
        viewHolder.productTitle.setText(data.get(i).title);
        viewHolder.productVendor.setText(data.get(i).vendor);

        // Calculates the total quantity and the highest and lowest price
        int quantity = 0;
        double lowPrice = 0.0;
        double highPrice = 0.0;

        for (Variants j : data.get(i).variants) {
            quantity += Integer.parseInt(j.inventory_quantity);

            double currentPrice = Double.parseDouble(j.price);

            if (lowPrice == 0.0) {
                lowPrice = currentPrice;
                highPrice = currentPrice;
            } else if (currentPrice < lowPrice) {
                lowPrice = currentPrice;
            } else if (currentPrice > highPrice) {
                highPrice = currentPrice;
            }
        }
        final String quantityText = String.valueOf(quantity);

        // Formats the price text
        String priceText;
        DecimalFormat df = new DecimalFormat("#0.00");
        if (lowPrice == highPrice) {
            priceText = "$" + df.format(lowPrice);
        } else {
            priceText = "$" + df.format(lowPrice) + " - " + "$" + df.format(highPrice);
        }
        viewHolder.productPrice.setText(priceText);

        // Sets the custom layout and the description for the Snackbar
        final Snackbar snackbar = Snackbar.make(snackbarParentLayout, "", Toast.LENGTH_SHORT);
        LayoutInflater inflater = (LayoutInflater) viewHolder.itemView.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View snackView = inflater.inflate(R.layout.product_snackbar, null);
        TextView snackbarDescription = snackView.findViewById(R.id.snackbar_description);
        snackbarDescription.setText(data.get(i).body_html);

        // Removes the padding that the origin snackbar has and adds the custom Snackbar layout to it
        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
        snackbarLayout.setPadding(0, 0, 0, 0);
        snackbarLayout.addView(snackView, 0);

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    // Connects to URL, establishes a connection and creates a Bitmap
                    URL url = new URL(data.get(i).image.src);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.connect();
                    InputStream inputStream = httpURLConnection.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    // Gets the palette from the bitmap
                    Palette palette = Palette.from(bitmap).generate();

                    // Gets the color from the palette
                    final int dominantColor = palette.getDominantColor(Color.WHITE);
                    final int darkVibrantColor = palette.getDarkVibrantColor(Color.DKGRAY);

                    // Formats the string containing the quantity of the product, it
                    // colors the number while also converting the color integer to a HEX color
                    final String next = "<font color='" + String.format("#%06X", (0xFFFFFF &
                            darkVibrantColor)) + "'><b>" + quantityText + "</b></font> available";

                    // Updates the views in the UI thread
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewHolder.productImage.setImageBitmap(bitmap);
                            viewHolder.productTitle.setTextColor(darkVibrantColor);
                            viewHolder.productPrice.setTextColor(darkVibrantColor);
                            snackbar.getView().setBackgroundColor(darkVibrantColor);
                            viewHolder.productQuantity.setText(Html.fromHtml(next));
                            viewHolder.productImage.setBackgroundColor(lighten(dominantColor));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

        viewHolder.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Vibrates device when clicking on CardView and
                // shows the Snackbar with the products description
                Vibrator vibrator = (Vibrator) viewHolder.itemView.getContext()
                        .getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(50);
                snackbar.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * Lightens up a color by the specified factor
     *
     * @param color  The color to be lightened
     * @return The lightened up color
     */
    private int lighten(int color) {
        float factor = 0.5f;
        return Color.argb(Color.alpha(color),
                (int) ((Color.red(color) * (1 - factor) / 255 + factor) * 255),
                (int) ((Color.green(color) * (1 - factor) / 255 + factor) * 255),
                (int) ((Color.blue(color) * (1 - factor) / 255 + factor) * 255));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView productTitle;
        private TextView productVendor;
        private TextView productPrice;
        private TextView productQuantity;
        private ImageView productImage;
        private ConstraintLayout constraintLayout;

        public ViewHolder(View view) {
            super(view);

            // Initializes every view of each views in the each RecyclerView children
            productTitle = view.findViewById(R.id.product_title);
            productVendor = view.findViewById(R.id.product_vendor);
            productPrice = view.findViewById(R.id.product_price);
            productQuantity = view.findViewById(R.id.product_quantity);
            productImage = view.findViewById(R.id.product_image);
            constraintLayout = view.findViewById(R.id.product_layout);
        }
    }
}