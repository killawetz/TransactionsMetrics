package Util;

import java.sql.*;

public class DBConnector {

    public enum Query{
        SELECT,
        INSERT,
        UPDATE
    }

    public enum IsolationLevel {
        READ_COMMITTED(2),
        REPEATABLE_READ(4),
        SERIALIZE(8);

        private int numericalValue;
        IsolationLevel(int i) {
            this.numericalValue = i;
        }

        public int getNumericalValue() {
            return numericalValue;
        }
    }

    public static Connection getDBConnection(int isolationLevel) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres?characterEncoding=cp1251",
                    "postgres",
                    "qwerty");
            connection.setTransactionIsolation(isolationLevel);
        } catch (SQLException throwables) {
            System.out.println("Connection Failed");
            throwables.printStackTrace();
        }

        if (connection != null) {
            System.out.println("You successfully connected to database now");
        } else {
            System.out.println("Failed to make connection to database");
        }
        return connection;
    }
}


