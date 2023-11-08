/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_browser.identities;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

class IdentitiesMessageHelper
{
	static String getConfirmTextForIdentitiesNodes(MessageSource msg, Set<IdentityEntry> selection)
	{
		Collection<String> ids = new ArrayList<>();
		for (IdentityEntry o: selection)
			ids.add(o.getSourceIdentity().toHumanReadableString());		
		return MessageUtils.createConfirmFromStrings(msg, ids);
	}
}
