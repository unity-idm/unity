/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Helper with shared routings for bootstraping template engine.
 * @author K. Benedyczak
 */
public class FreemarkerUtils
{
	public static final String TEMPLATES_ROOT = "/templates";
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, FreemarkerUtils.class);
	
	public static TemplateLoader getTemplateLoader(String webContentsDirectory, 
			String templatesRoot, Class<?> fallbackResourceLoaderClass)
	{
		ClassTemplateLoader fallbackLoader = new ClassTemplateLoader(
				fallbackResourceLoaderClass, templatesRoot);
		FileTemplateLoader primaryLoader;
		File webContents = new File(webContentsDirectory, templatesRoot);
		try
		{
			primaryLoader = new FileTemplateLoader(webContents);
		} catch (IOException e)
		{
			log.warn("Templates directory " + webContentsDirectory + 
					" can not be read. Will use the default bundled templates only.");
			return fallbackLoader;
		}
		return new MultiTemplateLoader(new TemplateLoader [] {primaryLoader, fallbackLoader});
	}
	
	public static void processTemplate(Configuration cfg, String view, 
			Map<String, String> datamodel, Writer out) throws IOException
	{
		Template temp = cfg.getTemplate(view);
		log.debug("Using template " + temp.getName());
		try
		{
			temp.process(datamodel, out);
		} catch (TemplateException e)
		{
			throw new IOException(e);
		}
		out.flush();
	}
	
	public static String processStringTemplate(Map<String, Object> datamodel, String templateStr)
	{
		if (templateStr == null)
		{
			return null;
		}
		
		StringTemplateLoader stringLoader = new StringTemplateLoader();
		String templateName = "templateName";
		stringLoader.putTemplate(templateName, templateStr);
		Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
		cfg.setTemplateLoader(stringLoader);
		try
		{
			Template templateCon = cfg.getTemplate(templateName);
			StringWriter writer = new StringWriter();
			templateCon.process(datamodel, writer);
			return writer.toString();
		} catch (Exception e)
		{
			log.error("Can not process freemarker template from string " + templateStr, e);
		}
		return templateStr;
	}
	
	public static boolean validateStringTemplate(String templateStr)
	{
		if (templateStr == null)
		{
			return true;
		}
		StringTemplateLoader stringLoader = new StringTemplateLoader();
		String templateName = "templateName";
		stringLoader.putTemplate(templateName, templateStr);
		Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
		cfg.setTemplateLoader(stringLoader);
		Template templateCon;
		try
		{
			templateCon = cfg.getTemplate(templateName);
			StringWriter writer = new StringWriter();
			templateCon.process(new HashMap<>(), writer);
		} catch (Exception e) {
			return false;
		}
		return true;
		
	}
	
}
