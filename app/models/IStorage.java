package models;

import java.util.Iterator;

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
	 */
	public boolean store(Photo photo);
	
	/**
	 * @param id
	 * @return a stored photo given its id
	 */
	public Photo getImage(String id);
	
	/**
	 * @return an iterator over photos
	 */
	public Iterator<Photo> getPhotos();
}
