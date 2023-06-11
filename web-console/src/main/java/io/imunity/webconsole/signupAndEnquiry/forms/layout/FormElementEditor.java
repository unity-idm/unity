/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.forms.layout;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.registration.layout.FormElement;

/**
 * Interface to be implemented by {@link FormElement} editors.
 * Editors most often simply present the contained element. 
 * Sometimes they allow for providing additional settings.
 * @author K. Benedyczak
 */
interface FormElementEditor<T extends FormElement> extends Component
{
	T getElement();
}
