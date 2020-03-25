/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.policyAgreement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.types.I18nString;

public class PolicyAgreementConfigTextParser
{
	public static List<Long> searchAllDocIdsInConfigText(I18nString text) throws Exception
	{
		List<Long> ret = new ArrayList<>();
		String pattern = "\\{([^\\}]*)\\}";
		Pattern r = Pattern.compile(pattern);

		for (String v : text.getMap().values())
		{
			Matcher m = r.matcher(v);
			while (m.find())
			{
				ret.add(parseSingle(m.group(0)));
			}
		}
		return ret;
	}

	private static Long parseSingle(String exp) throws Exception
	{
		String withoutCurly = exp.substring(1, exp.length() - 1);
		return Long.valueOf(withoutCurly.split(":")[0]);
	}
	
	public static I18nString convertTextToConfig(Collection<PolicyDocumentWithRevision> policyDocuments, I18nString fromPresentation)
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

	public static I18nString convertTextToPresentation(Collection<PolicyDocumentWithRevision> policyDocuments, I18nString fromConfig)
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

}
