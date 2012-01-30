import java.util.ArrayList;
import java.util.TreeSet;


public class CourseNode implements Comparable<CourseNode>{
	String dept;
	String courseNum;
	String term;
	String year;
	public ArrayList<BookTable> bookTable;
	
	
	CourseNode(String _dept, String _courseNum, String _term, String _year){
		dept = _dept;
		courseNum = _courseNum;
		term = _term;
		year = _year;
		bookTable = new ArrayList<BookTable>();
	}
	
	public String getCourseInfo(){
		return this.dept + " " + this.courseNum + " " + this.term + " " + this.year;
	}
	
//	public int hashCode(){
//		return 3*dept.hashCode() + 7*courseNum.hashCode() + 11*term.hashCode() + 17*year.hashCode();
//	}
	
	public String toString(){
		String ret = "";
		ret = ret + "Class Name: " + this.dept+" "+this.courseNum+" "+this.term + " " + this.year + "\n";
		for (BookTable curTable : bookTable){
			ret = ret + curTable.toString();
		}
		ret = ret + "\n";
		
		return ret;
	}
	
	public int compareTo(CourseNode other){
		if (this.dept.compareTo(other.dept)!=0) return this.dept.compareTo(other.dept);
		else if (this.courseNum.compareTo(other.courseNum)!=0) return this.courseNum.compareTo(other.courseNum);
		else if (this.term.compareTo(other.term)!=0) return this.term.compareTo(other.term);
		else if (this.year.compareTo(other.year)!=0) return this.year.compareTo(other.year);
		else return 0;
	}
	
	public boolean equals(CourseNode other){
		return  ((this.dept.equals(other.dept))&&(this.courseNum.equals(other.courseNum))&&(this.term.equals(other.term))&&(this.year.equals(other.year)));
	}
}
