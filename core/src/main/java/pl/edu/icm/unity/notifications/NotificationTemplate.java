/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.notifications;

import java.util.Locale;
import java.util.Map;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * Wraps notification message template. It consist of text, subject may support localization.
 * What is most important it handles parameter substitution.
 * <p>
 * Implementation note: this will be extended in future: based on freemarker, message metadata (as use HTML) 
 * added etc. For now is simplistic.
 * <p>
 * The syntax:
 * all '${foo}' expressions are replaced by the value of the foo key.
 * 
 * @author K. Benedyczak
 */
public class NotificationTemplate
{
	private Map<Locale, String> bodyTemplatesByLocale;
	private Map<Locale, String> subjectTemplatesByLocale;
	private Locale defaultLocale;

	public NotificationTemplate(Map<Locale, String> bodyTemplatesByLocale,
			Map<Locale, String> subjectTemplatesByLocale, Locale defaultLocale)
	{
		this.bodyTemplatesByLocale = bodyTemplatesByLocale;
		this.subjectTemplatesByLocale = subjectTemplatesByLocale;
		this.defaultLocale = defaultLocale;
	}

	public String getBody(Map<String, String> params)
	{
		return getMsg(bodyTemplatesByLocale, params);
	}

	public String getSubject(Map<String, String> params)
	{
		return getMsg(subjectTemplatesByLocale, params);
	}
	
	private String getMsg(Map<Locale, String> templatesByLocale, Map<String, String> params)
	{
		Locale loc = UnityMessageSource.getLocale(defaultLocale);
		String template = templatesByLocale.get(loc);
		for (Map.Entry<String, String> paramE: params.entrySet())
			template = template.replace("${"+paramE.getKey()+"}", paramE.getValue());
		return template;
	}
}
