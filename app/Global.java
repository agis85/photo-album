import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Http.RequestHeader;

/**
 * 
 * Play Application global settings
 * @author Agis Chartsias
 */
public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		Logger.info("Application starting...");
	}
	
	@Override
	public void onStop(Application app) {
		Logger.info("Application shutdown...");
	}
	
	@Override
	public Result onError(RequestHeader req, Throwable t) {
		String stacktrace = ExceptionUtils.getStackTrace(t);
		String code = RandomStringUtils.randomAlphanumeric(6);
		Logger.error("Exception: " + code, t);
		return Results.status(500, views.html.error.render(code, stacktrace));
	}
}
