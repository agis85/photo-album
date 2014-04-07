package controllers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

import models.Comment;
import models.HBaseStorage;
import models.IStorage;
import models.Photo;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

import org.apache.commons.codec.binary.Base64;



public class Application extends Controller {
	
	private static IStorage storage;
    public static IStorage getStorage() {
    	if (storage == null) storage = new HBaseStorage();
    	return storage;
    }
  
    public static Result index() {
        return ok(views.html.index.render());
    }

    /**
     * Upload a new photo to the database
     * @return
     * @throws IOException
     */
    public static Result upload() throws IOException {
    	DynamicForm form = Form.form().bindFromRequest();
    	String title = form.get("title");
    	
    	MultipartFormData formData = request().body().asMultipartFormData();
    	if (formData == null) {
			flash("error", "Wrong request body format");
			return badRequest(views.html.index.render());
		}
		FilePart data = formData.getFile("data");
		if (data == null) {
			flash("error", "Missing file!");
			return badRequest(views.html.index.render());
		}
		
		File file = data.getFile();
		if (file == null) {
			flash("error", "Missing file!");
			return badRequest(views.html.index.render());
		}
		
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		BufferedImage image;
		String base64 = null;
		try {
			image = ImageIO.read(file);
			ImageIO.write(image, "jpg", outstream);
			outstream.flush();
			base64=Base64.encodeBase64String(outstream.toByteArray());
		} catch (Exception e) {
			flash("error", "Error when reading an image: " + e.getMessage());
			Logger.error("Image read error", e);
			return badRequest(views.html.index.render());
		} finally {
			outstream.close();
		}
		
		byte[] imgbytes = Base64.decodeBase64(base64);
		Photo photo = new Photo(title, imgbytes, new Date().getTime());
		boolean success = false;
		try {
			success = getStorage().store(photo);
		} catch (IOException e) {
			flash("error", e.getMessage());
			Logger.error("", e);
		}
		
		if (!success) flash("error", "Image already exists");
		else flash("success", "Image uploaded");
		
		return index();
    }
    
    /**
     * View a photo with its comments
     * @param imageHash
     * @param date
     * @return
     */
    public static Result viewPhoto(String imageHash, Long date) {
    	byte[] key = getStorage().getPhotoKey(imageHash, date);
    	Photo p;
		try {
			p = storage.getPhoto(key);
		} catch (IOException e) {
			flash("error", "Error when reading photo: " + e.getMessage());
			Logger.error("", e);
			return badRequest(views.html.index.render());
		}
    	return ok(views.html.viewPhoto.render(p));
    }
    
    /**
     * Store a comment with a parent specified by the given args
     * @param imageHash
     * @param photoDate
     * @return
     */
    public static Result addComment(String imageHash, Long photoDate) {
    	byte[] photoKey = getStorage().getPhotoKey(imageHash, photoDate);
    	Photo p;
		try {
			p = storage.getPhoto(photoKey);
		} catch (IOException e) {
			flash("error", "Error when reading photo: " + e.getMessage());
			Logger.error("", e);
			return badRequest(views.html.index.render());
		}
		
    	DynamicForm form = Form.form().bindFromRequest();
    	String body = form.get("comment_body");
    	Date cdate = new Date();
    	Comment c = new Comment(body, p, cdate.getTime());
    	try {
			getStorage().store(c);
		} catch (IOException e) {
			flash("error", e.getMessage());
			Logger.error("", e);
		}
    	
    	flash("success", "Added comment");
		
		return viewPhoto(imageHash, photoDate);
    }   
}
