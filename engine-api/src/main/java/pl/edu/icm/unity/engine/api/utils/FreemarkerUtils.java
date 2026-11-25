/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Helper with shared routings for bootstraping template engine.
 * @author K. Benedyczak
 */
public class FreemarkerUtils
{
	public static final String TEMPLATES_ROOT = "/templates";
	private static final String STRING_TEMPLATE_NAME = "templateName";
	
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
			log.warn("Templates directory {} can not be read. Will use the default bundled templates only.", webContents);
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
	
	public static Configuration createConfiguration(Version incompatibleImprovements)
	{
		Configuration cfg = new Configuration(incompatibleImprovements);
		cfg.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
		cfg.setAPIBuiltinEnabled(false);
		return cfg;
	}

	public static String processStringTemplate(Map<String, Object> datamodel, String templateStr)
	{
		if (templateStr == null)
		{
			return null;
		}
		try
		{
			Template template = parseStringTemplate(templateStr);
			StringWriter writer = new StringWriter();
			template.process(datamodel, writer);
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
		try
		{
			parseStringTemplate(templateStr);
		} catch (IOException e)
		{
			return false;
		}
		return true;
	}

	private static Template parseStringTemplate(String templateStr) throws IOException
	{
		Configuration cfg = createConfiguration(Configuration.VERSION_2_3_30);
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
		cfg.setLogTemplateExceptions(false);
		return new Template(STRING_TEMPLATE_NAME, new StringReader(templateStr), cfg);
	}
}
