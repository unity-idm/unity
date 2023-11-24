/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@PrototypeComponent
public class EnquiryFormEditor extends VerticalLayout
{
	public EnquiryFormEditor init(boolean arg)
	{
		return this;
	}


	public void setForm(EnquiryForm target)
	{
		//todo remember to use FORM_PROFILE var
	}
}
