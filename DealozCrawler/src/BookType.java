import java.util.TreeSet;


public class BookType implements Comparable<BookType>, Cloneable{
	String bookType;
	String bookISBN;
	TreeSet<PriceRow> priceTable;
	
	BookType (String _bookType, String _bookISBN) {
		bookType = _bookType;
		bookISBN = _bookISBN;
		priceTable = new TreeSet<PriceRow>();
	}

	@Override
	public int compareTo(BookType other) {
		return (bookType.compareTo(other.bookType));
	}
	
	public String toString(){
		String ret = "";
		ret = ret + bookType + ": ISBN: " + bookISBN + "\n";
		if (priceTable.size()!=0) {
			ret = ret + "Condition," + "Total Price," + "Buy Method," + "Shipping Time," + "Store," + "Link" + "\n";	
			for (PriceRow curRow : priceTable) {
				ret = ret + curRow.toString() + "\n";
			}
		}
		return ret;
	}
	
}
	

