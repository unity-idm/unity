/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.safe_html;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import java.util.*;

/**
 * HTML escpers commonly used in front-end to parse user input before
 * displaying.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public final class HtmlEscapers
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
		
		Map<Character, String> escapeV = new HashMap<>();
		escapeV.put('"', "&quot;");
		escapeV.put('\'', "&#39;");
		escapeV.put('&', "&amp;");
		escapeV.put('<', "&lt;");
		escapeV.put('>', "&gt;");
		ESCAPES = Collections.unmodifiableMap(escapeV);
	}
	
	public static String escape(String html)
	{
		return com.google.common.html.HtmlEscapers.htmlEscaper().escape(html);
	}

	/**
	 * Escape the HTML string including several simple HTML tags. It is allowed
	 * to use: &lt;br&gt;, &lt;b&gt;, &lt;i&gt;, &lt;h1&gt;, &lt;h2&gt;,
	 * &lt;h3&gt;, &lt;small&gt; &lt;hr&gt; &lt;code&gt; tags and their
	 * corresponding closing tags (where appropriate). The tags must be entered
	 * literally, no additional spaces nor tag arguments are allowed. However
	 * upper case version are also permitted. 
	 * All other suspicious characters escaped.
	 */
	public static String simpleEscape(String semiSafeHtml)
	{
		StringBuilder escaped = new StringBuilder();
		if (semiSafeHtml == null)
			return "";
		char[] input = semiSafeHtml.toCharArray();
		for (int i=0; i<input.length; i++)
		{
			if (ESCAPES.containsKey(Character.valueOf(input[i])))
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
	
	private HtmlEscapers() {}
}
