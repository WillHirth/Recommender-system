import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DatabaseTest {

    // Declare member variables for the connection to the database
    final String connection_string = "_PATH_TO_\\comp3208task2.db";
    public Connection c;

    public static void main(String[] args) {
        // Create database object and run methods to read the training and test data
        // then predict values
        System.out.println("Starting");
        DatabaseTest db = new DatabaseTest();
        db.addTrainingData("_PATH_TO_\\comp3208_100k_train_withratings.csv");
        db.addTestingData("_PATH_TO_\\comp3208_100k_test_withoutratings.csv");
        db.predictValues(130, 310, 0.0006f, 5 / 10000.0f);
        System.out.println("Done");
    }

    // Database constructor
    public DatabaseTest() {
        try {
            // Establish database connection
            c = DriverManager.getConnection(connection_string);
            c.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Add the trainingdata from the specified csv to a table called trainingset
    public void addTrainingData(String fileName) {
        try {
            System.out.println("Creating training table");
            // Create the table in the database
            Statement stmt = c.createStatement();
            stmt.executeUpdate("DROP TABLE trainingset");
            stmt.executeUpdate("CREATE TABLE trainingset (UserID INT, ItemID INT, Rating INT)");
            c.commit();
            stmt.close();

            System.out.println("Inserting values");
            PreparedStatement stat = c.prepareStatement("INSERT INTO trainingset VALUES (?,?,?)");

            // Read the data from the file then insert it into the table using the prepared
            // statement
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line = br.readLine();
            while (line != null) {
                String[] array = line.split(",");
                stat.setDouble(1, Float.parseFloat(array[0]));
                stat.setDouble(2, Float.parseFloat(array[1]));
                stat.setDouble(3, Float.parseFloat(array[2]));
                stat.executeUpdate();
                c.commit();
                line = br.readLine();
            }
            br.close();
            System.out.println("Finished inserting values");
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Add the testingdata from the specified csv to a table called testingset
    public void addTestingData(String fileName) {
        try {
            System.out.println("Creating testing table");
            // Create the table in the database
            Statement stmt = c.createStatement();
            stmt.executeUpdate("DROP TABLE testingset");
            stmt.executeUpdate("CREATE TABLE testingset (UserID INT, ItemID INT, time INT)");
            c.commit();
            stmt.close();

            System.out.println("Inserting values");
            PreparedStatement stat = c.prepareStatement("INSERT INTO testingset VALUES (?,?,?)");

            // Read the data from the file then insert it into the table using the prepared
            // statement
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line = br.readLine();
            while (line != null) {
                String[] array = line.split(",");
                stat.setDouble(1, Float.parseFloat(array[0]));
                stat.setDouble(2, Float.parseFloat(array[1]));
                // Time must be read as a double to maintain the same level of precision as the
                // csv
                stat.setDouble(3, Double.parseDouble(array[2]));
                stat.executeUpdate();
                c.commit();
                line = br.readLine();
            }
            br.close();
            System.out.println("Finished inserting values");
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Uses the trainingdata to learn then predict the rating for the testing data
    // and write it into a database called predictedvalues
    public void predictValues(int factors, int iterations, float learningRate, float regularization) {
        // Declare the arrays which will store the vectors representing the items and
        // users respectively
        ArrayList<double[]> itemVectors = new ArrayList<double[]>();
        ArrayList<double[]> userVectors = new ArrayList<double[]>();

        try {
            Statement stmt = c.createStatement();
            int itemNum = stmt.executeQuery("SELECT max(itemid) FROM trainingset").getInt(1);
            int userNum = stmt.executeQuery("SELECT max(userid) FROM trainingset").getInt(1);

            // loop over item and randomly initialize their vectors
            for (int i = 0; i < itemNum; i++)
                itemVectors.add(Matrix.randomVector(factors, 0.0, 5.0 / factors));

            // loop over user and randomly initialize their vectors
            for (int i = 0; i < userNum; i++)
                userVectors.add(Matrix.randomVector(factors, 0.0, 5.0 / factors));

            // For the given number of iterations order the trainingset randomly and predict
            // the value
            for (int i = 0; i < iterations; i++) {
                ResultSet r = stmt.executeQuery("SELECT * FROM trainingset ORDER BY RANDOM()");
                while (r.next()) {
                    int userid = r.getInt(1) - 1;
                    int itemid = r.getInt(2) - 1;
                    double[] userVector = userVectors.get(userid);
                    double[] itemVector = itemVectors.get(itemid);
                    // Find the error in the predicted value by subtracting the dot product of the
                    // transposed item vector and the user vector from the actual value
                    double error = r.getInt(3) - Matrix.multiply(itemVector, userVector);
                    // Update the user and item vectors by adding γ * (error * q(i,*) - λ * p(u,*))
                    // and γ * (error * p(u,*) - λ * q(i,*)) to the respective vectors
                    // Where γ is the learning rate, λ is the regularization term, q(i,*) is the
                    // item vector and p(u,*) is the user vector
                    userVectors
                            .set(userid,
                                    Matrix.add(
                                            Matrix.multiply(learningRate,
                                                    (Matrix.subtract(Matrix.multiply(error, itemVector),
                                                            Matrix.multiply(regularization, userVector)))),
                                            userVector));
                    itemVectors
                            .set(itemid,
                                    Matrix.add(
                                            Matrix.multiply(learningRate,
                                                    (Matrix.subtract(Matrix.multiply(error, userVector),
                                                            Matrix.multiply(regularization, itemVector)))),
                                            itemVector));
                }
            }

            // As some item ids are not present they will still have their initial random
            // values
            // This loop sets those item vector to a vector containing only 0s
            for (int i = 0; i < itemNum; i++)
                if (stmt.executeQuery("SELECT COUNT(*) FROM trainingset WHERE itemid = " + (i + 1)).getInt(1) == 0)
                    itemVectors.set(i, Matrix.vectorOfZeros(factors));

            System.out.println("Predicting ratings");
            stmt.executeUpdate("DROP TABLE predictedvalues");
            stmt.executeUpdate("CREATE TABLE predictedvalues (userid INT, itemid INT, predictedrating INT, time INT)");
            c.commit();

            // Add all the values from the testing set to an arraylist
            ArrayList<Triple> testingSet = new ArrayList<Triple>();
            ResultSet r = stmt.executeQuery("SELECT * FROM testingset");

            while (r.next())
                testingSet.add(new Triple(r.getInt(1), r.getInt(2), r.getInt(3)));

            // Loop over each line in the testingset and calculate the predicted value by
            // doing a vector dot product of the transposed user vector and the item vector
            for (Triple t : testingSet) {
                double prediction = Matrix.multiply(userVectors.get((int) t.getUserid() - 1),
                        itemVectors.get((int) t.getItemid() - 1));

                // Perform some validation checks on the predicted value to ensure it is within
                // the range and a valid number
                if (prediction > 5)
                    prediction = 5;
                if (prediction < 0.5)
                    prediction = 0.5f;

                if (!Double.isNaN(prediction) && !Double.isInfinite(prediction))
                    stmt.executeUpdate("INSERT INTO predictedvalues VALUES (" + t.getUserid() + ", " + t.getItemid()
                            + ", " + prediction + ", " + t.getTime() + ")");
                else
                    System.out.println("Error with User: " + t.getUserid() + " Item: " + t.getItemid());

                c.commit();
            }

            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // The class used to represent the testingset when read from the table in
    // predict values
    public class Triple {
        private float userid;
        private float itemid;
        private double time;

        public Triple(float userid, float itemid, double time) {
            this.userid = userid;
            this.itemid = itemid;
            this.time = time;
        }

        public float getUserid() {
            return this.userid;
        }

        public float getItemid() {
            return this.itemid;
        }

        public double getTime() {
            return this.time;
        }
    }
}