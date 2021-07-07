import Util.DBConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public class TransactionThread extends Thread {

    private final Connection connection;
    private final int numberOfIterations;
    private final DBConnector.Query query;
    private final CountDownLatch startLatch;
    private final CountDownLatch finishLatch;
    private ArrayList<Long> listOfTotalTimeMetrics = new ArrayList<>();
    private ArrayList<Long> listOfAverageTimeMetrics = new ArrayList<>();


    public TransactionThread(Connection connection,
                             int numberOfIterations,
                             DBConnector.Query query,
                             CountDownLatch startLatch,
                             CountDownLatch finishLatch) {
        this.connection = connection;
        this.numberOfIterations = numberOfIterations;
        this.query = query;
        this.startLatch = startLatch;
        this.finishLatch = finishLatch;
    }

    @Override
    public void run() {
        PreparedStatement currentStatement = null;
        try {
            switch (query) {
                case INSERT:
                    currentStatement = connection.prepareStatement(
                            "INSERT INTO public.film(name, description, budget, year, runtime)" +
                                    "values(?, 'descr', 90210, ?, 123)");
                    currentStatement.setString(1, "film_name");
                    currentStatement.setInt(2, randomYear());
                    currentStatement.execute();
                    break;
                case SELECT:
                    currentStatement = connection.prepareStatement(
                            "SELECT * from FILM where budget < 1000000");
                    currentStatement.execute();
                    break;
                case UPDATE:
                    currentStatement = connection.prepareStatement("update film " +
                            "set runtime = 222" +
                            "where budget < 1000000");
                    currentStatement.execute();
                    break;
            }
            executeSelectedQuery(currentStatement, query);
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }

    private void executeSelectedQuery(PreparedStatement ps, DBConnector.Query query) {
        boolean isTransactionSuccessful = false;
        long startTime = 0;
        long endTime = 0;
        long sumTime = 0;
        startLatch.countDown();
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        for (int i = 0; i < numberOfIterations; i++) {
            startTime = System.nanoTime();
            do {
                try {
                    switch (query) {
                        case INSERT:
                            ps.setString(1, "film_name" + i);
                            ps.setInt(2, randomYear());
                            ps.executeUpdate();
                            break;
                        case SELECT:
                            ps.executeQuery();
                            break;
                        case UPDATE:
                            ps.executeUpdate();
                            break;
                    }
                    isTransactionSuccessful = true;
                } catch (SQLException sqlException) {
                    if (sqlException.getSQLState().equals("40001")) {
                        isTransactionSuccessful = false;
                    } else sqlException.printStackTrace();
                }
            } while (!isTransactionSuccessful);
            endTime = System.nanoTime() - startTime;
            sumTime += endTime;
            listOfAverageTimeMetrics.add(endTime);
            listOfTotalTimeMetrics.add(sumTime);
        }

        finishLatch.countDown();
        try {
            finishLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            ps.close();
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }



    }

    private int randomYear() {
        return ThreadLocalRandom.current().nextInt(1890, 2022);
    }

    public ArrayList<Long> getListOfAverageTimeMetrics() {
        return listOfAverageTimeMetrics;
    }

    public ArrayList<Long> getListOfTotalTimeMetrics() {
        return listOfTotalTimeMetrics;
    }
}


