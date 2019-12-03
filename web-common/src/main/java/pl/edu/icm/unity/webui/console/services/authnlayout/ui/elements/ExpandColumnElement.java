/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui.elements;

import java.util.function.Consumer;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnElement;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnElementBase;

/**
 * 
 * @author P.Piernik
 *
 */
public class ExpandColumnElement extends ColumnElementBase
{

	public ExpandColumnElement(UnityMessageSource msg, Consumer<ColumnElement> removeElementListener,
			Runnable dragStart, Runnable dragStop)
	{
		super(msg, msg.getMessage("AuthnColumnLayoutElement.expand"), Images.expand,
				dragStart, dragStop, removeElementListener);
	}

}