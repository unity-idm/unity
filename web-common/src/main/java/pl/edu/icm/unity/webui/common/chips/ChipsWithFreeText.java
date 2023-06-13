/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.chips;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import pl.edu.icm.unity.base.message.MessageSource;

/**
 * {@link ChipsWithDropdown} text version. Allow also for free-text input.
 * 
 * @author P.Piernik
 *
 */
public class ChipsWithFreeText extends ChipsWithDropdown<String>
{
	public ChipsWithFreeText(MessageSource msg)
	{
		super();
		combo.setNewItemProvider(s -> {
			if (getValue().contains(s))
				return Optional.empty();
			return Optional.of(s);	
		});
		combo.setDescription(msg.getMessage("typeOrSelect"));
		combo.setPlaceholder(msg.getMessage("typeOrSelect"));
	}
	
	@Override
	protected void updateComboVisibility(Set<String> selected, List<String> available)
	{
		combo.setVisible(true);
	}
}
