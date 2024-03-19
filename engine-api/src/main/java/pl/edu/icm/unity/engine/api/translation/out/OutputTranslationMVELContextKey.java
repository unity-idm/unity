/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.translation.out;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum OutputTranslationMVELContextKey
{
	protocol(OutputTranslationMVELContextKey.descriptionPrefix + "protocol"),
	protocolSubtype(OutputTranslationMVELContextKey.descriptionPrefix + "protocolSubtype"),
	requester(OutputTranslationMVELContextKey.descriptionPrefix + "requester"),
	attr(OutputTranslationMVELContextKey.descriptionPrefix + "attr"),
	attrs(OutputTranslationMVELContextKey.descriptionPrefix + "attrs"),
	attrObj(OutputTranslationMVELContextKey.descriptionPrefix + "attrObj"),
	requesterAttr(OutputTranslationMVELContextKey.descriptionPrefix + "requesterAttr"),
	requesterAttrs(OutputTranslationMVELContextKey.descriptionPrefix + "requesterAttrs"),
	requesterAttrObj(OutputTranslationMVELContextKey.descriptionPrefix + "requesterAttrObj"),
	idsByType(OutputTranslationMVELContextKey.descriptionPrefix + "idsByType"),
	importStatus(OutputTranslationMVELContextKey.descriptionPrefix + "importStatus"),
	groups(OutputTranslationMVELContextKey.descriptionPrefix + "groups"),
	usedGroup(OutputTranslationMVELContextKey.descriptionPrefix + "usedGroup"),
	subGroups(OutputTranslationMVELContextKey.descriptionPrefix + "subGroups"),
	groupsObj(OutputTranslationMVELContextKey.descriptionPrefix + "groupsObj"),
	authenticatedWith(OutputTranslationMVELContextKey.descriptionPrefix + "authenticatedWith"),
	idp(OutputTranslationMVELContextKey.descriptionPrefix + "idp"),
	authentications(OutputTranslationMVELContextKey.descriptionPrefix + "authentications"),
	mfa(OutputTranslationMVELContextKey.descriptionPrefix + "mfa"),
	upstreamACRs(OutputTranslationMVELContextKey.descriptionPrefix + "upstreamACRs"),
	upstreamIdP(OutputTranslationMVELContextKey.descriptionPrefix + "upstreamIdP"),
	upstreamProtocol(OutputTranslationMVELContextKey.descriptionPrefix + "upstreamProtocol");

	
	
	public static final String descriptionPrefix = "OutputTranslationMVELContextKey.";
	public final String descriptionKey;

	private OutputTranslationMVELContextKey(String descriptionKey)
	{
		this.descriptionKey = descriptionKey;
	}

	public static Map<String, String> toMap()
	{
		return Stream.of(values()).collect(Collectors.toMap(v -> v.name(), v -> v.descriptionKey));
	}
}