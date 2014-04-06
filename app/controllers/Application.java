package controllers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

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

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

public class Application extends Controller {
  
    public static Result index() {
        return ok(views.html.index.render());
    }

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
			base64=Base64.encode(outstream.toByteArray());
		} catch (Exception e) {
			flash("error", "Error when reading an image: " + e.getMessage());
			Logger.error("Image read error", e);
			return badRequest(views.html.index.render());
		} finally {
			outstream.close();
		}
		
		byte[] imgbytes = Base64.decode(base64);
		Photo photo = new Photo(title, imgbytes, new Date());
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
    
    private static IStorage storage;
    public static IStorage getStorage() {
    	if (storage != null) return storage;
    	return new HBaseStorage();
    }
}
