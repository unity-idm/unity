/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.layout;

import com.vaadin.flow.component.HasComponents;
import pl.edu.icm.unity.base.registration.layout.FormElement;

/**
 * Interface to be implemented by {@link FormElement} editors.
 * Editors most often simply present the contained element. 
 * Sometimes they allow for providing additional settings.
 * @author K. Benedyczak
 */
interface FormElementEditor<T extends FormElement> extends HasComponents
{
	T getComponent();
}
