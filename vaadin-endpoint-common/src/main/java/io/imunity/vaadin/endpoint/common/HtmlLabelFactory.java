/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.google.common.html.HtmlEscapers;
import com.vaadin.flow.component.Html;
import pl.edu.icm.unity.base.message.MessageSource;

public class HtmlLabelFactory
{
	public static Html getHtmlLabel(MessageSource msg, String name, String msgKey, Object... unsafeArgs)
	{
		Object[] escapedArgs = escapeArgs(unsafeArgs);
		Html html = new Html(msg.getMessageNullArg(msgKey, escapedArgs));
		html.getElement().setProperty("label", name);
		return html;
	}

	private static Object[] escapeArgs(Object... unsafeArgs)
	{
		Object[] escapedArgs = new Object[unsafeArgs.length];
		for (int i=0; i<unsafeArgs.length; i++)
			escapedArgs[i] = unsafeArgs[i] instanceof String ?
					HtmlEscapers.htmlEscaper().escape((String)unsafeArgs[i]) :
					unsafeArgs[i];
		return escapedArgs;
	}
}
