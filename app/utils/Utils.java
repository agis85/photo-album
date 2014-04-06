package utils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains utility methods.
 * 
 * @author Agis Chartsias
 *
 */
public class Utils {

	final static Logger log = LoggerFactory.getLogger(Utils.class);
	
	/**
	 * Create an MD5 hash of the given data. 
	 * @param data
	 * @return a hex representation of the hashed data
	 * @throws NoSuchAlgorithmException
	 */
	public static String md5(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(data);
		byte[] hash = md.digest();
		return Hex.encodeHexString(hash);
	}
	
	/**
	 * Get the named HTable from HBase, creating it if necessary
	 * @param tableName
	 * @param families
	 * @param conf
	 * @throws IOException
	 * @throws IOException
	 */
	public static void createHTable(String tableName, String[] families, Configuration conf) throws IOException  {
		assert tableName != null;
		assert families != null;
		HBaseAdmin admin = new HBaseAdmin(conf);
		try {
			if (!admin.tableExists(tableName)) {
				log.debug("Creating table {}", tableName);
				HTableDescriptor tableDesc = new HTableDescriptor(tableName);
				for (int i = 0; i < families.length; i++)
					tableDesc.addFamily(new HColumnDescriptor(families[i]));
				admin.createTable(tableDesc);
			}
		} finally {
			admin.close();
		}
	}
	
	/**
	 * Check if a table exists in HBase
	 * @param tableName
	 * @param conf
	 * @return
	 */
	public static boolean tableExists(String tableName, Configuration conf) {
		HBaseAdmin admin = null;
		try {
			admin = new HBaseAdmin(conf);
			return admin.tableExists(tableName);
		} catch (MasterNotRunningException e) {
			log.error("", e);
		} catch (ZooKeeperConnectionException e) {
			log.error("", e);
		} catch (IOException e) {
			log.error("", e);
		} finally {
			try {
				if (admin != null) admin.close();
			} catch (IOException e) {
				log.error("", e);
			}
		}
		return false;
	}
}
