package Model;

import com.google.gson.annotations.SerializedName;
import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

import java.util.Random;

public class MovieModel {

    private Lorem lorem = LoremIpsum.getInstance();

    @SerializedName("title")
    private String title;

    @SerializedName("budget")
    private String budget;

    @SerializedName("runtime")
    private int runtime;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("overview")
    private String description;

    @SerializedName("poster_path")
    private String posterUrl;

    public String getScreenshot() {
        return "https://image.tmdb.org/t/p/w500" + posterUrl;
    }

    public String getTitle() {
        return title;
    }

    public int getBudget() {
        int intBudget = Integer.parseInt(budget);
        if(intBudget == 0) {
            Random random = new Random();
            return random.nextInt(Integer.MAX_VALUE/2 - 1) + 1;
        }
        return intBudget;
    }

    public int getRuntime() {
        return runtime;
    }

    public int getReleaseDate() {
        return Integer.parseInt(releaseDate.split("-")[0]);
    }

    public String getDescription() {
        if(description == null || description.isEmpty()) {
            return lorem.getWords(15, 30);
        }
        return description;
    }
}
