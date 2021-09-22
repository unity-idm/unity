/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import io.imunity.otp.HashFunction;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.ldap.client.config.LdapProperties;
import pl.edu.icm.unity.ldap.client.config.LdapProperties.BindAs;
import pl.edu.icm.unity.ldap.client.config.common.LDAPConnectionProperties;

class OTPWithLDAPProperties extends LDAPConnectionProperties
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, OTPWithLDAPProperties.class);

	public static final String LDAP_SEARCH_BASENAME = "searchBaseName";
	public static final String LDAP_SEARCH_FILTER = "searchFilter";
	public static final String LDAP_SEARCH_SCOPE = "searchScope";
	public static final String OTP_SECRET_ATTRIBUTE = "otpSecretAttribute";
	public static final String OTP_CODE_LENGHT = "otpCodeLenght";
	public static final String OTP_ALLOWED_TIME_DRIFT_STEPS = "otpAllowedTimeDriftSteps";
	public static final String OTP_HASH_FUNCTION = "otpHashFunction";
	public static final String OTP_TIME_STEP_SECODS = "otpTimeStepSeconds";

	public static final HashFunction DEFAULT_OTP_HASH_FUNCTION = HashFunction.SHA1;
	public static final int DEFAULT_OTP_CODE_LENGHT = 6;
	public static final int DEFAULT_OTP_ALLOWED_TIME_DRIFT_STEPS = 3;
	public static final int DEFAULT_OTP_TIME_STEP_SECODS = 30;

	@DocumentationReferencePrefix
	public static final String PREFIX = "otpldap.";

	public final static Map<String, PropertyMD> defaults = new HashMap<String, PropertyMD>();

	static
	{

		defaults.put(SYSTEM_DN, new PropertyMD().setMandatory().setCategory(main).setDescription(
				"The value must be the DN of the system user to authenticate as before performing any queries."));
		defaults.put(SYSTEM_PASSWORD,
				new PropertyMD().setCategory(main).setMandatory()
						.setDescription("The value must be the password of the system "
								+ "user to authenticate as before performing any queries."));
		defaults.put(VALID_USERS_FILTER,
				new PropertyMD("objectclass=*").setCategory(main)
						.setDescription("Standard LDAP filter of valid users."
								+ " Even the users who can authenticate but are not matching this filter will "
								+ "have access denied."));
		defaults.put(OTP_SECRET_ATTRIBUTE, new PropertyMD().setMandatory().setCategory(main)
				.setDescription("Name of LDAP attribute holding otp secret"));
		defaults.put(OTP_CODE_LENGHT, new PropertyMD(String.valueOf(DEFAULT_OTP_CODE_LENGHT)).setCategory(main)
				.setDescription("How long each generated code is valid. 30 seconds is the safest bet."));
		defaults.put(OTP_ALLOWED_TIME_DRIFT_STEPS,
				new PropertyMD(String.valueOf(DEFAULT_OTP_ALLOWED_TIME_DRIFT_STEPS)).setCategory(main)
						.setDescription("If larger then zero, then codes generated in that many steps behind"
								+ "or after server''s time will be accepted.  "));
		defaults.put(OTP_TIME_STEP_SECODS,
				new PropertyMD(String.valueOf(DEFAULT_OTP_TIME_STEP_SECODS)).setCategory(main)
						.setDescription("How long each generated code is valid. 30 seconds is the safest bet. "
								+ "Google and Microsoft authenticator apps only support setting of 30s"));
		defaults.put(OTP_HASH_FUNCTION, new PropertyMD(String.valueOf(DEFAULT_OTP_HASH_FUNCTION)).setCategory(main)
				.setDescription("Hash algorithm to be used. SHA1 is the most commonly supported, "
						+ "other variants are more secure. Google and Microsoft authenticator apps only support SHA1"));

		defaults.put(LDAP_SEARCH_BASENAME,
				new PropertyMD().setCategory(main)
						.setDescription("Base DN for the search.  The value can include a special"
								+ "string: '\\{USERNAME\\}'. The username provided by the client"
								+ " will be substituted."));
		defaults.put(LDAP_SEARCH_FILTER, new PropertyMD().setCategory(main)
				.setDescription("Filter in LDAP syntax, to match requested entries. The filter can include a special"
						+ "string: '\\{USERNAME\\}'. The username provided by the client" + " will be substituted."));
		defaults.put(LDAP_SEARCH_SCOPE, new PropertyMD(SearchScope.sub).setCategory(main)
				.setDescription("LDAP search scope to be used for this search."));

		defaults.put(USER_DN_TEMPLATE,
				new PropertyMD().setCategory(main).setDescription("Template of a DN of "
						+ "the user that should be used to log in. The tempalte must possess a single occurence "
						+ "of a special string: '\\{USERNAME\\}'. "));

		defaults.put(VALID_USERS_FILTER,
				new PropertyMD("objectclass=*").setCategory(main)
						.setDescription("Standard LDAP filter of valid users."
								+ " Even the users who can authenticate but are not matching this filter will "
								+ "have access denied."));

		defaults.putAll(LDAPConnectionProperties.getDefaults());

	}

	OTPWithLDAPProperties(Properties properties)
	{
		super(PREFIX, properties, defaults, log);
	}

	LdapProperties toFullLDAPProperties()
	{
		Properties ldap = new Properties();
		ldap.putAll(properties);
		ldap.remove(PREFIX + OTP_SECRET_ATTRIBUTE);
		ldap.remove(PREFIX + OTP_ALLOWED_TIME_DRIFT_STEPS);
		ldap.remove(PREFIX + OTP_CODE_LENGHT);
		ldap.remove(PREFIX + OTP_TIME_STEP_SECODS);
		ldap.remove(PREFIX + OTP_HASH_FUNCTION);

		ldap.put(PREFIX + LdapProperties.BIND_AS, BindAs.system.toString());
		ldap.put(PREFIX + LdapProperties.BIND_ONLY, String.valueOf(false));

		if (getValue(USER_DN_TEMPLATE) == null)
		{
			ldap.put(PREFIX + LdapProperties.USER_DN_SEARCH_KEY, LdapProperties.USER_DN_SEARCH_KEY);
			String advSearchprefix = PREFIX + LdapProperties.ADV_SEARCH_PFX + LdapProperties.USER_DN_SEARCH_KEY + ".";
			if (getValue(LDAP_SEARCH_BASENAME) != null)
				ldap.put(advSearchprefix + LdapProperties.ADV_SEARCH_BASE, getValue(LDAP_SEARCH_BASENAME));
			if (getValue(LDAP_SEARCH_FILTER) != null)
				ldap.put(advSearchprefix + LdapProperties.ADV_SEARCH_FILTER, getValue(LDAP_SEARCH_FILTER));
			if (getValue(LDAP_SEARCH_SCOPE) != null)
				ldap.put(advSearchprefix + LdapProperties.ADV_SEARCH_SCOPE, getValue(LDAP_SEARCH_SCOPE));
		}

		return new LdapProperties(PREFIX, ldap);
	}

	Properties getProperties()
	{
		return properties;
	}
}
