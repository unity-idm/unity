/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Allows to show a plain error page, if error can not be returned to the requester. 
 * 
 * @author K. Benedyczak
 */
public class ErrorHandler
{
	private Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ErrorHandler.class);
	private FreemarkerHandler freemarker;
	
	public ErrorHandler(FreemarkerHandler freemarker)
	{
		this.freemarker = freemarker;
	}

	public void showErrorPage(String error, String errorReason, HttpServletResponse response) 
			throws IOException, EopException
	{
		log.debug("OAuth error is going to be shown to the user redirected to Unity by the " +
				"OAuth client: " + error + " " + (errorReason == null ? "" : errorReason));
		response.setContentType("application/xhtml+xml; charset=utf-8");
		PrintWriter w = response.getWriter();
		Map<String, String> data = new HashMap<String, String>();
		data.put("error", error);
		if (errorReason != null)
			data.put("errorCause", errorReason);
		freemarker.process("finishError.ftl", data, w);
		throw new EopException();
	}

	public void showHoldOnPage(String requestUri, HttpServletResponse response) 
			throws IOException, EopException
	{
		response.setContentType("application/xhtml+xml; charset=utf-8");
		PrintWriter w = response.getWriter();
		Map<String, String> data = new HashMap<String, String>();
		String originalUri = Base64.encodeBase64URLSafeString(requestUri.getBytes());
		data.put("originalRequest", originalUri);
		freemarker.process("holdonError.ftl", data, w);
		throw new EopException();
	}
}
