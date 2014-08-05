/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.idpcommon;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * As we are not in the servlet environment, we need a thin Freemarker wrapper.
 * Useful for extending with injectable components fixing the template path.
 * @author K. Benedyczak
 */
public class FreemarkerHandlerBase
{
	private Configuration cfg;
	
	public FreemarkerHandlerBase(Class<?> clazzForTemplates, String pathPrefix)
	{
		cfg = new Configuration();
		cfg.setClassForTemplateLoading(clazzForTemplates, pathPrefix);
		cfg.setObjectWrapper(new DefaultObjectWrapper());
	}
	
	public void process(String view, Map<String, String> datamodel, Writer out) 
			throws IOException
	{
		Template temp = cfg.getTemplate(view);
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
