/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.bulk;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.server.bulkops.ProcessingRule;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Contract of all rule editors
 * @author K. Benedyczak
 */
public interface RuleEditor<T extends ProcessingRule> extends Component
{
	T getRule() throws FormValidationException;
}
