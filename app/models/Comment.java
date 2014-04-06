package models;


/**
 * Base class of a photo's comment
 * 
 * @author Agis Chartsias
 *
 */
public class Comment {

	private String contents;
	private Photo parent;
	private Long date;
	
	public static enum Sentiment {
		POSITIVE,
		NEGATIVE
	};
	
	public Comment(String contents, Photo parent, Long date) {
		this.contents = contents;
		this.parent = parent;
		this.date = date;
	}
	
	public String getContents() {
		return contents;
	}
	public Photo getParent() {
		return parent;
	}
	public Long getDate() {
		return date;
	}
}
