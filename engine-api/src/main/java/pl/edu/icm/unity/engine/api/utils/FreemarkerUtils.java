/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Helper with shared routings for bootstraping template engine.
 * @author K. Benedyczak
 */
public class FreemarkerUtils
{
	public static final String TEMPLATES_ROOT = "/templates";
	
	private static final Logger log = Log.getLogger(Log.U_SERVER, FreemarkerUtils.class);
	
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
}
