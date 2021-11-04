/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.translation.form;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum RegistrationMVELContextKey
{
	idsByType (RegistrationMVELContextKey.descriptionPrefix + "idsByType"), 
	ridsByType (RegistrationMVELContextKey.descriptionPrefix + "ridsByType"),
	idsByTypeObj (RegistrationMVELContextKey.descriptionPrefix + "idsByTypeObj"),
	ridsByTypeObj (RegistrationMVELContextKey.descriptionPrefix + "ridsByTypeObj"),
	attrs (RegistrationMVELContextKey.descriptionPrefix + "attrs"),
	attr (RegistrationMVELContextKey.descriptionPrefix + "attr"),
	rattrs (RegistrationMVELContextKey.descriptionPrefix + "rattrs"),
	rattr (RegistrationMVELContextKey.descriptionPrefix + "rattr"),
	groups (RegistrationMVELContextKey.descriptionPrefix + "groups"),
	rgroups (RegistrationMVELContextKey.descriptionPrefix + "rgroups"),
	status (RegistrationMVELContextKey.descriptionPrefix + "status"),
	triggered (RegistrationMVELContextKey.descriptionPrefix + "triggered"),
	onIdpEndpoint (RegistrationMVELContextKey.descriptionPrefix + "onIdpEndpoint"),
	userLocale (RegistrationMVELContextKey.descriptionPrefix + "userLocale"),
	registrationForm (RegistrationMVELContextKey.descriptionPrefix + "registrationForm"),
	requestId (RegistrationMVELContextKey.descriptionPrefix + "requestId"),
	agrs (RegistrationMVELContextKey.descriptionPrefix + "agrs"),
	validCode (RegistrationMVELContextKey.descriptionPrefix + "validCode");
	
	
	public final String descriptionKey;

	private RegistrationMVELContextKey(String descriptionKey)
	{
		this.descriptionKey = descriptionKey;
	}

	public static final String descriptionPrefix = "RegistrationMVELContextKey.";

	public static Map<String, String> toMap()
	{
		return Stream.of(values()).collect(Collectors.toMap(v -> v.name(), v -> v.descriptionKey));
	}
}