import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.lang.Math;
import java.io.PrintWriter;

public class Task1 {
    public static void main(String[] args) {
        List<float[]> predLines = new ArrayList<>();
        List<float[]> goldLines = new ArrayList<>();

        // Reads the .csv files into an arraylist which is stored in memory
        readCsv("PATH_TO_\\comp3208_micro_pred.csv", predLines);
        readCsv("PATH_TO_\\comp3208_micro_gold.csv", goldLines);

        // Initialise variables which will be used in the calculations
        int n = predLines.size();
        float sum1 = 0;
        float sum2 = 0;

        for (int i = 0; i < n; i++) {
            // sum1 stores the square of the difference between predicted and gold values
            // which is used for MSE and RMSE
            sum1 += (goldLines.get(i)[2] - predLines.get(i)[2]) * (goldLines.get(i)[2] - predLines.get(i)[2]);
            // sum2 stores the absolute difference between the predicted and vold values
            // which is used for MAE
            sum2 += (Math.abs(goldLines.get(i)[2] - predLines.get(i)[2]));
        }

        // The values of the MSE, RMSE, and MAE are saved to a file called results.csv
        try {
            // If results.csv does not exist it is created
            PrintWriter pw = new PrintWriter("PATH_TO_\\results.csv");
            pw.println(sum1 / n + ", " + Math.sqrt(sum1 / n) + ", " + sum2 / n);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
         * Checks that all values other than the predicted value are the same given that
         * the line number is the same
         * for(int i = 0; i < predLines.size(); i++){
         * for (int j = 0; j < predLines.get(i).length; j++){
         * if(!predLines.get(i)[j].equals(goldLines.get(i)[j]) && j != 2){
         * System.out.println("Difference on line: " + i + " column: " + j);
         * }
         * }
         * }
         */
    }

    /*
     * Input: file path of the .csv file which will be read, arraylist which it will
     * be read into
     * Output: void
     * Purpose: reads the .csv file into an arraylist containing float arrays
     */
    private static void readCsv(String filePath, List<float[]> arrayList) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line = br.readLine();
            while (line != null) {
                String[] array = line.split(",");

                // converts the string array into a float array
                float[] floatArray = new float[4];
                for (int i = 0; i < array.length; i++) {
                    floatArray[i] = Float.parseFloat(array[i]);
                }

                arrayList.add(floatArray);
                line = br.readLine();
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}