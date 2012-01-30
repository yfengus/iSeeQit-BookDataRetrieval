import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.TreeSet;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;




public class Runner {
	
	static String sourceFileName = "bkT_1.txt";
	static String outputFileName = "bkT_1_ISBNlist.txt";
	public static TreeSet<CourseNode> doneClassList = new TreeSet<CourseNode>();
	WebDriver dr = null;         //TODO
	static int cnt;
	long start;
	
	Runner(){
		doneClassList = readFromClassListFile();
		cnt = 0;
		start = 0;
	}
	
	public void execute(){
		long start = System.nanoTime();    
		FileReader fr = null;
		BufferedReader br = null;
		PrintWriter writer = null;
		try {
			br = new BufferedReader(new FileReader(sourceFileName));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		Scanner sc = new Scanner(br);
		String curLine = "";
		String[] curElems = null;
		CourseNode curCourse = null;
		
		while (sc.hasNextLine()){
			//handle course identifier
			curLine = sc.nextLine();
//			if (curLine.equals("")) System.exit(0);
			if (!(curLine.contains("Course: "))) throw new NoSuchElementException("No \"Course: \"");
			curLine = curLine.substring(8);
			curElems = curLine.split(" ");
			curCourse = new CourseNode(curElems[0],curElems[1],curElems[2],curElems[3]);
			while (doneClassList.contains(curCourse)) {   //Duplicated class. Skip to the next one.
				System.err.println("Skipped: " + curCourse.getCourseInfo());
				do {
					curLine = sc.nextLine();
//					System.err.println(curLine.length());
				} while ((sc.hasNextLine())&&(!(curLine.length()==0)));
				if (!sc.hasNextLine()) System.exit(0);
				curLine = sc.nextLine();
//				System.out.println(curLine);
				curLine = curLine.substring(8);
				curElems = curLine.split(" ");
				curCourse = new CourseNode(curElems[0],curElems[1],curElems[2],curElems[3]);
			}
			
			
			//skip column titles
			sc.nextLine();
			
			//handle each textbook
			//BookTable (String _bookName, String _bookAuthor, String _bookPub, String _bookEd, 
			//String _bookReqOrOpt, String _hcISBN, String _pcISBN, boolean _isBundle)
			//BookTable (String _bookName, String _bookAuthor, String _bookPub, String _bookEd, String _bookReqOrOpt, boolean _isCustom)
			curLine = sc.nextLine();
			do {
				System.out.println(curLine);
				curElems = curLine.split(",");
				System.out.println(curElems[0]);
				if (curElems[0].charAt(0) != '9') {   //custom textbook
					System.out.println("Custom textbook");
					curCourse.bookTable.add(new BookTable(curElems[2], curElems[1], curElems[3], curElems[4], curElems[5], true));
				} else {
					ArrayList<BookType> bookTypeGroup = getBookTypeGroup(curElems[0], getVersionNumber(" "+curElems[4]+" "));
					if (bookTypeGroup == null) {
						System.out.println("Custom textbook");
						curCourse.bookTable.add(new BookTable(curElems[2], curElems[1], curElems[3], curElems[4], curElems[5], true)); //custom textbook
					}
					else {    //normal textbook. 
						curCourse.bookTable.add(new BookTable(curElems[2], curElems[1], curElems[3], curElems[4], curElems[5], false));
						curCourse.bookTable.get(curCourse.bookTable.size()-1).bookTypeGroup = bookTypeGroup;
//						Dealoz_Parser dealozParser = new Dealoz_Parser();
						//BookRow (String _condition, String _totalPrice, String _shipTime, String _store)
						for (BookType curType : curCourse.bookTable.get(curCourse.bookTable.size()-1).bookTypeGroup) {
							System.out.println(curType.bookType+": "+curType.bookISBN);
							String crawlRet = null;
//							crawlRet = dealozParser.crawl(curType.bookISBN);
							if (!(crawlRet == null)) {
								String[] storeChoices = crawlRet.split("\n");
								for (String curChoice : storeChoices) {
									curElems = curChoice.split(",");
									curType.priceTable.add(new PriceRow(curElems[0], curElems[7], curElems[2], curElems[5], curElems[8], curElems[3]));
								}
							}
						}
					}
				}
				if (sc.hasNextLine()) curLine = sc.nextLine();
				else curLine = "";
				cnt++;
				if (cnt==10) {
					System.err.println("Elapsed time: " + (System.nanoTime()-start));
					writeToTimeCountFile(System.nanoTime()-start);
					cnt = 0;
					start = System.nanoTime();
				}
			} while (curLine.length()>0);   //stop when meets empty line
			
			try {
				writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFileName, true)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(curCourse.toString());
			writer.write(curCourse.toString());
			writer.close();
			doneClassList.add(curCourse);
			writeToClassListFile(doneClassList);
			
		}
	}
	
	public static void main(String[] args) {
//		System.out.println(BookCount.count());
		Runner runner = new Runner();
		runner.execute();
	}
	
	public ArrayList<BookType> getBookTypeGroup (String _ISBN13, int knownVer) {
		ArrayList<BookType> ret = new ArrayList<BookType>();
//		String[] ret = new String[3];   //ret[0]: hardcover, ret[1]: papercover
//		ret[0] = "";
//		ret[1] = "";
//		ret[2] = "";      //note for this book
		String curBookType;
		System.err.println("Known ver #: " + knownVer);
		int curBookVer = knownVer==0? 0 : knownVer;
		
		String _ISBN10 = ISBN13to10(_ISBN13);		
		System.out.println(_ISBN10);
		Document doc = null;
		try {
			Connection curConnection = Jsoup.connect("http://www.amazon.com/request/dp/" + _ISBN10 + "/ref=sr_1_1?s=books&ie=UTF8&qid=id&sr=1-1");
			curConnection.timeout(600000);  //increase timeout so that the program can wait longer to get data
			doc = curConnection.get();
		} catch (IOException e) {       //can't find the book on amazon, meaning custom textbook
//			ret[0] = null;
//			ret[1] = null;
			ret = null;
			return ret;
		}
		
		//first know what type and version _ISBN13 is
		Elements curBookVerElems = doc.select("table tbody tr td.bucket div.content ul li");
		curBookType = curBookVerElems.get(0).text();
		if (curBookType.contains(":")) {
			curBookType = curBookType.split(":")[0];
		}
		System.err.println("Book type: " + curBookType);
		ret.add(new BookType(curBookType, _ISBN13));
		
////		System.out.println(curBookVerElems.html());
//		if (curBookVerElems.html().contains("Paperback")) {
//			curBookType = "Papercover";
////			ret[1] = _ISBN13;			
//			ret.add(new BookType("Paperback", _ISBN13));
//		}
//		else if (curBookVerElems.html().contains("Hardcover")) {
//			curBookType = "Hardcover";
//			ret.add(new BookType("Hardcover", _ISBN13));
////			ret[0] = _ISBN13;
//		}
//		else if (curBookVerElems.html().contains("Spiral-bound")){
//			curBookType = "Spiral-bound";
//			ret.add(new BookType("Spiral-bound", _ISBN13));
//		}
//		else if (curBookVerElems.html().contains("Textbook Binding")) {
//			curBookType = "Textbook Binding";
//			ret.add(new BookType("Textbook Binding", _ISBN13));
//		}
//		else if (curBookVerElems.html().contains("Loose Leaf")) {
//			curBookType = "Loose Leaf";
//			ret.add(new BookType("Loose Leaf", _ISBN13));
//		}
//		else if (curBookVerElems.html().contains("DVD-ROM")) {
//			curBookType = "DVD-ROM";
//			ret.add(new BookType("DVD-ROM", _ISBN13));
//		}
//		else if (curBookVerElems.html().contains("Plastic Comb")) {
//			curBookType = "Plastic Comb";
//			ret.add(new BookType("Plastic Comb", _ISBN13));
//		}
//		else if (curBookVerElems.html().contains("Unknown Binding")) {
//			curBookType = "Unknown Binding";
//			ret.add(new BookType("Unknown Binding", _ISBN13));
//		}
//		else if (curBookVerElems.html().contains("Audio CD")) {
//			curBookType = "Audio CD";
//			ret.add(new BookType("Audio CD", _ISBN13));
//		}
//		else if (curBookVerElems.html().contains("CD-ROM")) {
//			curBookType = "CD-ROM";
//			ret.add(new BookType("CD-ROM", _ISBN13));
//		}
//		else if (_ISBN10.equals("0716779390")) {
//			curBookType = "iClicker";
//			ret.add(new BookType("iClicker", _ISBN13));
////			ret[0] = _ISBN13;
////			ret[1] = null;
////			ret[2] = "iClicker";
//			return ret;
//		}
//		else {
//			throw new NoSuchElementException("Book type unknown.");
//		}
		
		
		if (curBookVer==0) {
			Scanner htmlScanner = new Scanner(curBookVerElems.html());
			String curLine = "";
	//		System.out.println(curBookVerElems.html());
			while (htmlScanner.hasNextLine()) {
				curLine = htmlScanner.nextLine();
				if (curLine.contains("Publisher:")) break;
			}
			curLine = curLine.replace("&amp;", "&");
			int startIndex = curLine.indexOf(";");
			if (startIndex == -1) {
				return ret;
			}
			curLine = curLine.substring(startIndex);
			startIndex = 0;
			int endIndex = curLine.indexOf("Edition");
			if (endIndex == -1) endIndex = curLine.indexOf("edition");
			String verNum = "";
			if (endIndex != -1) {                //version number exists
				verNum = curLine.substring(startIndex+1, endIndex);
				System.out.println(verNum);
				System.out.println("Ver #: " + getVersionNumber(verNum));
				curBookVer = getVersionNumber(verNum);
				if (curBookVer == 0) {    //Version number is not valid
					return ret;
				}
	//			throw new NoSuchElementException();
			} else {           //No version number. Return only the existing ISBN. 
				return ret;
			}
		}
		
		Elements moreButtons = doc.select("table.twisterMediaMatrix tbody tr td div div.cBoxInner table td.tmm_buttonTD");
//		System.out.println(moreButtons);
//		System.err.println("# of potential showMoreButton: " + moreButtons.size());
		
		//TODO 
        if (moreButtons.size()!=0) {
			if (dr == null) dr = new FirefoxDriver();
			dr.get("http://www.amazon.com/request/dp/" + _ISBN10 + "/ref=sr_1_1?s=books&ie=UTF8&qid=id&sr=1-1");
			
//			List<HtmlAnchor> anchorList = page.getAnchors();
//			ArrayList<HtmlAnchor> expandButtons  = new ArrayList<HtmlAnchor>();
//			for (HtmlAnchor curAnchor : anchorList) {
//				if (curAnchor.getId().endsWith("showMoreButton")) {
//					expandButtons.add(curAnchor);
//				}
//			}
			System.err.println("Check ");
			List<WebElement> expandButtons = dr.findElements(By.cssSelector("table.twisterMediaMatrix tbody tr td div div.cBoxInner table td.tmm_buttonTD a[id$=showMoreButton]"));
			
			System.err.println("# of real showMoreButton: " + expandButtons.size());
			for (WebElement curButton : expandButtons) {
				if (curButton.getAttribute("style").equals("")){
					curButton.click();
					System.err.println("Clicked one button");
				}
			}
			
//			//try 20 times to wait .5 second each for filling the page.
//			boolean AjaxLoaded = false;
//	        for (int i = 0; i < 20; i++) {
//	        	Elements bookVers = Jsoup.parse(page.getWebResponse().getContentAsString()).select("table.twisterMediaMatrix div.cBoxInner table tbody[id*=_binding_]");
//                for (Element bookVer : bookVers) { 
//                	if (bookVer.text().length()==0) {
////                		synchronized (page) {
//        	                try {
//								Thread.sleep(500);
//							} catch (InterruptedException e) {
//								e.printStackTrace();
//							}
////        	            }
//                		break;
//                	}
//                	AjaxLoaded = true;
//                }
//                if (AjaxLoaded) break;
//	        }
			
//	        doc = Jsoup.parse(dr.getPageSource());
	        
			(new WebDriverWait(dr, 600)).until(new ExpectedCondition<Boolean>() {
	            public Boolean apply(WebDriver d) {
	            	Elements bookVers = Jsoup.parse(d.getPageSource()).select("table.twisterMediaMatrix div.cBoxInner table tbody[id*=_binding_]");
	                for (Element bookVer : bookVers) { 
	                	if (bookVer.text().length()==0) return false;
	                }
	                return true;
	            }
	        });
			doc = Jsoup.parse(dr.getPageSource());
		}
		
		Elements bookVers = doc.select("table.twisterMediaMatrix div.cBoxInner table tbody[id*=_binding_]");
		
		for (Element curVer : bookVers) {
//			System.out.println(curVer);
			Elements curElems = curVer.select("td.tmm_bookTitle a");
			System.out.println(curElems);
			System.out.println("Avaliable books: " + curElems.size());
			for (Element curElem : curElems) {
				System.out.println("In inner loop.");
				String curLink = curElem.attr("href");
				
				int index = curLink.indexOf("dp/");
				String curISBN13 = ISBN10to13(curLink.substring(index+3, index+12));
				System.err.println("Going to "+curLink);
				Connection curConnection = Jsoup.connect(curLink);
				curConnection.timeout(600000);  //increase timeout so that the program can wait longer to get data
				try {
					doc = curConnection.get();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Elements bookVersElems = doc.select("table td[class=bucket] div[class=content] ul li");
//				System.out.println(bookVersElems.html());
				String curType = "";
				Scanner htmlScanner = new Scanner(bookVersElems.html());
				String curLine = "";
				int startIndex;
				int endIndex;
				while (htmlScanner.hasNextLine()) {
					curLine = htmlScanner.nextLine();
					if (curLine.contains("pages")) {
						System.out.println(curLine);
						startIndex = curLine.indexOf("<b>");
						endIndex = curLine.indexOf(":");
						curType = curLine.substring(startIndex+3, endIndex);
						System.out.println(curType);
					}
					if (curLine.contains("Publisher:")) break;
				}
				curLine = curLine.replace("&amp;", "&");
				System.out.println(curLine);
				startIndex = curLine.indexOf(";");
				if (startIndex == -1) {
					System.err.println("Alternative book Ver #: " + 0);
					continue;
				}
				curLine = curLine.substring(startIndex);
				startIndex = 0;
				endIndex = curLine.indexOf("Edition");
//				if ((endIndex != -1) && (endIndex<startIndex)) endIndex = curLine.substring(endIndex+7).indexOf("Edition");
				if (endIndex == -1) {
					endIndex = curLine.indexOf("edition");
//					if ((endIndex != -1) && (endIndex<startIndex)) endIndex = curLine.substring(endIndex+7).indexOf("edition");
				}
				if (endIndex == -1) {
					System.err.println("Alternative book Ver #: " + 0);
					continue;    //no version number provided
				}
				System.err.println("StartIndex: " + startIndex + ", " + "EndIndex: " + endIndex);
				String verNum = curLine.substring(startIndex+1, endIndex);
				System.out.println(verNum);
				System.err.println("Alternative book Ver #: " + getVersionNumber(verNum));
				if (getVersionNumber(verNum) == 0) {    //invalid version number
					System.err.println("Alternative book Ver #: " + 0);
					continue;
				}
				if (getVersionNumber(verNum)==curBookVer) {
					System.err.println("Alternative book Ver #: " + curBookVer +". Version number matched.");
					if (checkISBNValidity(curISBN13)) {
						ret.add(new BookType(curType, curISBN13));
						break;
					} else {
						System.err.println("Invalid ISBN");
						continue;
					}
				}
				if (!checkISBNValidity(curISBN13)) {
					System.err.println("Invalid ISBN");
					continue;    //skip if the ISBN is not valid
				}
				
			}

		}
		
		boolean ISBNInReturnArray = false;
		for (BookType curType : ret) {
			if (curType.bookISBN.contains(_ISBN13)) {
//				System.out.println("Cur ISBN: " + curType.bookISBN);
				ISBNInReturnArray = true;
				break;
			}
		}
		if (!ISBNInReturnArray)	{
			for (BookType curType : ret) {
				System.out.println(curType.toString());
			}
			throw new NoSuchElementException("_ISBN13 not in return array"); //_ISBN13 must be one of them
		}
		
		return ret;
	}
	
	public static String ISBN13to10(String ISBN13) {
		String CheckDigits = new String("0123456789X0");
	    String s9;
	    int i, n, v;
	    boolean ErrorOccurred;
	    ErrorOccurred = false;
	    s9 = ISBN13.substring(3, 12);
	    n = 0;
	    for (i=0; i<9; i++) {
	      if (!ErrorOccurred) {
	        v = Character.getNumericValue(s9.charAt(i));
	        if (v==-1) ErrorOccurred = true;
	        else n = n + (10 - i) * v; 
	      }
	    }
	    if (ErrorOccurred) return "ERROR";
	    else {
	      n = 11 - (n % 11);
	      return s9 + CheckDigits.substring(n, n+1); 
	    }
	  }
	
	public String ISBN10to13(String ISBN10) {
		String CheckDigits = new String("0123456789X0");
	    String s12;
	    int i, n, v;
	    boolean ErrorOccurred;
	    ErrorOccurred = false;
	    s12 = "978" + ISBN10.substring(0, 9);
	    n = 0;
	    for (i=0; i<12; i++) {
	      if (!ErrorOccurred) {
	        v = Character.getNumericValue(s12.charAt(i));
	        if (v==-1) ErrorOccurred = true;
	        else {
	          if ((i % 2)==0) n = n + v;
	          else n = n + 3*v;
	        }
	      }
	    }
	    if (ErrorOccurred) return "ERROR";
	    else {
	      n = n % 10;
	      if (n!=0) n = 10 - n;
	      return s12 + CheckDigits.substring(n, n+1);
	    }
	  }
	
	public int getVersionNumber(String verStr) {
//		System.err.println("Ver str checked: " + verStr);
		if ((verStr.toLowerCase().contains("first"))||(verStr.contains(" 1 "))||(verStr.toLowerCase().contains("1st"))) return 1;
		if ((verStr.toLowerCase().contains("second"))||(verStr.contains(" 2 "))||(verStr.toLowerCase().contains("2nd"))) return 2;
		if ((verStr.toLowerCase().contains("third"))||(verStr.contains(" 3 "))||(verStr.toLowerCase().contains("3rd"))) return 3;
		if ((verStr.toLowerCase().contains("fourth"))||(verStr.contains(" 4 "))||(verStr.toLowerCase().contains("4th"))) return 4;
		if ((verStr.toLowerCase().contains("fifth"))||(verStr.contains(" 5 "))||(verStr.toLowerCase().contains("5th"))) return 5;
		if ((verStr.toLowerCase().contains("sixth"))||(verStr.contains(" 6 "))||(verStr.toLowerCase().contains("6th"))) return 6;
		if ((verStr.toLowerCase().contains("seventh"))||(verStr.contains(" 7 "))||(verStr.toLowerCase().contains("7th"))) return 7;
		if ((verStr.toLowerCase().contains("eighth"))||(verStr.contains(" 8 "))||(verStr.toLowerCase().contains("8th"))) return 8;
		if ((verStr.toLowerCase().contains("ninth"))||(verStr.contains(" 9 "))||(verStr.toLowerCase().contains("9th"))) return 9;
		if ((verStr.toLowerCase().contains("tenth"))||(verStr.contains(" 10 "))||(verStr.toLowerCase().contains("10th"))) return 10;
		if ((verStr.toLowerCase().contains("eleventh"))||(verStr.contains(" 11 "))||(verStr.toLowerCase().contains("11th"))) return 11;
		if ((verStr.toLowerCase().contains("Twelfth"))||(verStr.contains(" 12 "))||(verStr.toLowerCase().contains("12th"))) return 12;
		if ((verStr.toLowerCase().contains("thirteenth"))||(verStr.contains(" 13 "))||(verStr.toLowerCase().contains("13th"))) return 13;
		if ((verStr.toLowerCase().contains("fourteenth"))||(verStr.contains(" 14 "))||(verStr.toLowerCase().contains("14th"))) return 14;
		if ((verStr.toLowerCase().contains("fifteenth"))||(verStr.contains(" 15 "))||(verStr.toLowerCase().contains("15th"))) return 15;
		if ((verStr.toLowerCase().contains("Sixteenth"))||(verStr.contains(" 16 "))||(verStr.toLowerCase().contains("16th"))) return 16;
		if ((verStr.toLowerCase().contains("Seventeenth"))||(verStr.contains(" 17 "))||(verStr.toLowerCase().contains("17th"))) return 17;
		if ((verStr.toLowerCase().contains("eighteenth"))||(verStr.contains(" 18 "))||(verStr.toLowerCase().contains("18th"))) return 18;
		if ((verStr.toLowerCase().contains("Nineteenth"))||(verStr.contains(" 19 "))||(verStr.toLowerCase().contains("19th"))) return 19;
		if ((verStr.toLowerCase().contains("twentieth"))||(verStr.contains(" 20 "))||(verStr.toLowerCase().contains("20th"))) return 20;
		if ((verStr.contains("Revised"))||(verStr.contains("Updated"))) return 2;
		if ((verStr.contains("Reprint"))) return 1;
		if ((verStr.contains("New"))) return 2;
//		if ((verStr.contains("Basic Books"))) return -1;
//		if ((verStr.contains("Combination"))) return -1;
//		if ((verStr.contains("Reissue"))) return -1;
//		if ((verStr.contains("40th"))) return 40;
//		if ((verStr.contains("College"))) return -1;
//		if ((verStr.contains("IMPORT"))) return -1;
//		if ((verStr.contains("Import"))) return -1;
//		if ((verStr.contains("Centenary"))) return -1;
//		if ((verStr.contains("Expanded"))) return -1;
//		if ((verStr.contains("Rev Upd Su"))) return -1;
//		if ((verStr.contains("ZZZ"))) return -1;
//		if ((verStr.contains("annotated"))) return -1;
//		if ((verStr.contains("Har/Cdr"))) return -1;
//		if ((verStr.contains("2003"))) return -1;
		return 0;
//		throw new NoSuchElementException();
	}
	
	public boolean checkISBNValidity (String _ISBN13){
		Document doc = null;
		try {
//			Connection curConnection = Jsoup.connect("http://www.dealoz.com/prod.pl?shipping_type=standard&quantity=1&class=new&class=used&class=international&class=gbb&zip=&cat=book&lang=en-us&op=buy&search_country=us&shipto=us&cur=usd&nw=y&ean="+_ISBN13+"&asin="+_ISBN13+"&mfr=Prentice&prev=&limit=100&sort=total_cost%3Aasc&catby=&query=");
			Connection curConnection = Jsoup.connect("http://www.biblio.com/search.php?stage=1&result_type=works&keyisbn="+_ISBN13);
			curConnection.timeout(600000);  //increase timeout so that the program can wait longer to get data
			doc = curConnection.get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (doc.html().contains("503 Service unavailable")) {
//			try {
//				throw new NoSuchElementException("Can't connect to dealoz");
				throw new NoSuchElementException("Can't connect to Biblio");
//				Thread.sleep(300000);
//				Connection curConnection = Jsoup.connect("http://www.dealoz.com/prod.pl?shipping_type=standard&quantity=1&class=new&class=used&class=international&class=gbb&zip=&cat=book&lang=en-us&op=buy&search_country=us&shipto=us&cur=usd&nw=y&ean="+_ISBN13+"&asin="+_ISBN13+"&mfr=Prentice&prev=&limit=100&sort=total_cost%3Aasc&catby=&query=");
//				curConnection.timeout(600000);  //increase timeout so that the program can wait longer to get data
//				doc = curConnection.get();
//			} catch (IOException e) {
//				e.printStackTrace();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}
		if (doc.html().contains("We're sorry. We were unable to find any books matching your criteria.")){
			return false;
		} else {
			return true;
		}
	}
	
	public TreeSet<CourseNode> readFromClassListFile() {
		TreeSet<CourseNode> ret = new TreeSet<CourseNode>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("DoneClassList_1.txt"));
			Scanner sc = new Scanner(br);
			String[] curElems = null;
			while (sc.hasNextLine()) {
				curElems = sc.nextLine().split(" ");
				ret.add(new CourseNode(curElems[0], curElems[1], curElems[2], curElems[3]));
			}
			br.close();
			return ret;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	public static void writeToClassListFile(TreeSet<CourseNode> doneList) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter("DoneClassList_1.txt", false));
			for (CourseNode curCourse : doneList) {
				writer.write(curCourse.getCourseInfo());
				writer.write("\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void writeToTimeCountFile(Long timeUsed){
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter("TimeUsed_10Books_1.txt", true));
			writer.write(timeUsed.toString());
			writer.write("\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

class Dealoz_Parser {
	
	static String tokenBtwSubvalue = "/";
	static String tokenBtwValue = ",";
//	static String sourceFileName = "ISBNsource.txt";
	String bookISBN;

	Dealoz_Parser(){
		
	}
	
	public String crawl (String _bookISBN) {    //ISBN 13
		Document doc = null;
		bookISBN = _bookISBN;
		doc = null;
		
		try {
			Connection curConnection = Jsoup.connect("http://www.dealoz.com/prod.pl?shipping_type=standard&quantity=1&class=new&class=used&class=international&class=gbb&zip=&cat=book&lang=en-us&op=buy&search_country=us&shipto=us&cur=usd&nw=y&ean="+bookISBN+"&asin="+bookISBN+"&mfr=Prentice&prev=&limit=100&sort=total_cost%3Aasc&catby=&query=");
			curConnection.timeout(600000);  //increase timeout so that the program can wait longer to get data
			doc = curConnection.get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (doc.html().contains("503 Service unavailable")) {
				throw new NoSuchElementException("Can't connect to dealoz");
		}
		if (doc.html().contains("There's no pricing information available for this product.")){
			return null;
		}
		
		Elements offerTable = null;
		offerTable = doc.select("div[class=prod-price] table").get(1).select("tbody");
		
		
		String output = "";
		Element curRow = null;
		
		offerTable = offerTable.select("tr[height=40px]");
		
		for (int i=0; i<offerTable.size(); i++){
			curRow = offerTable.get(i);
			output = output + formatRow(curRow);
		}
	    
//		System.out.println(output);
		return output;
	}
	
	public static String formatRow(Element curRow){
		String output = "";
		
		//book condition
		String bookCondStr = curRow.select("div.condition").text();
		for (int i=0; i<bookCondStr.length(); i++){
			String curChar = bookCondStr.substring(i, i+1);
			if (!Character.isLetterOrDigit(curChar.toCharArray()[0])&&(!curChar.equals(" "))&&(!curChar.equals("."))) {
				bookCondStr = bookCondStr.substring(0, i) + bookCondStr.substring(i+1);
			}
		}
//		System.out.println(bookCondStr);
		int pos = bookCondStr.indexOf("ℜ");
		if (pos!=-1) bookCondStr = bookCondStr.substring(0, pos) + " & Re" + bookCondStr.substring(pos+1);
		bookCondStr = checkComma(bookCondStr);
		String bookCondRet = "";
		if (bookCondStr.contains("New")) bookCondRet = bookCondRet + "New";
		if (bookCondStr.contains("Used")) bookCondRet = bookCondRet + "Used";
		if (bookCondStr.contains("Intl")) bookCondRet = bookCondRet + " International";
		if (bookCondStr.contains("Rental")) bookCondRet = bookCondRet==""? "Rental" : " Rental";
		if (bookCondRet.equals("")) bookCondRet = "_Unknown";
		output = output + bookCondRet + ",";
		
		//book price
//		System.out.println(curRow.select("a.price_button").text());
		String bookPrice = curRow.select("a.price_button").text();
		bookPrice = checkComma(bookPrice);
		output = output + bookPrice + ",";
		
		//buying method
		String buyMethodStr = curRow.select("img[src=/img/shop/en-us/buy.gif],img[src=/img/shop/en-us/gbb.gif],img[src=/img/shop/en-us/bid.gif],img[src=/img/shop/en-us/rent.gif]").get(0).attr("src");
//		System.out.println(buyMethodStr);  
		String buyMethodName = null;
		if (buyMethodStr.equals("/img/shop/en-us/buy.gif")) buyMethodName = "Buy at store";
		else if (buyMethodStr.equals("/img/shop/en-us/gbb.gif")) buyMethodName = "Buy & return";
		else if (buyMethodStr.equals("/img/shop/en-us/bid.gif")) buyMethodName = "Bid at store";
		else buyMethodName = "Rent at store";
		output = output + buyMethodName + ",";
	
		//buy link
		Element buyLinkElem = curRow.select("td table tbody tr td a").get(0);
		String buyLink = "http://www.dealoz.com" + buyLinkElem.attr("href");
		output = output + buyLink + ",";
//		output = output + "link" + ",";
		
		//shipping fee
//		System.out.println(curRow.select("td[width=12%]").get(0).select("div[style=color:#333333; padding-left:10px; font-size:12px;]").text()); 
		String shippingFee = curRow.select("td div[style=color:#333333; padding-left:10px; font-size:13px;]").text();
		shippingFee = checkComma(shippingFee);
		output = output + shippingFee + ",";
		
		//Estimated shipping time
//		System.out.println(curRow.select("td[width=12%]").get(0).select("div.notes").text()); 
		String shippingTime = curRow.select("td div.notes").text();
		shippingTime = checkComma(shippingTime);
		output = output + shippingTime + ",";
		
		//Deals Applied
//		System.out.println(curRow.select("td[width=12%]").get(1).select("span").text()); 
		String dealsApplied = curRow.select("td div[style=padding-left:10px;]").text();
		dealsApplied = checkComma(dealsApplied);
		output = output + dealsApplied + ",";
		
		//Total price
//		System.out.println(curRow.select("font[style=font-size:18px;font-weight:bold;color:#000000;]").text()); 
		String totalPrice = curRow.select("font[style=font-size:18px;font-weight:bold;color:#000000;]").text();
		totalPrice = checkComma(totalPrice);
		output = output + totalPrice + ",";
		
		//Store name
		Elements storeNameHTML = curRow.select("div[id=prod-store-info] a").select("b");
		String storeName = "";
		if (storeNameHTML.size()==0){
			storeName = curRow.select("div[id=prod-store-info] a").select("img[title]").get(0).attr("title");
		} else {
			storeName = curRow.select("div[id=prod-store-info] a").select("b").text();
		}
//		System.out.println(storeName);
		storeName = checkComma(storeName);
		output = output + storeName;
		
		output = output + "\n";
		
//		System.out.println(output);
	    return output;
	}
	
	//replace comma with semicolon in the string to avoid confusion when transforming to Excel table
	public static String checkComma(String str){
		String ret = str;
		int pos = str.indexOf(",");
		if (pos!=-1) ret = ret.substring(0, pos) + ";" + ret.substring(pos+1);
		return ret;
	}
	
	
		
		
}
	

