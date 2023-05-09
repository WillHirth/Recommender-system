import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseTest {

    // Declare member variables for the connection
    final String connection_string = "jdbc:sqlite:/_PATH_TO_\\comp3208task2.db";
    public Connection c;

    public static void main(String[] args) {
        // Create database object and run methods
        DatabaseTest db = new DatabaseTest();
        // db.addTrainingData("_PATH_TO_\\comp3208_example_package\\comp3208_100k_train_withratings.csv");
        // db.addTestingData("_PATH_TO_\\comp3208_100k_test_withoutratings.csv");
        // db.computeSimilarityMatrix();
        db.predictRatings();
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
            Statement stmt = c.createStatement();
            stmt.executeUpdate("DROP TABLE trainingset");
            stmt.executeUpdate("CREATE TABLE trainingset (UserID INT, ItemID INT, Rating INT)");
            c.commit();
            stmt.close();

            System.out.println("Inserting values");
            PreparedStatement stat = c.prepareStatement("INSERT INTO trainingset VALUES (?,?,?)");

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
            Statement stmt = c.createStatement();
            stmt.executeUpdate("DROP TABLE testingset");
            stmt.executeUpdate("CREATE TABLE testingset (UserID INT, ItemID INT, time INT)");
            c.commit();
            stmt.close();

            System.out.println("Inserting values");
            PreparedStatement stat = c.prepareStatement("INSERT INTO testingset VALUES (?,?,?)");

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

    // Compute the similaritymatrix using the trainingdata
    public void computeSimilarityMatrix() {
        try {
            System.out.println("Creating similarity matrix table");
            Statement stmt = c.createStatement();
            stmt.executeUpdate("DROP TABLE similaritymatrix");
            stmt.executeUpdate("CREATE TABLE similaritymatrix (user1 INT, user2 INT, similarity INT)");
            c.commit();

            // Precomputes the user averages so they are not repeatedly calculated when
            // computing the similarity between items
            System.out.println("Calculating averages");
            int userNum = stmt.executeQuery("SELECT max(userid) AS largestItem FROM trainingset").getInt(1);
            List<Float> userAverages = getUserAverages(userNum);
            System.out.println("Averages calculated");

            System.out.println("Computing similarities");

            /*
             * PreparedStatement selectUsers = c.
             * prepareStatement("SELECT userid FROM trainingset WHERE itemid=? AND userid IN (SELECT userid FROM trainingset WHERE itemid=?)"
             * );
             * PreparedStatement selectRating = c.
             * prepareStatement("SELECT rating FROM trainingset WHERE userid=? AND itemid=?"
             * );
             * PreparedStatement insertStatement =
             * c.prepareStatement("INSERT INTO similaritymatrix VALUES(?, ?, ?)");
             */

            // Loops over every pair of users i and j
            for (int i = 1; i < userNum + 1; i++) {
                // j only goes up to i to prevent repeat similarities (i.e. comparing 1 to 5
                // then 5 to 1)
                for (int j = 1; j < i; j++) {
                    // Generate a list of every item rated by i and j
                    ArrayList<Integer> items = new ArrayList<Integer>();
                    ResultSet r = stmt.executeQuery("SELECT itemid FROM trainingset WHERE userid= " + i
                            + " AND itemid IN (SELECT itemid FROM trainingset WHERE userid= " + j + ")");
                    while (r.next()) {
                        items.add(r.getInt(1));
                    }

                    // Computes the similarity between the 2 users
                    // sum1 is the sum
                    float sum1 = 0;
                    float sum2 = 0;
                    float sum3 = 0;
                    for (Integer item : items) {
                        float ratingByi = stmt
                                .executeQuery(
                                        "SELECT rating FROM trainingset WHERE itemid = " + item + " AND userid = " + i)
                                .getInt(1);
                        float ratingByj = stmt
                                .executeQuery(
                                        "SELECT rating FROM trainingset WHERE itemid = " + item + " AND userid = " + j)
                                .getInt(1);
                        sum1 += (ratingByi - userAverages.get(i - 1)) * (ratingByj - userAverages.get(j - 1));
                        sum2 += (ratingByi - userAverages.get(i - 1)) * (ratingByi - userAverages.get(i - 1));
                        sum3 += (ratingByj - userAverages.get(j - 1)) * (ratingByj - userAverages.get(j - 1));
                    }

                    float similarity = (float) (sum1 / (Math.sqrt(sum2) * Math.sqrt(sum3)));
                    if (!Float.isNaN(similarity))
                        stmt.executeUpdate(
                                "INSERT INTO similaritymatrix VALUES(" + j + ", " + i + ", " + similarity + ")");
                }
                c.commit();
            }
            System.out.println("Finished calculating similarity matrix");
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Calculates the user averages in the trainingset when given the number of
    // users
    public ArrayList<Float> getUserAverages(int userNum) {
        ArrayList<Float> returnList = new ArrayList<>();
        try {
            PreparedStatement stat = c.prepareStatement("SELECT avg(rating) FROM trainingset where userid = ?");
            for (int i = 1; i < userNum + 1; i++) {
                stat.setInt(1, i);
                ResultSet r = stat.executeQuery();
                if (r.next())
                    returnList.add(r.getFloat(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return returnList;
    }

    // Predicts the ratings from testingset and adds them to a table called
    // predictedvalues
    public void predictRatings() {
        System.out.println("Predicting ratings");
        try {
            Statement stmt = c.createStatement();
            stmt.executeUpdate("DROP TABLE predictedvalues");
            stmt.executeUpdate("CREATE TABLE predictedvalues (userid INT, itemid INT, predictedrating INT, time INT)");
            c.commit();

            // Reads the testingset into memory so it can be looped over
            System.out.println("Reading values from testingset");
            ArrayList<Triple> testingSet = new ArrayList<Triple>();
            ResultSet r = stmt.executeQuery("SELECT * FROM testingset");
            while (r.next())
                testingSet.add(new Triple(r.getInt(1), r.getInt(2), r.getInt(3)));

            System.out.println("Calculating averages");
            int userNum = stmt.executeQuery("SELECT max(userid) AS largestItem FROM trainingset").getInt(1);
            List<Float> userAverages = getUserAverages(userNum);
            System.out.println("Averages calculated");

            PreparedStatement stat = c.prepareStatement(
                    "SELECT similarity FROM similaritymatrix WHERE (user1 = ? OR user1 = ?) AND (user2 = ? OR user2 = ?)");
            // For every row in the testing set:
            for (Triple t : testingSet) {
                // Selects the neighbours (every user with a similarity > 0) of the user whose
                // rating will be predicted
                ArrayList<Float> neighbours = new ArrayList<Float>();
                ResultSet r1 = stmt.executeQuery(
                        "SELECT userid FROM trainingset WHERE itemid = " + t.getItemid() + " AND userid IN "
                                + "(SELECT user2 FROM similaritymatrix WHERE similarity > 0 AND user1 = "
                                + t.getUserid() + ")");
                while (r1.next())
                    neighbours.add(r1.getFloat(1));

                ResultSet r2 = stmt.executeQuery(
                        "SELECT userid FROM trainingset WHERE itemid = " + t.getItemid() + " AND userid IN "
                                + "(SELECT user1 FROM similaritymatrix WHERE similarity > 0 AND user2 = "
                                + t.getUserid() + ")");
                while (r2.next())
                    neighbours.add(r1.getFloat(1));

                float sum1 = 0;
                float sum2 = 0;
                for (float f : neighbours) {
                    stat.setFloat(1, f);
                    stat.setFloat(2, t.getUserid());
                    stat.setFloat(3, f);
                    stat.setFloat(4, t.getUserid());
                    float similarity = stat.executeQuery().getFloat(1);
                    ResultSet rs = stmt.executeQuery(
                            "SELECT rating FROM trainingset WHERE itemid = " + t.getItemid() + " AND userid = " + f);
                    if (rs.next()) {
                        float userRating = rs.getFloat(1);
                        sum1 += similarity * (userRating - userAverages.get((int) f - 1));
                        sum2 += similarity;
                    }
                }

                // Calculates the prediction and ensures that it is between 0.5 and 5
                float prediction = userAverages.get((int) (t.getUserid() - 1)) + sum1 / sum2;
                if (prediction > 5)
                    prediction = 5;
                if (prediction < 0.5)
                    prediction = 0.5f;

                if (!Float.isNaN(prediction))
                    stmt.executeUpdate("INSERT INTO predictedvalues VALUES (" + t.getUserid() + ", " + t.getItemid()
                            + ", " + prediction + ", " + t.getTime() + ")");
                else
                    stmt.executeUpdate("INSERT INTO predictedvalues VALUES (" + t.getUserid() + ", " + t.getItemid()
                            + ", " + userAverages.get((int) t.getUserid() - 1) + ", " + t.getTime() + ")");

                c.commit();
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Finished predicting ratings");
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