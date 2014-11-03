/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;

/**
 * <b>IMPORTANT!</b> Use this class ONLY to show data which is at least partially trusted. 
 * For instance it is fine to use this class to display text entered by a privileged user, but shouldn't be used
 * to present text entered by an ordinary or unauthenticated user (as when using a registration form).
 * <p>
 * HTML label displaying a contents which can include several simple HTML tags. It is allowed to 
 * use: &lt;br&gt;, &lt;b&gt;, &lt;i&gt;, &lt;h1&gt;, &lt;h2&gt;, &lt;h3&gt;, &lt;small&gt;
 * &lt;hr&gt; &lt;code&gt; tags and their corresponding closing tags (where appropriate). 
 * The tags must be entered literally,
 * no additional spaces nor tag arguments are allowed. However upper case version are also permitted.
 * All other suspicious characters escaped.   
 *   
 * @author K. Benedyczak
 */
public class HtmlSimplifiedLabel extends Label
{
	public static final Set<String> ALLOWED;
	public static final Map<Character, String> ESCAPES;
	private static int MAX_TOKEN_LEN;
	
	static
	{
		Set<String> vals = new HashSet<>();
		addTag(vals, "b");
		addTag(vals, "i");
		addTag(vals, "h1");
		addTag(vals, "h2");
		addTag(vals, "h3");
		addTag(vals, "small");
		addTag(vals, "code");

		addTagS(vals, "br");
		addTagS(vals, "hr");
		ALLOWED = Collections.unmodifiableSet(vals);
		
		Map<Character, String> escapeV = new HashMap<Character, String>();
		escapeV.put('"', "&quot;");
		escapeV.put('\'', "&#39;");
		escapeV.put('&', "&amp;");
		escapeV.put('<', "&lt;");
		escapeV.put('>', "&gt;");
		ESCAPES = Collections.unmodifiableMap(escapeV);
	}

	private static void addTag(Set<String> whereto, String tag)
	{
		Collections.addAll(whereto, "<"+ tag + ">", "<"+ tag.toUpperCase() + ">", 
				"</"+ tag + ">", "</"+ tag.toUpperCase() + ">");
		if (tag.length() + 3 > MAX_TOKEN_LEN)
			MAX_TOKEN_LEN = tag.length() + 3;
	}

	private static void addTagS(Set<String> whereto, String tag)
	{
		Collections.addAll(whereto, "<"+ tag + ">", "<"+ tag.toUpperCase() + ">");
		if (tag.length() + 2 > MAX_TOKEN_LEN)
			MAX_TOKEN_LEN = tag.length() + 2;
	}
	
	public HtmlSimplifiedLabel()
	{
		setContentMode(ContentMode.HTML);
	}

	public HtmlSimplifiedLabel(String value)
	{
		this();
		setValue(value);
	}
	
	@Override
	public final void setValue(String value)
	{
		super.setValue(escape(value));
	}
	
	private String escape(String semiSafeHtml)
	{
		StringBuilder escaped = new StringBuilder();
		char[] input = semiSafeHtml.toCharArray();
		for (int i=0; i<input.length; i++)
		{
			if (ESCAPES.containsKey(input[i]))
			{
				int readAheadMax = Math.min(MAX_TOKEN_LEN, input.length - i);
				Set<String> aheadTokens = new HashSet<>();
				for (int j=3; j<=readAheadMax; j++)
				{
					aheadTokens.add(new String(input, i, j));
				}
				
				SetView<String> intersection = Sets.intersection(ALLOWED, aheadTokens);
				if (intersection.size() == 1)
				{
					String allowedToken = intersection.iterator().next();
					escaped.append(allowedToken);
					i += allowedToken.length() - 1;
				} else
				{
					escaped.append(ESCAPES.get(input[i]));
				}
			} else
			{
				escaped.append(input[i]);
			}
		}
		
		return escaped.toString();
	}
}
