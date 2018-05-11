/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;


public class TranslationTest
{
	private static final String[] LOCALES = {"pl", "de"};
	
	public static final MessageBundleSpec[] SPECS = new MessageBundleSpec[]	{
		new MessageBundleSpec("server-engine-api", "/messages/engine-api/messages"),
		new MessageBundleSpec("server-std-plugins", "/messages/stdext/messages"),
		new MessageBundleSpec("server-web-common", "/messages/webui/messages"),
		new MessageBundleSpec("server-web-admin/home", "/messages/webhome/messages"),
		new MessageBundleSpec("server-saml", "/messages/saml/messages"),
		new MessageBundleSpec("server-unicore", "/messages/unicore/messages"),
		new MessageBundleSpec("serveroauth", "/messages/oauth/messages"),
	}; 
	
	public static final Set<DuplicateException> EXCEPTIONS = Sets.newHashSet(
		new DuplicateException("SamlUnicoreIdPWebUI.preferences", "pl"),
		new DuplicateException("NumericAttributeHandler.min", "pl"),
		new DuplicateException("NumericAttributeHandler.minE", "pl"),
		new DuplicateException("RegistrationRequest.status", "pl"),
		new DuplicateException("IdentityDetails.credStatusValue", "pl"),
		new DuplicateException("IdentityDetails.groupLine", "pl"),
		new DuplicateException("IdentityDetails.identityLocal", "pl"),
		new DuplicateException("IdentityDetails.status", "pl"),
		new DuplicateException("SPInfoComponent.requesterName", "pl"),
		new DuplicateException("IdentityFormatter.identityCore", "pl")
	);
	
	public static final Set<String> IGNORED = Sets.newHashSet(
			"MessageUsedForIntegrationTesting.only0",
			"MessageUsedForIntegrationTesting.only1"
			);

	@Test
	public void testTranslationCoverage()
	{
		for (String locale: LOCALES)
			testLocale(locale);
	}
	
	private void testLocale(String locale)
	{
		System.out.println("\n== " + locale.toUpperCase() + " ===================================================================================================");
		Result result = new Result();
		for (MessageBundleSpec spec: SPECS)
		{
			try
			{
				testBundle(spec, locale, result);
			} catch (IOException e)
			{
				System.err.println("Can not load " + spec.getName() + ", skipping: " 
						+ e.toString());
			}
		}
		System.out.println("=======================================================================================================");
		int invalid = result.notTranslated + result.duplicated;
		System.out.println(locale.toUpperCase() + ": not translated: " + result.notTranslated + 
				" \tDuplicate: " + result.duplicated + 
				" \tTotal invalid: " + invalid + 
				" \tTotal: " + result.all +
				" \tCoverage: " + (1-(float)invalid/result.all));
	}
	
	private void testBundle(MessageBundleSpec spec, String locale, Result result) throws IOException
	{
		Properties base = load(spec.getBaseLocation(), "");
		boolean header = false;
		Properties translated = load(spec.getBaseLocation(), locale);
		TreeSet<String> missing = new TreeSet<>();
		TreeSet<String> theSame = new TreeSet<>();
		for (Map.Entry<Object, Object> entry: base.entrySet())
		{
			String key = (String) entry.getKey();
			String baseMsg = (String) entry.getValue();
			if (IGNORED.contains(key))
				continue;
			
			if (!translated.containsKey(key))
			{
				missing.add(key);
			} else if (translated.get(key).equals(baseMsg) && baseMsg.length() > 2)
			{
				if (!EXCEPTIONS.contains(new DuplicateException(key, locale)))
					theSame.add(key);
			}
		}
		if (!header && (!missing.isEmpty() || !theSame.isEmpty()))
		{
			printHeader(spec.getName());
			header = true;
		}

		if (!missing.isEmpty())
			report(missing, "missing", locale, spec);
		if (!theSame.isEmpty())
			report(theSame, "duplicated", locale, spec);
		result.notTranslated += missing.size();
		result.duplicated += theSame.size();
		result.all+=translated.size();
	}
	
	private void printHeader(String name)
	{
		System.out.println("-------------------------------------------------------");
		System.out.println("Module " + name);
	}
	
	private void report(Set<String> missing, String info, String locale, MessageBundleSpec spec) throws IOException
	{
		Properties base = load(spec.getBaseLocation(), "");
		System.out.println("\n" + info + " translations:\n");
		for (Object key: missing)
			System.out.println(String.format("%-80s%s", key+"=", "#eng: " + base.getProperty(key.toString())));
	}
	
	private Properties load(String baseLocation, String locale) throws IOException
	{
		String locSuffix = locale.equals("") ? "" : "_" + locale;
		String resource = baseLocation + locSuffix + ".properties";
		InputStream is = UnityMessageSource.class.getResourceAsStream(resource);
		if (is == null)
			throw new IOException("Resource " + resource + " not found");
		Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
		Properties p = new Properties();
		p.load(reader);
		return p;
	}
	
	private static class Result
	{
		int notTranslated = 0;
		int duplicated = 0;
		int all = 0;	
	}
}
