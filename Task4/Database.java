import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;

public class Database {
    // Declare member variables for the connection to the database
    private final String connection_string = "jdbc:sqlite:_PATH_TO_\\comp3208task2.db";
    private Connection c;

    // Declare the arrays which will store the biases and vectors representing the
    // items and users respectively
    private ArrayList<double[]> itemVectors = new ArrayList<double[]>();
    private ArrayList<double[]> userVectors = new ArrayList<double[]>();
    private double[] userbias;
    private double[] itembias;
    private double globalbias;
    private int itemNum;
    private int userNum;

    // Database constructor
    public Database() throws SQLException {
        // Establish database connection
        c = DriverManager.getConnection(connection_string);
        c.setAutoCommit(false);
    }

    // Main method to create database object and run the method to train and predict
    // values
    public static void main(String[] args) {
        // Training data was read using .import
        // "_PATH_TO_\\comp3208_20m_train_withratings.csv" trainingset --csv
        // Testing data was read using .import
        // "_PATH_TO_\\comp3208_20m_test_withoutratings.csv" testingset --csv
        System.out.println("Starting");
        try {
            Database db = new Database();
            // Arguments refer to: Factors, Iterations, Learning Rate, Regularization Term,
            // and if it is testing
            db.run(200, 300, 0.0014f, 0.001f, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Finished");

        // Results were exported using .mode csv, .output results.csv, SELECT * FROM
        // predictedvalues;
    }

    /*
     * Uses the trainingdata to create the vectors representing the users and items
     * Then predicts the rating for the testing data and writes it into a database
     * called predictedvalues or prints the MAE
     * Arguments:
     * Factors - The number of factors in the user and item vectors
     * Iterations - The number of iterations where the vectors are updated
     * Learning rate - Multiplied to the values which are added to the vectors
     * (increasing this means that the values change by larger amounts each
     * iteration)
     * Regularization - The regularization term which is multiplied by the the
     * original vector and subtracted to prevent overfitting
     * Testing - Boolean which determines if the model will predict the values for
     * the testingset or work out the MAE for the model
     */
    private void run(int factors, int iterations, float learningRate, float regularization, boolean testing)
            throws SQLException {
        Statement stmt = c.createStatement();
        itemNum = stmt.executeQuery("SELECT MAX(itemid) FROM trainingset").getInt(1);
        userNum = stmt.executeQuery("SELECT MAX(userid) FROM trainingset").getInt(1);
        globalbias = stmt.executeQuery("SELECT AVG(rating) FROM trainingset").getDouble(1);
        userbias = new double[userNum];
        itembias = new double[itemNum];

        train(factors, iterations, learningRate, regularization, stmt, false);
        predict(stmt);

        stmt.close();
    }

    // Method to train the model (which is 2 matricies which represent the users and
    // items)
    private void train(int factors, int iterations, float learningRate, float regularization, Statement stmt,
            boolean testing) throws SQLException {
        initializedVectors(factors);

        // As some item ids are not present they will always have their initial random
        // values
        // This loop sets those biases to 0 and item vectors to a vector containing only
        // 0s
        PreparedStatement stat = c.prepareStatement("SELECT * FROM trainingset WHERE itemid = ?");
        for (int i = 0; i < itemNum; i++) {
            stat.setInt(1, i + 1);
            if (stat.executeQuery().next() == false) {
                itemVectors.set(i, Vector.vectorOfZeros(factors));
                itembias[i] = 0;
            }
        }
        stat.close();

        System.out.println("Training");
        // For the given number of iterations order the trainingset randomly and predict
        // the value then update the values in the vectors using the error
        for (int i = 0; i < iterations; i++) {
            ResultSet r;
            if (testing)
                r = stmt.executeQuery("SELECT * FROM trainingset WHERE rowid % 20 != 0 ORDER BY RANDOM()");
            else
                r = stmt.executeQuery("SELECT * FROM trainingset ORDER BY RANDOM()");

            while (r.next()) {
                // Find the userid and itemid pair and their respective vectors in the matricies
                int userid = r.getInt(1) - 1;
                int itemid = r.getInt(2) - 1;
                double[] userVector = userVectors.get(userid);
                double[] itemVector = itemVectors.get(itemid);
                // Find the error in the predicted value by subtracting the prediction from the
                // actual value
                // The prediction is the dot product of the item vector and the transposed user
                // vector + the global average + the item and user biases
                double error = r.getInt(3) - Vector.dot(itemVector, userVector) - globalbias - userbias[userid]
                        - itembias[itemid];
                // Update user and item biases
                userbias[userid] += learningRate * (error - regularization * userbias[userid]);
                itembias[itemid] += learningRate * (error - regularization * itembias[itemid]);
                // Update the user and item vectors by adding γ * (error * q(i,*) - λ * p(u,*))
                // and γ * (error * p(u,*) - λ * q(i,*)) to the respective vectors
                // Where γ is the learning rate, λ is the regularization term, q(i,*) is the
                // item vector and p(u,*) is the user vector
                userVectors.set(userid,
                        Vector.add(Vector.multiply(learningRate, (Vector.subtract(Vector.multiply(error, itemVector),
                                Vector.multiply(regularization, userVector)))), userVector));
                itemVectors.set(itemid,
                        Vector.add(Vector.multiply(learningRate, (Vector.subtract(Vector.multiply(error, userVector),
                                Vector.multiply(regularization, itemVector)))), itemVector));
            }
        }
    }

    // Initializes the user and item vectors as random vectors of the size specified
    // by factors and the biases to random values
    private void initializedVectors(int factors) {
        Random random = new Random();
        // Loop over item and randomly initialize their vectors and biases
        for (int i = 0; i < itemNum; i++) {
            itemVectors.add(Vector.randomVector(random, factors, 0.0, 1.0 / factors));
            itembias[i] = 0.0 + random.nextDouble() * (1.0 / factors - 0.0);
        }

        // Loop over user and randomly initialize their vectors and biases
        for (int i = 0; i < userNum; i++) {
            userVectors.add(Vector.randomVector(random, factors, 0.0, 1.0 / factors));
            userbias[i] = 0.0 + random.nextDouble() * (1.0 / factors - 0.0);
        }

    }

    // Method to predict used in the predict and predictionMAE
    private double predict(int userid, int itemid) {
        return Vector.dot(itemVectors.get(itemid), userVectors.get(userid)) + globalbias + userbias[userid]
                + itembias[itemid];
    }

    // Method which calculates the predicted value for 20% of the data then prints
    // the MAE which is used for testing
    private float predictionMAE(int i, Statement stmt) throws SQLException {
        ResultSet r = stmt.executeQuery("SELECT * FROM trainingset WHERE rowid % 20 = 0");
        int n = 0;
        float sum = 0;
        // Total up the absolute error of the prediction then divide it by the number of
        // predictions to calculate the MAE
        while (r.next()) {
            double prediction = predict((int) r.getFloat(1) - 1, (int) r.getFloat(2) - 1);
            if (prediction > 5)
                prediction = 5;
            if (prediction < 0.5)
                prediction = 0.5f;
            sum += (Math.abs(r.getFloat(3) - prediction));
            n++;
        }
        System.out.println("Mae for " + i + " iterations: " + sum / n);
        return sum / n;
    }

    // Method to calculate the prediction for each row in the testingset and write
    // to a table called predictedvalues
    private void predict(Statement stmt) throws SQLException {
        // Drop the table containing the previous predicted values and create a new one
        stmt.executeUpdate("DROP TABLE predictedvalues");
        stmt.executeUpdate("CREATE TABLE predictedvalues (UserId INT, ItemId INT, PredictedRating INT, Time INT)");
        c.commit();

        // Add all the values from the testing set to an arraylist
        ArrayList<Triple> testingSet = new ArrayList<Triple>();
        ResultSet r = stmt.executeQuery("SELECT * FROM testingset");
        while (r.next())
            testingSet.add(new Triple(r.getInt(1), r.getInt(2), r.getInt(3)));

        System.out.println("Predicting values");
        // Loop over each line in the testingset and calculate the predicted value by
        // doing a vector dot product of the transposed user vector and the item vector
        for (Triple t : testingSet) {
            double prediction = predict(t.getUserid() - 1, t.getItemid() - 1);

            // Perform some validation checks on the predicted value to ensure it is within
            // the range and a valid number
            if (prediction > 5)
                prediction = 5;
            if (prediction < 0.5)
                prediction = 0.5f;

            if (!Double.isNaN(prediction) && !Double.isInfinite(prediction))
                // Add the predicted value into the table along with the original userid,
                // itemid, and time
                stmt.executeUpdate("INSERT INTO predictedvalues VALUES (" + t.getUserid() + ", " + t.getItemid() + ", "
                        + prediction + ", " + t.getTime() + ")");
            else
                System.out.println("Error with User: " + t.getUserid() + ", Item: " + t.getItemid());
        }
        c.commit();
    }

    // The class used to represent the testingset when read from the table in
    // predict values
    private class Triple {
        private int userid;
        private int itemid;
        // double must be used instead of float for time to maintain the precision of
        // the value
        private double time;

        public Triple(int userid, int itemid, double time) {
            this.userid = userid;
            this.itemid = itemid;
            this.time = time;
        }

        public int getUserid() {
            return this.userid;
        }

        public int getItemid() {
            return this.itemid;
        }

        public double getTime() {
            return this.time;
        }
    }
}