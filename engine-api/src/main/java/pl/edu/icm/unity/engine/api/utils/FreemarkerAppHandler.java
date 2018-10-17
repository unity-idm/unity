/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Used for cases where plain web page should be presented to the user, based on freemarker template.
 * 
 * @author K. Benedyczak
 */
@Component
public class FreemarkerAppHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			FreemarkerAppHandler.class);
	private static final String ERROR_TPL = "error-app.ftl";
	private Configuration cfg;
	
	@Autowired
	public FreemarkerAppHandler(UnityServerConfiguration serverConfig)
	{
		cfg = new Configuration(Configuration.VERSION_2_3_21);
		String webRoot = serverConfig.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH);
		cfg.setTemplateLoader(FreemarkerUtils.getTemplateLoader(webRoot, 
				FreemarkerUtils.TEMPLATES_ROOT, getClass()));
		BeansWrapperBuilder builder = new BeansWrapperBuilder(Configuration.VERSION_2_3_21);
		cfg.setObjectWrapper(builder.build());
	}
	
	public void printAppErrorPage(Writer out, String subsystem, 
			String genericIntro, String error, String cause) 
			throws IOException
	{
		log.info("Presenting error page with error {}: {}", error, cause);
		
		Map<String, String> datamodel = new HashMap<>();
		datamodel.put("subsystem", subsystem);
		datamodel.put("generalInfo", genericIntro);
		datamodel.put("error", error);
		datamodel.put("errorCause", cause);
		FreemarkerUtils.processTemplate(cfg, ERROR_TPL, datamodel, out);
	}

	public void printGenericPage(Writer out, String template, Map<String, String> datamodel) 
			throws IOException
	{
		FreemarkerUtils.processTemplate(cfg, template, datamodel, out);
	}
}
