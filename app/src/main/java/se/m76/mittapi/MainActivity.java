package se.m76.mittapi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import se.m76.mittapi.models.Star;
import se.m76.mittapi.models.Stars;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Startar ...");

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.226:5000/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        //Star star = new Star();

        MittApiService service = retrofit.create(MittApiService.class);

        Call<Stars> calll = service.listStars();
        calll.enqueue(new Callback<Stars>() {
            @Override
            public void onResponse(Call<Stars> call, Response<Stars> response) {
                int statusCode = response.code();
                Log.i(TAG,"Fick data?");
                Log.i(TAG, String.valueOf(response.body()));
                Log.i(TAG, " " + response.body().getResult().size());
                Log.i(TAG, "Namn:   " + response.body().getResult().get(0).getName());
            }

            @Override
            public void onFailure(Call<Stars> call, Throwable t) {
                Log.e(TAG,"Gick inte det!!",t);
            }
        });

    }
}

