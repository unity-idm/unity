/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.endpoints.layout.elements;

import java.util.function.Consumer;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.endpoints.authnlayout.ColumnElement;
import pl.edu.icm.unity.webui.authn.endpoints.authnlayout.ColumnElementBase;
import pl.edu.icm.unity.webui.common.Images;

/**
 * 
 * @author P.Piernik
 *
 */
public class RegistrationColumnElement extends ColumnElementBase
{

	public RegistrationColumnElement(UnityMessageSource msg, Consumer<ColumnElement> removeElementListener,
			Runnable dragStart, Runnable dragStop)
	{
		super(msg, msg.getMessage("AuthnColumnLayoutElement.registration"), Images.addIdentity,
				dragStart, dragStop, removeElementListener);
	}

}