package models;

import java.security.NoSuchAlgorithmException;
import java.util.Date;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import utils.Utils;

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
		try {
			this.id = Utils.md5(contents);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
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
	
	public String getBase64() {
		return Base64.encode(contents);
	}
}
