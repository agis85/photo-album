package models;

import java.security.NoSuchAlgorithmException;

import utils.Utils;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * A photo of the album. It contains a title, date and a byte representation of
 * the image.
 * 
 * @author Agis Chartsias
 *
 */
public class Photo {

	private Long date;
	private String title;
	private String imageHash;
	private byte[] contents;
	
	public Photo(String id, Long date) {
		this.imageHash = id;
		this.date = date;
	}
	
	public Photo(String title, byte[] contents, Long date) {
		this.title = title;
		this.contents = contents;
		this.date = date;
		try {
			this.imageHash = Utils.md5(contents);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getImageHash() {
		return imageHash;
	}
	
	public byte[] getContents() {
		return contents;
	}
	
	public Long getDate() {
		return date;
	}
	
	/**
	 * Used in HTML image rendering
	 * @return a base64 string of the photo's contents
	 */
	public String getContentsBase64() {
		return Base64.encode(contents);
	}
}
