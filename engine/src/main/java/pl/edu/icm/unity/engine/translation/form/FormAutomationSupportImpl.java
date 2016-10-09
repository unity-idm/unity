/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import pl.edu.icm.unity.engine.api.registration.RequestSubmitStatus;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.RegistrationContext;
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

	@Transactional
	@Override
	public I18nMessage getPostSubmitMessage(BaseRegistrationInput request,
			RegistrationContext context, String requestId)
	{
		return profile.getPostSubmitMessage(request, context, requestId);
	}
	
	@Transactional
	@Override
	public String getPostSubmitRedirectURL(BaseRegistrationInput request,
			RegistrationContext context, String requestId)
	{
		return profile.getPostSubmitRedirectURL(request, context, requestId);
	}

	@Transactional
	@Override
	public String getPostCancelledRedirectURL(RegistrationContext context)
	{
		return profile.getPostCancelledRedirectURL(context);
	}

	@Transactional
	@Override
	public String getPostConfirmationRedirectURL(UserRequestState<?> request,
			IdentityParam confirmed, String requestId)
	{
		return profile.getPostConfirmationRedirectURL(request, confirmed, requestId);
	}

	@Transactional
	@Override
	public String getPostConfirmationRedirectURL(UserRequestState<?> request,
			Attribute confirmed, String requestId)
	{
		return profile.getPostConfirmationRedirectURL(request, confirmed, requestId);
	}
}
