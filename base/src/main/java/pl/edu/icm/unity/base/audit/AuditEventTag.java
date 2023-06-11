/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.audit;

/**
 * Enum containing Tag values - provided to organize possible values.
 * Note: DB and GUI are working based on string vales. All AuditEventTag values are supported but AuditEvent tag is not limited to those values.
 */
public enum AuditEventTag
{
	AUTHN("Authn"),
	GROUPS("Groups"),
	MEMBERS("Members"),
	USERS("Users");

	private final String stringValue;

	AuditEventTag(String stringValue)
	{
		this.stringValue = stringValue;
	}

	public String getStringValue()
	{
		return stringValue;
	}
}
