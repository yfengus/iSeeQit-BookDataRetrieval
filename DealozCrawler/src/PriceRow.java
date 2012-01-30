
public class PriceRow implements Comparable<PriceRow>, Cloneable{
	String condition;
	Double totalPrice;
	String buyMethod;
	String shipTime;
	String store;
	String buyLink;
	
	PriceRow (String _condition, String _totalPrice, String _buyMethod, String _shipTime, String _store, String _buyLink){
		condition = _condition;
		totalPrice = Double.parseDouble(_totalPrice.substring(1));
		buyMethod = _buyMethod;
		shipTime = _shipTime;
		store = _store;
		buyLink = _buyLink;
	}
	
	public String toString(){
		return condition + "," + totalPrice + "," + buyMethod + "," + shipTime + "," + store + "," + buyLink;
	}

	public int compareTo(PriceRow other) {
		if (this.totalPrice.compareTo(other.totalPrice)!=0) return this.totalPrice.compareTo(other.totalPrice);
		else if (this.condition.compareTo(other.condition)!=0) return this.condition.compareTo(other.condition);
		else if (this.shipTime.compareTo(other.shipTime)!=0) return this.shipTime.compareTo(other.shipTime);
		else return 0;
	}
	
	

}
