package models;

import java.io.IOException;

/**
 * Interface for storing images. Can be backed by a NoSQL db like HBase, or a relational DB,
 * or a blob store.
 * 
 * @author Agis Chartsias
 *
 */
public interface IStorage {

	/**
	 * Store an image to the datastore
	 * @param photo
	 * @return true on success
	 * @throws IOException 
	 */
	public boolean store(Photo photo) throws IOException;
	
	/**
	 * Store a comment to the datastore
	 * @param comment
	 * @throws IOException 
	 */
	public void store(Comment comment) throws IOException;
	
	/**
	 * @param key
	 * @return a stored photo given its id
	 * @throws IOException 
	 */
	public Photo getPhoto(byte[] key) throws IOException;
	
	/**
	 * @return an iterator over photos
	 */
	public Iterable<Photo> getPhotos();

	/**
	 * Construct a key for the photo storage. This might be different
	 * for every implementation.
	 * @param photo
	 * @return
	 */
	byte[] getPhotoKey(String hashId, Long date);
	
	/**
	 * Construct a key for the comment storage
	 * @param comment
	 * @return
	 */
	byte[] getCommentKey(String hashId, Long date);
	
	/**
	 * @param p
	 * @return the comments for a photo
	 */
	public Iterable<Comment> getComments(Photo p);
}
