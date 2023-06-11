/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.policyAgreement;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;

public class PolicyAgreementConfigTextParser
{	
	private static Logger log = Log.getLogger(Log.U_SERVER_CORE, PolicyAgreementConfigTextParser.class);
	
	public static String DOC_PLACEHOLDER_PATTER = "\\{([^\\}]*)\\}";
	
	public static Map<Long, DocPlaceholder> getAllDocsPlaceholdersInConfigText(I18nString text) throws Exception
	{
		Map<Long, DocPlaceholder> ret = new HashMap<>();
		Pattern r = Pattern.compile(DOC_PLACEHOLDER_PATTER);

		for (String v : text.getMap().values())
		{
			Matcher m = r.matcher(v);
			while (m.find())
			{

				String exp = m.group(0);
				String withoutCurly = exp.substring(1, exp.length() - 1);
				String[] split = withoutCurly.split(":");
				Long id = Long.valueOf(split[0]);
				ret.put(id, new DocPlaceholder(id, split[1]));
			}
		}
		return ret;
	}

	public static Map<Long, DocPlaceholder> getAllDocsPlaceholdersInConfigText(String text) 
	{
		Map<Long, DocPlaceholder> ret = new HashMap<>();
		String pattern = "\\{([^\\}]*)\\}";
		Pattern r = Pattern.compile(pattern);
		if (text == null)
			return ret;
		Matcher m = r.matcher(text);
		while (m.find())
		{
			String exp = m.group(0);
			String withoutCurly = exp.substring(1, exp.length() - 1);
			String[] split = withoutCurly.split(":");
			try
			{
				Long id = Long.valueOf(split[0]);
				ret.put(id, new DocPlaceholder(id, split[1]));
			} catch (NumberFormatException e)
			{
				log.debug("Invalid document placeholder in text " + text);
				continue;
			}
		}
		return ret;
	}

	public static I18nString convertTextToConfig(Collection<PolicyDocumentWithRevision> policyDocuments,
			I18nString fromPresentation)
	{
		if (fromPresentation == null)
			return null;

		I18nString ret = fromPresentation.clone();
		for (PolicyDocumentWithRevision doc : policyDocuments)
		{
			ret.replace("{" + doc.name + ":", "{" + doc.id + ":");
		}
		return ret;

	}

	public static I18nString convertTextToPresentation(Collection<PolicyDocumentWithRevision> policyDocuments,
			I18nString fromConfig)
	{
		if (fromConfig == null)
			return null;
		I18nString ret = fromConfig.clone();

		for (PolicyDocumentWithRevision doc : policyDocuments)
		{
			ret.replace("{" + doc.id + ":", "{" + doc.name + ":");

		}
		return ret;
	}

	public static class DocPlaceholder
	{
		public final Long docId;
		public final String displayedText;

		public DocPlaceholder(Long docId, String displayedText)
		{
			this.docId = docId;
			this.displayedText = displayedText;
		}

		@Override
		public String toString()
		{

			return "{" + docId + ":" + displayedText + "}";
		}
		
		public String toPatternString()
		{

			return "\\{" + docId + "\\:" + displayedText + "\\}";
		}


		public static DocPlaceholder fromString(String from)
		{
			String withoutCurly = from.substring(1, from.length() - 1);
			String[] split = withoutCurly.split(":");
			if (split.length == 2)
			{
				try
				{
					return new DocPlaceholder(Long.valueOf(split[0]), split[1]);
				} catch (NumberFormatException e)
				{
					//ok
				}
			}
			return null;

		}
	}
}
