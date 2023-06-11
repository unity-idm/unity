/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui.components;

import java.util.function.Consumer;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.ExpandConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponent;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponentBase;

/**
 * 
 * @author P.Piernik
 *
 */
public class ExpandColumnComponent extends ColumnComponentBase
{	
	public ExpandColumnComponent(MessageSource msg, Consumer<ColumnComponent> removeElementListener,
			Runnable dragStart, Runnable dragStop)
	{
		super(msg, msg.getMessage("AuthnColumnLayoutElement.expand"), Images.expand,
				dragStart, dragStop, removeElementListener);
	}

	@Override
	public void setConfigState(AuthnElementConfiguration state)
	{

		
	}

	@Override
	public AuthnElementConfiguration getConfigState()
	{
		return new ExpandConfig();
	}
}