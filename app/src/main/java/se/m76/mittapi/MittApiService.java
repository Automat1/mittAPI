package se.m76.mittapi;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import se.m76.mittapi.models.Star;
import se.m76.mittapi.models.Stars;
import se.m76.mittapi.models.Trav;
import se.m76.mittapi.models.Travs;

/**
 * Created by Admin on 2017-01-31.
 */

public interface MittApiService {

    @GET("star")
    Call<Stars> listStars();

    @GET("travs")
    Call<Travs> listTravs();

    @POST("travs/new")
    Call<Travs> newTrav(@Body Travs travs);
}
