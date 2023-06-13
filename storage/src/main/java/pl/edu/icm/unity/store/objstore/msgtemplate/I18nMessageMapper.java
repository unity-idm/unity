/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.msgtemplate;

import java.util.Optional;

import pl.edu.icm.unity.base.i18n.I18nMessage;
import pl.edu.icm.unity.store.types.common.I18nStringMapper;

class I18nMessageMapper
{
	static DBI18nMessage map(I18nMessage message)
	{
		return DBI18nMessage.builder()
				.withBody(Optional.ofNullable(message.getBody())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withSubject(Optional.ofNullable(message.getSubject())
						.map(I18nStringMapper::map)
						.orElse(null))
				.build();
	}

	static I18nMessage map(DBI18nMessage dbi18nMessage)
	{
		return new I18nMessage(Optional.ofNullable(dbi18nMessage.subject)
				.map(I18nStringMapper::map)
				.orElse(null),
				Optional.ofNullable(dbi18nMessage.body)
						.map(I18nStringMapper::map)
						.orElse(null));
	}
}
