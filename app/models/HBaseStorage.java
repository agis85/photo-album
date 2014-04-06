package models;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.Utils;

/**
 * An HBase implementation of an IStorage
 * 
 * The key of each row will be of the form inverse_timestamp-photo_hash and the value the 
 * photo's byte array. Having such a key structure will allow us to scan for sorted images
 * 
 * @author Agis Chartsias
 *
 */
public class HBaseStorage implements IStorage {
	
	static Logger log = LoggerFactory.getLogger(HBaseStorage.class);
	
	private static String TABLE_NAME = "PHOTO-STORE";
	private static String[] COLUMN_FAMILY = {"cf"};
	private static int KEY_LENGTH = 8 + 16; // 8 for timestamp + 16 for hash id
	
	Configuration configuration;
	HTablePool pool;
	
	public HBaseStorage() {
		log.info("Opening HBaseStorage");
		this.configuration = HBaseConfiguration.create();
		pool = new HTablePool(configuration, Integer.MAX_VALUE);
		
		try {
			Utils.createHTable(TABLE_NAME, COLUMN_FAMILY, configuration);
		} catch (IOException e) {
			log.error("HBaseStorage init error");
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean store(Photo photo) throws IOException {
		assert Utils.tableExists(TABLE_NAME, configuration) : "Photo table does not exist";
		
		HTableInterface table = pool.getTable(TABLE_NAME);
		try {
			table.put(photoToPut(photo));
		} finally {
			log.debug("Returning table to pool");
			table.close();
		}
		return false;
	}

	@Override
	public Photo getImage(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<Photo> getPhotos() {
		return new PhotoIterable(10);
	}

	/**
	 * HBase column names
	 */
	private static byte[] COL_PHOTO_VALUE = "value".getBytes();
	private static byte[] COL_PHOTO_TITLE = "title".getBytes();
	private static byte[] COL_PHOTO_DATE = "date".getBytes();
	
	/**
	 * @param photo
	 * @return an HBase Put opject representation of a photo
	 */
	private Put photoToPut(Photo photo) {
		Put put = new Put(getKey(photo));
		put.add(COLUMN_FAMILY[0].getBytes(), COL_PHOTO_VALUE, photo.getContents());
		put.add(COLUMN_FAMILY[0].getBytes(), COL_PHOTO_TITLE, photo.getTitle().getBytes());
		put.add(COLUMN_FAMILY[0].getBytes(), COL_PHOTO_DATE, Bytes.toBytes(photo.getDate().getTime()));
		return put;
	}
	
	/**
	 * Convert a HBase Result to a Photo
	 * @param row
	 * @return
	 */
	private Photo read(Result row) {
		byte[] value = row.getValue(COLUMN_FAMILY[0].getBytes(), COL_PHOTO_VALUE);
		byte[] title = row.getValue(COLUMN_FAMILY[0].getBytes(), COL_PHOTO_TITLE);
		byte[] date = row.getValue(COLUMN_FAMILY[0].getBytes(), COL_PHOTO_DATE);
		Date d = new Date(Bytes.toLong(date));
		Photo photo = new Photo(Bytes.toString(title), value, d);
		return photo;
	}
	
	
	/**
	 * Construct a key for the photo table. The key' length is 32-bytes 
	 * from which the first 8 are the reverse timestamp and the next 16
	 * are the photo's MD5 hash id.
	 *  
	 * @param photo
	 * @return
	 */
	private byte[] getKey(Photo photo) {
		long invTime = Integer.MAX_VALUE - photo.getDate().getTime();
		byte[] hash = photo.getId().getBytes();
		
		byte[] key = new byte[KEY_LENGTH];
		Bytes.putLong(key, 0, invTime);
		Bytes.putBytes(key, 9, hash, 0, 16);
		return key;
	}
	
	/**
	 * An iterable over the photo storage table. The results
	 * are sorted by newest-first
	 */
	class PhotoIterable implements Iterable<Photo> {
		long numResults;
		
		public PhotoIterable(long numResults) {
			this.numResults = numResults;
		}
		
		@Override
		public Iterator<Photo> iterator() {
			final Scan scan = new Scan();
			
			try {
				return new Iterator<Photo>() {
					// Don't use HTablePool to get table to avoid synchronisation
					// issues with other storing threads
					HTableInterface table = new HTable(configuration, TABLE_NAME);
					ResultScanner scanner = table.getScanner(scan);
					long count = 0;
					Photo current = getNext();
					
					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
					
					@Override
					public Photo next() {
						Photo cur = current;
						current = getNext();
						if (current == null) {
							try {
								scanner.close();
								table.close();
							} catch (IOException e) {
								log.error("", e);
							}
						}
						return cur;
					}
					
					@Override
					public boolean hasNext() {
						if (current == null) return false;
						return true;
					}
					
					private Photo getNext() {
						if (count >= numResults) return null;
						
						 try {
							Result row = scanner.next();
							if (row == null) return null;
							Photo photo = read(row);
							count++;
							return photo;
						} catch (IOException e) {
							log.error("Photo read error", e);
						}
						return null;
					}
				};
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
	}
}
