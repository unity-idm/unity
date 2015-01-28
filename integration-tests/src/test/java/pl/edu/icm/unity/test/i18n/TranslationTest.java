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

import pl.edu.icm.unity.server.utils.UnityMessageSource;


public class TranslationTest
{
	public static final MessageBundleSpec[] SPECS = new MessageBundleSpec[]	{
		new MessageBundleSpec("core", "/pl/edu/icm/unity/server/core/messages/messages", "pl"),
		new MessageBundleSpec("std-extensions", "/pl/edu/icm/unity/stdext/messages/messages", "pl"),
		new MessageBundleSpec("web-common", "/pl/edu/icm/unity/webui/messages/messages", "pl"),
		new MessageBundleSpec("web-admin/home", "/pl/edu/icm/unity/webadmin/messages/home-messages", "pl"),
		new MessageBundleSpec("saml", "/pl/edu/icm/unity/samlidp/messages/messages", "pl"),
		new MessageBundleSpec("unicore", "/pl/edu/icm/unity/unicore/messages/messages", "pl"),
		new MessageBundleSpec("oauth", "/pl/edu/icm/unity/oauth/messages/messages", "pl"),
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
		new DuplicateException("SPInfoComponent.requesterName", "pl")
	);
	
	private int notTranslated = 0;
	private int duplicated = 0;
	private int all = 0;
	
	@Test
	public void testTranslationCoverage()
	{
		for (MessageBundleSpec spec: SPECS)
		{
			try
			{
				testBundle(spec);
			} catch (IOException e)
			{
				System.err.println("Can not load " + spec.getName() + ", skipping: " 
						+ e.toString());
			}
		}
		System.out.println("=======================================================================================================");
		int invalid = notTranslated + duplicated;
		System.out.println("Not translated: " + notTranslated + 
				" \tDuplicate: " + duplicated + 
				" \tTotal invalid: " + invalid + 
				" \tTotal: " + all +
				" \tCoverage: " + (1-(float)invalid/all));
	}
	
	private void testBundle(MessageBundleSpec spec) throws IOException
	{
		Properties base = load(spec.getBaseLocation(), "");
		boolean header = false;
		for (String locale: spec.getEnabledLocales())
		{
			Properties translated = load(spec.getBaseLocation(), locale);
			TreeSet<String> missing = new TreeSet<>();
			TreeSet<String> theSame = new TreeSet<>();
			for (Map.Entry<Object, Object> entry: base.entrySet())
			{
				String key = (String) entry.getKey();
				String baseMsg = (String) entry.getValue();
				if (!translated.containsKey(key))
					missing.add(key);
				else if (translated.get(key).equals(baseMsg) && baseMsg.length() > 2)
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
			notTranslated += missing.size();
			duplicated += theSame.size();
			all+=translated.size();
		}
	}
	
	private void printHeader(String name)
	{
		System.out.println("-------------------------------------------------------");
		System.out.println("Module " + name);
	}
	
	private void report(Set<String> missing, String info, String locale, MessageBundleSpec spec)
	{
		System.out.println(spec.getName() + ": " + info + " translations in " + locale);
		for (Object key: missing)
			System.out.println("  " + key);
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
}
