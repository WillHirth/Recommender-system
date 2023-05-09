import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class Test{
    public static void main(String[] args){
        List<float[]> arrayList = new ArrayList<>();
		//Read the csv
        try {
			BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\whirt\\Documents\\School\\3rd year\\Social Computing Techniques\\comp3208_example_package\\comp3208_100k_train_withratings.csv"));
			String line = br.readLine();
			while (line != null) {
				String[] array = line.split(",");
				
				//converts the string array into a float array
				float[] floatArray = new float[4];
				for (int i = 0; i < array.length; i++)
					floatArray[i] = Float.parseFloat(array[i]);				
				
				arrayList.add(floatArray);
				line = br.readLine();
			}
			br.close();
		} catch(Exception e) {
			e.printStackTrace();         
		}

		//Find the largest value in the second column of the array (This would be the itemid)
        float largest = 0;
        for (float[] fs : arrayList)
            if(fs[1] > largest)
                largest = fs[1];

		//Print the largest value
        System.out.println(largest);
    }
}