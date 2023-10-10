/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.*;

import java.util.Optional;

@Uses.Container({
		@Uses(MultiSelectComboBox.class),
		@Uses(PasswordField.class),
		@Uses(Upload.class),
		@Uses(FidoBrowserCallableComponent.class),
		@Uses(ConfirmDialog.class)
})
public abstract class UnityViewComponent extends Composite<Div> implements HasUrlParameter<String>, HasDynamicTitle
{
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter)
	{
	}

	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.empty();
	}
}
