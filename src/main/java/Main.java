import Generator.MovieGenerator;
import Util.DBConnector;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;

public class Main {

    private static TransactionThread insertThread;
    private static TransactionThread selectThread;
    private static TransactionThread updateThread;
    private static JSONObject jsonTotal = new JSONObject();
    private static JSONObject jsonAverage = new JSONObject();
    private static FileWriter fileWriter;


    public static void main(String[] args) {

        for (DBConnector.IsolationLevel level : DBConnector.IsolationLevel.values()) {
            executeRequest(level.getNumericalValue());
            createJsonFile(level);
        }

        System.out.println("The end");


    }

    private static void executeRequest(int isolationLevel) {
        Connection con1 = DBConnector.getDBConnection(isolationLevel);
        Connection con2 = DBConnector.getDBConnection(isolationLevel);
        Connection con3 = DBConnector.getDBConnection(isolationLevel);
        int numberOfIterations = 1000;
        CountDownLatch startLatch = new CountDownLatch(3);
        CountDownLatch finishLatch = new CountDownLatch(4);

        try {
            PreparedStatement preparedStatement = con1.prepareStatement("delete\n" +
                    "from film\n" +
                    "where film.id > 0");

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }

        MovieGenerator movieGenerator = new MovieGenerator();
        movieGenerator.generate(400, isolationLevel);


        insertThread = new TransactionThread(con1, numberOfIterations, DBConnector.Query.INSERT, startLatch, finishLatch);
        selectThread = new TransactionThread(con2, numberOfIterations, DBConnector.Query.SELECT, startLatch, finishLatch);
        updateThread = new TransactionThread(con3, numberOfIterations, DBConnector.Query.UPDATE, startLatch, finishLatch);

        insertThread.start();
        selectThread.start();
        updateThread.start();

        finishLatch.countDown();
        try {
            finishLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            con1.close();
            con2.close();
            con3.close();
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }

    }

    private static void createJsonFile(DBConnector.IsolationLevel isolationLevel) {
        JSONObject totalMetricsObject = new JSONObject();
        totalMetricsObject.put("INSERT", insertThread.getListOfTotalTimeMetrics());
        totalMetricsObject.put("SELECT", selectThread.getListOfTotalTimeMetrics());
        totalMetricsObject.put("UPDATE", updateThread.getListOfTotalTimeMetrics());
        jsonTotal.put(isolationLevel.toString(), totalMetricsObject);

        JSONObject averageMetricsObject = new JSONObject();
        averageMetricsObject.put("INSERT", insertThread.getListOfAverageTimeMetrics());
        averageMetricsObject.put("SELECT", selectThread.getListOfAverageTimeMetrics());
        averageMetricsObject.put("UPDATE", updateThread.getListOfAverageTimeMetrics());
        jsonAverage.put(isolationLevel.toString(), averageMetricsObject);



        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("src\\main\\data\\metrics_total.json");
            fileWriter.write(jsonTotal.toString());
            fileWriter.flush();
            fileWriter = new FileWriter("src\\main\\data\\metrics_average.json");
            fileWriter.write(jsonAverage.toString());
            fileWriter.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

}
