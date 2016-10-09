/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import pl.edu.icm.unity.engine.api.registration.FormAutomationSupport;

/**
 * Internal interface allowing to configure the bean.
 * 
 * @author K. Benedyczak
 */
public interface FormAutomationSupportExt extends FormAutomationSupport
{
	void init(BaseFormTranslationProfile profile);
}
