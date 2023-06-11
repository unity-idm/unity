/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.translation.form;

import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;

/**
 * Generator for registration translation action
 * 
 * @author P.Piernik
 *
 */
public interface RegistrationFormTranslationActionGenerator
{
	TranslationAction getAddAttributeAction(String attributeName, String group, String attributeValue);

	TranslationAction getAutoProcessAction(AutomaticRequestAction action);
	
	TranslationAction getAddIdentifierIndentityAction(String identity);
	
	TranslationAction getAddToGroupAction(String group);
}
