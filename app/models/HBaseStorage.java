package models;

import java.util.Iterator;

/**
 * An HBase implementation of an IStorage
 * @author Agis Chartsias
 *
 */
public class HBaseStorage implements IStorage {

	@Override
	public boolean store(Photo photo) {
		return false;
	}

	@Override
	public Photo getImage(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Photo> getPhotos() {
		// TODO Auto-generated method stub
		return null;
	}

}
