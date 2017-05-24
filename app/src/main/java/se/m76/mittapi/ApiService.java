package se.m76.mittapi;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import se.m76.mittapi.models.Stars;
import se.m76.mittapi.models.Trav;
import se.m76.mittapi.models.Travs;

/**
 * Created by Admin on 2017-01-31.
 */

public interface ApiService {

    @GET("star")
    Call<Stars> listStars();

    @GET("travs")
    Call<Travs> listTravs(@Query("lat")  double lat, @Query("lon") double lon);

    @POST("trav/")
    Call<Trav> newTrav(@Body Trav trav);
}
