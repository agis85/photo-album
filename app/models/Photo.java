package models;

import java.util.Date;

/**
 * A photo of the album. It contains a title, date and a byte representation of
 * the image.
 * 
 * @author Agis Chartsias
 *
 */
public class Photo {

	private Date date;
	private String title;
	private String id;
	private byte[] contents;
	
	public Photo(String title, byte[] contents, Date date) {
		this.title = title;
		this.contents = contents;
		this.date = date;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getId() {
		return id;
	}
	
	public byte[] getContents() {
		return contents;
	}
	
	public Date getDate() {
		return date;
	}
}
