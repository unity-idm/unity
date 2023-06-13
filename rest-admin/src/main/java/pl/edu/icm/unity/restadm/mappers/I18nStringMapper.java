/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import io.imunity.rest.api.types.basic.RestI18nString;
import pl.edu.icm.unity.base.i18n.I18nString;

public class I18nStringMapper
{
	public static RestI18nString map(I18nString source)
	{
		return RestI18nString.builder()
				.withDefaultValue(source.getDefaultValue())
				.withValues(source.getMap())
				.build();
	}

	public static I18nString map(RestI18nString restI18nString)
	{
		I18nString ret = new I18nString(restI18nString.defaultValue);
		ret.addAllValues(restI18nString.values);
		return ret;

	}
}
