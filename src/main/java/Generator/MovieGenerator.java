package Generator;

import Model.MovieModel;
import Util.DBConnector;
import Util.MyAPICall;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.sql.*;

public class MovieGenerator extends RetrofitGenerator {

    private PreparedStatement queryInsertIntoFilm;
    private PreparedStatement checkQuery;
    private int insertedMovieCounter;
    private Connection connection;

    private static final String GET_COUNT_OF_ROWS_QUERY = "SELECT count(id) FROM film";
    /* max number of movies in themoviedb is 800.000
    опционально значение MAX_NUMBER_OF_MOVIE может быть измененно */
    public static final int MAX_NUMBER_OF_MOVIE = 1000;

    @Override
    public void generate(int countOfRows, int isolationLevel) {
        connection = DBConnector.getDBConnection(isolationLevel);

        final MyAPICall myAPICall = getAPI();
        Response<MovieModel> response;
        Call<MovieModel> call;

        String filmName;
        String filmDescription;
        int filmBudget;
        int filmYear;
        int filmRuntime;

        int numberOfLines = checkNumberOfLines(countOfRows, MAX_NUMBER_OF_MOVIE);
        int i = 0;

        try {
            while (insertedMovieCounter < numberOfLines) {
                call = myAPICall.getMovieData(i);
                response = call.execute();
                if (response.code() == 200) {

                    filmName = response.body().getTitle();
                    filmDescription = response.body().getDescription();
                    filmBudget = response.body().getBudget();
                    filmYear = response.body().getReleaseDate();
                    filmRuntime = response.body().getRuntime();

                    insertIntoFilmTable(filmName, filmDescription, filmBudget, filmYear, filmRuntime);
                }
                i++;
            }
            closeConnections();
        } catch (IOException | SQLException exception) {
            exception.printStackTrace();
        }

    }

    @Override
    public void closeConnections() throws SQLException {
        connection.close();
        queryInsertIntoFilm.close();
    }


    private void insertIntoFilmTable(String name, String description, int budget, int year, int runtime) {
        try {

            queryInsertIntoFilm = connection.prepareStatement(
                    "INSERT INTO public.film(name, description, budget, year, runtime)" +
                            " VALUES(?, ?, ?, ?, ?)");

            queryInsertIntoFilm.setString(1, name);
            queryInsertIntoFilm.setString(2, description);
            queryInsertIntoFilm.setInt(3, budget);
            queryInsertIntoFilm.setInt(4, year);
            queryInsertIntoFilm.setInt(5, runtime);

            queryInsertIntoFilm.executeUpdate();

            insertedMovieCounter++;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
