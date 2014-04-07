package models;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
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
	
	private static String PHOTO_TABLE = "PHOTO-STORE";
	private static String COMMENT_TABLE = "COMMENT-STORE";
	private static String[] COLUMN_FAMILY = {"cf"};
	private static int KEY_LENGTH = 8 + 32; // 8 for timestamp + 32 for hash id
	
	Configuration configuration;
	HTablePool photoPool;
	HTablePool commentPool;
	
	public HBaseStorage() {
		log.info("Opening HBaseStorage");
		this.configuration = HBaseConfiguration.create();
		photoPool = new HTablePool(configuration, Integer.MAX_VALUE);
		commentPool = new HTablePool(configuration, Integer.MAX_VALUE);
		
		try {
			Utils.createHTable(PHOTO_TABLE, COLUMN_FAMILY, configuration);
			Utils.createHTable(COMMENT_TABLE, COLUMN_FAMILY, configuration);
		} catch (IOException e) {
			log.error("HBaseStorage init error");
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean store(Photo photo) throws IOException {
		assert Utils.tableExists(PHOTO_TABLE, configuration) : "Photo table does not exist";
		
		HTableInterface table = photoPool.getTable(PHOTO_TABLE);
		try {
			table.put(photoToPut(photo));
		} finally {
			log.debug("Returning table to pool");
			table.close();
		}
		return true;
	}

	@Override
	public Photo getPhoto(byte[] key) throws IOException {
		assert key != null : "Null key";
		assert Utils.tableExists(PHOTO_TABLE, configuration) : "Photo table does not exist!";

		Get get = new Get(key);
		HTableInterface table = photoPool.getTable(PHOTO_TABLE);
		try {
			Result row = table.get(get);
			if (row.isEmpty()) {
				log.debug("Got no entry for {}", key);
				return null;
			}

			Photo photo = readPhoto(row);
			return photo;
		}
		finally {
			log.debug("Returning table to pool after txn get");
			table.close();
		}
	}

	@Override
	public Iterable<Photo> getPhotos() {
		return new Iterable<Photo>() {
			long numResults = 20;
					
			@Override
			public Iterator<Photo> iterator() {
				final Scan scan = new Scan();
				
				try {
					return new Iterator<Photo>() {
						// Don't use HTablePool to get table to avoid synchronisation
						// issues with other storing threads
						HTableInterface table = new HTable(configuration, PHOTO_TABLE);
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
								Photo result = readPhoto(row);
								count++;
								return result;
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
		};
	}
		
	/********************
	 * 					*
	 *  PHOTO STORAGE   *
	 * 					*
	 ********************/
	
	/**
	 * HBase photo column names
	 */
	private static byte[] COL_PHOTO_VALUE = "value".getBytes();
	private static byte[] COL_PHOTO_TITLE = "title".getBytes();
	private static byte[] COL_PHOTO_DATE = "date".getBytes();
	
	/**
	 * @param photo
	 * @return an HBase Put opject representation of a photo
	 */
	private Put photoToPut(Photo photo) {
		Put put = new Put(getPhotoKey(photo.getImageHash(), photo.getDate()));
		put.add(COLUMN_FAMILY[0].getBytes(), COL_PHOTO_VALUE, photo.getContents());
		put.add(COLUMN_FAMILY[0].getBytes(), COL_PHOTO_TITLE, photo.getTitle().getBytes());
		put.add(COLUMN_FAMILY[0].getBytes(), COL_PHOTO_DATE, Bytes.toBytes(photo.getDate()));
		return put;
	}
	
	/**
	 * Convert a HBase Result to a Photo
	 * @param row
	 * @return
	 */
	private Photo readPhoto(Result row) {
		byte[] value = row.getValue(COLUMN_FAMILY[0].getBytes(), COL_PHOTO_VALUE);
		byte[] title = row.getValue(COLUMN_FAMILY[0].getBytes(), COL_PHOTO_TITLE);
		Long date = Bytes.toLong(row.getValue(COLUMN_FAMILY[0].getBytes(), COL_PHOTO_DATE));
		Photo photo = new Photo(Bytes.toString(title), value, date);
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
	@Override
	public byte[] getPhotoKey(String hashId, Long date) {
		assert date != null;
		
		long invTime = Long.MAX_VALUE - date;
		byte[] hash = hashId.getBytes();
		
		byte[] key = new byte[KEY_LENGTH];
		Bytes.putLong(key, 0, invTime);
		Bytes.putBytes(key, 8, hash, 0, 32);
		return key;
	}
	
	
	/*********************
	 * 					 *
	 *  COMMENT STORAGE  *
	 *					 *  					
	 *********************/
	
	/**
	 * HBase comment column names
	 */
	private static byte[] COL_COMMENT_PARENT = "parent".getBytes();
	private static byte[] COL_COMMENT_BODY = "body".getBytes();
	private static byte[] COL_COMMENT_DATE = "date".getBytes();
	
	/**
	 * @param photo
	 * @return an HBase Put opject representation of a photo
	 */
	private Put commentToPut(Comment comment) {
		Put put = new Put(getCommentKey(comment.getParent().getImageHash(), comment.getDate()));
		put.add(COLUMN_FAMILY[0].getBytes(), COL_COMMENT_BODY, comment.getContents().getBytes());
		put.add(COLUMN_FAMILY[0].getBytes(), COL_COMMENT_PARENT, getPhotoKey(comment.getParent().getImageHash(), comment.getParent().getDate()));
		put.add(COLUMN_FAMILY[0].getBytes(), COL_COMMENT_DATE, Bytes.toBytes(comment.getDate()));
		return put;
	}
	
	/**
	 * Convert a HBase Result to a Photo
	 * @param row
	 * @return
	 * @throws IOException 
	 */
	private Comment readComment(Result row) throws IOException {
		byte[] body = row.getValue(COLUMN_FAMILY[0].getBytes(), COL_COMMENT_BODY);
		byte[] photoKey = row.getValue(COLUMN_FAMILY[0].getBytes(), COL_COMMENT_PARENT);
		Long date = Bytes.toLong(row.getValue(COLUMN_FAMILY[0].getBytes(), COL_COMMENT_DATE));
		
		Photo parent = getPhoto(photoKey);
		Comment comment = new Comment(Bytes.toString(body), parent, date);
		return comment;
	}
	
	/**
	 * Construct a key for the photo table. The key' length is 32-bytes 
	 * from which the first 32 are the photo's hash id and the last
	 * 8 are the reverse timestamp.
	 *  
	 * @param photo
	 * @return
	 */
	public byte[] getCommentKey(String hashId, Long date) {
		long invTime = Long.MAX_VALUE - date;
		byte[] hash = hashId.getBytes();
		
		byte[] key = new byte[KEY_LENGTH];
		Bytes.putBytes(key, 0, hash, 0, 32);
		Bytes.putLong(key, 32, invTime);
		return key;
	}
	
	@Override
	public void store(Comment comment) throws IOException {
		assert Utils.tableExists(PHOTO_TABLE, configuration) : "Photo table does not exist";
		
		HTableInterface table = photoPool.getTable(COMMENT_TABLE);
		try {
			table.put(commentToPut(comment));
		} finally {
			log.debug("Returning table to pool");
			table.close();
		}
	}
	
	@Override
	public Iterable<Comment> getComments(final Photo p) {
		return new Iterable<Comment>() {
			long numResults = 20;
			byte[] startRow = getCommentKey(p.getImageHash(), Long.MAX_VALUE);
			byte[] stopRow = getCommentKey(p.getImageHash(), 0L);
					
			@Override
			public Iterator<Comment> iterator() {
				final Scan scan = new Scan();
				scan.setStartRow(startRow);
				scan.setStopRow(stopRow);
				
				try {
					return new Iterator<Comment>() {
						// Don't use HTablePool to get table to avoid synchronisation
						// issues with other storing threads
						HTableInterface table = new HTable(configuration, COMMENT_TABLE);
						ResultScanner scanner = table.getScanner(scan);
						long count = 0;
						Comment current = getNext();
						
						@Override
						public void remove() {
							throw new UnsupportedOperationException();
						}
						
						@Override
						public Comment next() {
							Comment cur = current;
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
						
						private Comment getNext() {
							if (count >= numResults) return null;
							
							 try {
								Result row = scanner.next();
								if (row == null) return null;
								Comment result = readComment(row);
								count++;
								return result;
							} catch (IOException e) {
								log.error("Comment read error", e);
							}
							return null;
						}
					};
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
}
