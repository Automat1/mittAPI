package se.m76.mittapi;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import se.m76.mittapi.models.Star;
import se.m76.mittapi.models.Stars;

/**
 * Created by Admin on 2017-01-31.
 */

public interface MittApiService {
    @GET("star")
    Call<Stars> listStars();
}
