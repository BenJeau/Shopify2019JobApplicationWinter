package com.benjeau.shopifywinter2019challenge;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Interface used to make Retrofit calls to the Shopify REST API
 */
public interface ShopifyService {
    @GET("admin/products.json?page=1&access_token=c32313df0d0ef512ca64d5b336a0d7c6")
    Call<Products> getJSON();
}
