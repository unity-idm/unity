/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;


/**
 * Factory, creating actions which can be used to redirect the request submitter,
 * after she confirms one of the verifiable elements of her registration request.
 * <p>
 * This factory simply reuses the action implementation of the {@link RedirectActionFactory}
 * @author K. Benedyczak
 */
@Component
@Deprecated
public class ConfirmationRedirectActionFactory extends AbstractRegistrationTranslationActionFactory
{
	
	public static final String NAME = "confirmationRedirect";
	
	public ConfirmationRedirectActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("URL", 
						"RegTranslationAction.confirmationRedirect.paramDesc.URL",
						Type.EXPRESSION, true)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new RedirectActionFactory.RedirectAction(getActionType(), parameters);
	}
}



