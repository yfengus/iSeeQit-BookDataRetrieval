import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;


public class BookCount {
	
	public static int count() {
		int cnt = 0;
		BufferedReader br = null;
		
		try {
			br = new BufferedReader(new FileReader("bkT_1_195_output.csv"));
			Scanner sc = new Scanner(br);
			String curLine = "";
			String[] curElems = null;
			while (sc.hasNextLine()){
				curLine = sc.nextLine();
				curElems = curLine.split(",");
				if ((curElems.length>0)&&(curElems[0].length()==13)) {
					cnt++;
				}
			}
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		}
		return cnt;
				
	}
}
