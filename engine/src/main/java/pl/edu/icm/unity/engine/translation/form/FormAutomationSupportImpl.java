/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import pl.edu.icm.unity.engine.api.registration.RequestSubmitStatus;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.UserRequestState;

/**
 * Classic translation profile used for post-processing registration requests.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class FormAutomationSupportImpl implements FormAutomationSupportExt
{
	private BaseFormTranslationProfile profile; 
	
	@Override
	public void init(BaseFormTranslationProfile profile)
	{
		this.profile = profile;
	}
	
	@Transactional
	@Override
	public AutomaticRequestAction getAutoProcessAction(
			UserRequestState<? extends BaseRegistrationInput> request, RequestSubmitStatus status)
	{
		return profile.getAutoProcessAction(request, status);
	}
}
