/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.file;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.files.URIHelper;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.LOGO_IMAGE;

class RemoteUrlComponent extends CustomField<LocalOrRemoteResource> implements HasValidator<LocalOrRemoteResource>
{
	private final MessageSource msg;
	private final TextField remoteUrl;
	private final LocalOrRemoteResource image;
	private boolean enabled;

	RemoteUrlComponent(MessageSource msg)
	{
		this.msg = msg;
		image = new LocalOrRemoteResource();
		image.getStyle().set("margin-left", "unset");
		image.addClassName(LOGO_IMAGE.getName());
		remoteUrl = new TextField();
		remoteUrl.setLabel(msg.getMessage("FileField.url"));
		remoteUrl.setWidth(TEXT_FIELD_BIG.value());
		remoteUrl.addValueChangeListener(e ->
		{
			setPresentationValue(new LocalOrRemoteResource(e.getValue(), ""));
			image.setSrc(e.getValue());
			updateValue();
		});

		VerticalLayout layout = new VerticalLayout(remoteUrl, image);
		layout.setPadding(false);
		add(layout);
	}

	@Override
	protected LocalOrRemoteResource generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(LocalOrRemoteResource s)
	{
		setValue(s);
	}

	@Override
	public void setValue(LocalOrRemoteResource value) {
		if(value == null)
			return;
		if(value.getLocal() != null)
			return;
		remoteUrl.setValue(value.getSrc());
		value.setLocal(null);
		super.setValue(value);
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	@Override
	public Validator<LocalOrRemoteResource> getDefaultValidator()
	{
		return (value, context) ->
		{
			if (value != null && !value.getSrc().isEmpty() && enabled)
			{
				if (!URIHelper.isWebReady(value.getSrc()) && !URIHelper.isLocalFile(value.getSrc()))
				{
					image.setVisible(false);
					return ValidationResult.error(msg.getMessage("FileField.notWebUri"));
				}
				image.setVisible(true);
			}
			return ValidationResult.ok();
		};
	}
}
