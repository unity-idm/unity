/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.chips;

import java.util.Optional;

/**
 * {@link ChipsWithDropdown} text version. Allow also for free-text input.
 * 
 * @author P.Piernik
 *
 */
public class ChipsWithFreeText extends ChipsWithDropdown<String>
{
	public ChipsWithFreeText()
	{
		super();
		combo.setNewItemProvider(s -> {
			combo.setSelectedItem(s);
			return Optional.of(s);
		});
	}
}
