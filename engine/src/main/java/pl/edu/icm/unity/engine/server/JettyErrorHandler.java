/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ErrorHandler;

import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.FreemarkerUtils;

/**
 * Custom error handler implementation for Jetty. 
 * Delegates to freemarker template which can be customized by users.
 * 
 * @author K. Benedyczak
 */
class JettyErrorHandler extends ErrorHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, JettyErrorHandler.class);
	private static final String ERROR_TPL = "error-generic.ftl";
	private Configuration cfg;

	JettyErrorHandler(String webContentsDirectory)
	{
		cfg = new Configuration(Configuration.VERSION_2_3_21);
		
		cfg.setTemplateLoader(FreemarkerUtils.getTemplateLoader(webContentsDirectory, 
				FreemarkerUtils.TEMPLATES_ROOT, getClass()));
		BeansWrapperBuilder builder = new BeansWrapperBuilder(Configuration.VERSION_2_3_21);
		cfg.setObjectWrapper(builder.build());
	}
	
	
	@Override
	protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, 
			String message, boolean showStacks) throws IOException
	{
		if (message == null)
			message=HttpStatus.getMessage(code);
		
		if (code == HttpStatus.NOT_FOUND_404)
		{
			log.debug("404 error: {}", message);
		} else
		{
			Throwable th = (Throwable)request.getAttribute(
					RequestDispatcher.ERROR_EXCEPTION);
			if (th != null)
				log.warn("Got HTTP error " + code + ": " + message, th);
			else
				log.warn("Got HTTP error {}: {}", code, message);
		}
		
		FreemarkerUtils.processTemplate(cfg, ERROR_TPL, createContext(code, message), writer);
	}


	private Map<String, String> createContext(int code, String message)
	{
		Map<String, String> context = new HashMap<>();
		context.put("error", message);
		context.put("errorCode", String.valueOf(code));
		return context;
	}
}
