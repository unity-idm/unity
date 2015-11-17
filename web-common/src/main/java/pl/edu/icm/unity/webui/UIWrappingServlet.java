/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.shared.Version;

import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Servlet using Freemarker based view. The view is populated with data required to 
 * properly include Vaadin UI from the Freemarker template.
 * <p>
 * The following variables are set in the Freemarker context:
 * <ul>
 * <li> theme
 * <li> vaadinVersion
 * <li> comErrMsgCaption, comErrMsg
 * <li> authErrMsgCaption, authErrMsg
 * <li> sessExpMsgCaption, sessExpMsg
 * <li> debug
 * <li> heartbeat
 * </ul>
 * @author K. Benedyczak
 */
public class UIWrappingServlet extends HttpServlet
{
	public static final String TEMPLATES_ROOT = "/templates";
	private final Configuration cfg;
	private final String mainTemplate;
	private final UnityMessageSource msg;
	private String theme;
	private boolean debug;
	private long heartbeat;
	
	public UIWrappingServlet(String mainTemplate, UnityMessageSource msg, String theme,
			boolean debug, long heartbeat)
	{
		this.mainTemplate = mainTemplate;
		this.msg = msg;
		this.theme = theme;
		this.debug = debug;
		this.heartbeat = heartbeat;
		cfg = new Configuration(Configuration.VERSION_2_3_21);
		cfg.setClassForTemplateLoading(getClass(), TEMPLATES_ROOT);
		BeansWrapperBuilder builder = new BeansWrapperBuilder(Configuration.VERSION_2_3_21);
		cfg.setObjectWrapper(builder.build());
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		resp.setContentType("text/html; charset=utf-8");
		process(mainTemplate, createContext(), resp.getWriter());
	}
	
	private Map<String, String> createContext()
	{
		Map<String, String> data = new HashMap<String, String>();
		data.put("theme", theme);
		data.put("vaadinVersion", Version.getFullVersion());
		data.put("debug", String.valueOf(debug));
		data.put("heartbeat", String.valueOf(heartbeat));
		data.put("comErrMsgCaption", msg.getMessage("UIWrappingServlet.comErrMsgCaption"));
		data.put("comErrMsg", msg.getMessage("UIWrappingServlet.comErrMsg"));
		data.put("authErrMsgCaption", msg.getMessage("UIWrappingServlet.authErrMsgCaption"));
		data.put("authErrMsg", msg.getMessage("UIWrappingServlet.authErrMsg"));
		data.put("sessExpMsgCaption", msg.getMessage("UIWrappingServlet.sessExpMsgCaption"));
		data.put("sessExpMsg", msg.getMessage("UIWrappingServlet.sessExpMsg"));
		return data;
	}
	
	private void process(String view, Map<String, String> datamodel, Writer out) 
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
