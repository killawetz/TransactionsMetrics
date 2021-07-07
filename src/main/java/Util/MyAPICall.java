package Util;

import Model.MovieModel;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;

public interface MyAPICall {

    //https://api.themoviedb.org/ 550?api_key=a3d52b90a5fc9f0d72a39591b5ff2304&language=en-US

    @GET("movie/{movie_id}?api_key=a3d52b90a5fc9f0d72a39591b5ff2304&language=en-US")
    Call<MovieModel> getMovieData(@Path("movie_id") int id);

}


