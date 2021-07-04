/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui;

import java.io.StringWriter;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import pl.edu.icm.unity.base.utils.Log;

public class FreemarkerStringTemplateProcesor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, FreemarkerStringTemplateProcesor.class);

	public static String process(Map<String, String> input, String templateStr)
	{
		StringTemplateLoader stringLoader = new StringTemplateLoader();
		String template = "content";
		stringLoader.putTemplate(template, templateStr);
		Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
		cfg.setTemplateLoader(stringLoader);
		try
		{
			Template templateCon = cfg.getTemplate(template);
			StringWriter writer = new StringWriter();
			templateCon.process(input, writer);
			return writer.toString();
		} catch (Exception e)
		{
			log.error("Can not process freemarker template from string {0}", templateStr);
		}
		return "";
	}
}
