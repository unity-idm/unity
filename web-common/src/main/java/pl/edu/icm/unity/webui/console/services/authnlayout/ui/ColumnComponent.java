/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;

public interface ColumnComponent extends Component
{
	void refresh();
	void validate() throws FormValidationException;
	void setConfigState(AuthnElementConfiguration state);
	void addValueChangeListener(Runnable valueChange);
	AuthnElementConfiguration getConfigState();
}
