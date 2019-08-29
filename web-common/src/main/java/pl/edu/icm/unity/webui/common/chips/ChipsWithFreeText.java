/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.chips;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * {@link ChipsWithDropdown} text version. Allow also for free-text input.
 * 
 * @author P.Piernik
 *
 */
public class ChipsWithFreeText extends ChipsWithDropdown<String>
{
	public ChipsWithFreeText(UnityMessageSource msg)
	{
		super();
		combo.setNewItemProvider(s -> {
			combo.setSelectedItem(s);
			return Optional.of(s);
		});
		combo.setDescription(msg.getMessage("addWithEnter"));
		combo.setPlaceholder(msg.getMessage("addWithEnter"));
	}
	
	@Override
	protected void updateComboVisibility(Set<String> selected, List<String> available)
	{
		combo.setVisible(true);
	}
}
