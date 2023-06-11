/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directorySetup.automation;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Contract of all rule editors
 * @author K. Benedyczak
 */
interface RuleEditor<T extends TranslationRule> extends Component
{
	T getRule() throws FormValidationException;
}
