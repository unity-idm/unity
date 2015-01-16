/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.safehtml;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.google.gwt.thirdparty.guava.common.html.HtmlEscapers;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;

/**
 * HTML label displaying a contents which is based on a safe template with HTML and unsafe arguments which are supposed
 * not to have HTML and are therefore escaped. This class aim is to protect programmer from silly mistakes allowing
 * for XSS attacks as a consequence. Its API probably can be abused but if used in the most convenient way it prevents 
 * from exposing vulnerable HTML labels. 
 * 
 *   
 * @author K. Benedyczak
 */
public class HtmlLabel extends Label
{
	private UnityMessageSource msg;
	
	public HtmlLabel(UnityMessageSource msg)
	{
		this.msg = msg;
		super.setContentMode(ContentMode.HTML);
		resetValue();
	}

	public HtmlLabel(UnityMessageSource msg, String msgKey, Object... unsafeArgs)
	{
		this(msg);
		setHtmlValue(msgKey, unsafeArgs);
	}
	
	public final void setContentMode(ContentMode cm)
	{
		if (getContentMode() != ContentMode.TEXT)
			throw new IllegalArgumentException("This method is forbidden in HtmlLabel");
	}
	
	@Override
	public final void setValue(String value)
	{
		if (getContentMode() != ContentMode.TEXT)
			throw new IllegalArgumentException("This method is forbidden in HtmlLabel");		
	}
	
	/**
 	 * Sets the message with the given key and optional args. 
	 * The args can be unsafe, the bundle message can contain HTML formatting.
	 * @param msgKey message bundle key
	 * @param unsafeArgs array with unsafe args
	 */
	public void setHtmlValue(String msgKey, Object... unsafeArgs)
	{
		Object[] escapedArgs = escapeArgs(unsafeArgs);
		super.setValue(msg.getMessageNullArg(msgKey, escapedArgs));
	}
	
	/**
	 * Adds the message with the given key and optional args after the end of the current value in a new line. 
	 * The args can be unsafe, the bundle message can contain HTML formatting.
	 * @param msgKey message bundle key
	 * @param unsafeArgs array with unsafe args
	 */
	public void addHtmlValueLine(String msgKey, Object... unsafeArgs)
	{
		Object[] escapedArgs = escapeArgs(unsafeArgs);
		StringBuilder current = new StringBuilder(getValue());
		if (current.length() > 0)
			current.append("<br>");
		current.append(msg.getMessageNullArg(msgKey, escapedArgs));
		super.setValue(current.toString());
	}

	/**
	 * Adds the message with the given key and optional args after the end of the current value. 
	 * The args can be unsafe, the bundle message can contain HTML formatting.
	 * @param msgKey message bundle key
	 * @param unsafeArgs array with unsafe args
	 */
	public void addHtmlValue(String msgKey, Object... unsafeArgs)
	{
		Object[] escapedArgs = escapeArgs(unsafeArgs);
		StringBuilder current = new StringBuilder(getValue());
		current.append(" ");
		current.append(msg.getMessageNullArg(msgKey, escapedArgs));
		super.setValue(current.toString());
	}
	
	private Object[] escapeArgs(Object... unsafeArgs)
	{
		Object[] escapedArgs = new Object[unsafeArgs.length];
		for (int i=0; i<unsafeArgs.length; i++)
			escapedArgs[i] = unsafeArgs[i] instanceof String ? 
					HtmlEscapers.htmlEscaper().escape((String)unsafeArgs[i]) :
					unsafeArgs[i];
		return escapedArgs;
	}
	
	public void resetValue()
	{
		super.setValue("");
	}
}
