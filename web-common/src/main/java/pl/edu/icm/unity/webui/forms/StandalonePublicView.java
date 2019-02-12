/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.forms;

import com.vaadin.navigator.View;
import com.vaadin.server.VaadinRequest;

/**
 * Implementations provide public registration or enquiry form view
 * @author P.Piernik
 *
 */
public interface StandalonePublicView extends View
{
	String getFormName();
	void refresh(VaadinRequest request);
}
