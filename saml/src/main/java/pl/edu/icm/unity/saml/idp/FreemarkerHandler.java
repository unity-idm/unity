/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.springframework.stereotype.Component;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * As we are not in the servlet environment, we need a thin Freemarker wrapper.
 * @author K. Benedyczak
 */
@Component
public class FreemarkerHandler
{
	private Configuration cfg;
	
	public FreemarkerHandler()
	{
		cfg = new Configuration();
		cfg.setClassForTemplateLoading(FreemarkerHandler.class, "/pl/edu/icm/unity/samlidp/freemarker");
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
