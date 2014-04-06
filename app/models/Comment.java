package models;

/**
 * Base class of a photo's comment
 * 
 * @author Agis Chartsias
 *
 */
public class Comment {

	private String id;
	private String contents;
	private Photo parent;
	
	public static enum Sentiment {
		POSITIVE,
		NEGATIVE
	};
	
	public Comment(String contents, Photo parent) {
		this.contents = contents;
		this.parent = parent;
	}
	
	public String getId() {
		return id;
	}
	public String getContents() {
		return contents;
	}
	public Photo getParent() {
		return parent;
	}
}
