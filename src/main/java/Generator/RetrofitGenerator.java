package Generator;

import Util.DBConnector;
import Util.MyAPICall;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.sql.SQLException;

public abstract class RetrofitGenerator {


    abstract public void generate(int countOfLines, int isolationLevel);

    abstract public void closeConnections() throws SQLException;


    /**
     * Вызывается для проверки аргумента {@link RetrofitGenerator#generate(int countOfRows, int isolationLevel)}
     * на превышение максимального порога для конкретной реализации.
     */
    protected int checkNumberOfLines(int inputNumberOfLines, int maxNumberOfLines) {
        return Math.min(maxNumberOfLines, inputNumberOfLines);

    }

    protected MyAPICall getAPI() {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        final String BASE_URL = "https://api.themoviedb.org/3/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(MyAPICall.class);
    }


}
