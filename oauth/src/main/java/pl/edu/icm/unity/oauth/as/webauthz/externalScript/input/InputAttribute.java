/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz.externalScript.input;

import java.util.List;
import java.util.Objects;

import pl.edu.icm.unity.base.attribute.Attribute;

public record InputAttribute(
		String name,
		String valueSyntax,
		String groupPath,
		List<String> values,
		String translationProfile,
		String remoteIdp)
{
	public InputAttribute
	{
		values = values != null ? List.copyOf(values) : List.of();
	}

	public static InputAttribute fromAttribute(Attribute a)
	{
		Objects.requireNonNull(a, "Attribute must not be null");

		return new InputAttribute(a.getName(), a.getValueSyntax(), a.getGroupPath(), a.getValues(),
				a.getTranslationProfile(), a.getRemoteIdp());
	}
}
