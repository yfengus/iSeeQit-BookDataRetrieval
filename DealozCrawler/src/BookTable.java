import java.util.ArrayList;
import java.util.TreeSet;


public class BookTable {
	String bookName;
	String bookAuthor;
	String bookPub;
	String bookEd;
	String bookReqOrOpt;
	
	ArrayList<BookType> bookTypeGroup;
//	String hcISBN;   //ISBN-13
//	public TreeSet<PriceRow> hcTable;
//	String pcISBN;   //ISBN-13
//	public TreeSet<PriceRow> pcTable;
	boolean isCustom;
//	boolean sameEdition;
//	boolean furTypeCheckNeeded;

	
	BookTable (String _bookName, String _bookAuthor, String _bookPub, String _bookEd, String _bookReqOrOpt, boolean _isCustom){
		
	//String _bookName, String _bookAuthor, String _bookPub, String _bookEd, String _bookReqOrOpt, String _hcISBN, 
	//	String _pcISBN, boolean _isCustom, boolean _sameEdition, boolean _furTypeCheckNeeded
		
		bookName = _bookName;
		bookAuthor = _bookAuthor;
		bookPub = _bookPub;
		bookEd = _bookEd;
		bookReqOrOpt = _bookReqOrOpt;
		
		isCustom = _isCustom;
		if (!_isCustom){
			bookTypeGroup = new ArrayList<BookType>();
		} else {
			bookTypeGroup = null;
		}
	}
	
	public String toString(){
		String ret = "";
		ret = ret + "Book Name: " + bookName + "\n";
		ret = ret + "Book Author: " + bookAuthor + "\n";
		ret = ret + "Book Publisher: " + bookPub + "\n";
		ret = ret + "Book Edition: " + bookEd + "\n";
		ret = ret + "Required? " + (bookReqOrOpt.equals("R")) + "\n";
		ret = ret + "Custom Book? " + isCustom + "\n";
		
		if (bookTypeGroup != null) {
			for (BookType curType : bookTypeGroup) {
				ret = ret + curType.toString();
			}
		}
		ret = ret + "\n";
		
		return ret;
	}
}
