/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.webui.idpcommon.EopException;

/**
 * Allows to show a plain error page, if error can not be returned to the requester. 
 * 
 * @author K. Benedyczak
 */
public class ErrorHandler
{
	private Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ErrorHandler.class);
	private FreemarkerAppHandler freemarker;
	
	public ErrorHandler(FreemarkerAppHandler freemarker)
	{
		this.freemarker = freemarker;
	}

	public void showErrorPage(String error, String errorReason, HttpServletResponse response) 
			throws IOException, EopException
	{
		log.debug("OAuth error is going to be shown to the user redirected to Unity by the " +
				"OAuth client: " + error + " " + (errorReason == null ? "" : errorReason));
		response.setContentType("text/html; charset=utf-8");
		PrintWriter w = response.getWriter();
		freemarker.printAppErrorPage(w, "OAuth", "Authorization Server got an invalid request.", error, errorReason);
		throw new EopException();
	}
}
