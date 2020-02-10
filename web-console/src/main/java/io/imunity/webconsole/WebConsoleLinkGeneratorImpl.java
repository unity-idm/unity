/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import org.springframework.stereotype.Component;

import com.vaadin.ui.Button;

import io.imunity.webconsole.signupAndEnquiry.registration.EditRegistrationFormView;
import io.imunity.webconsole.spi.WebConsoleLinkGenerator;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Implementation for {@link WebConsoleLinkGenerator}
 * 
 * @author P.Piernik
 *
 */
@Component
public class WebConsoleLinkGeneratorImpl implements WebConsoleLinkGenerator
{

	@Override
	public Button editRegistrationForm(String formName)
	{
		Button button = new Button(formName);
		button.setStyleName(Styles.vButtonLink.toString());
		button.addClickListener(e -> NavigationHelper.goToView(EditRegistrationFormView.VIEW_NAME + "/"
				+ CommonViewParam.name.toString() + "=" + formName));
		return button;
	}
}
