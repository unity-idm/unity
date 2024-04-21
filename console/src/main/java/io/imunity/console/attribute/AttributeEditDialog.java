/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.attribute;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.dependency.CssImport;

import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;

@CssImport(value = "./dialog.css", themeFor = "vaadin-confirm-dialog-overlay")
public class AttributeEditDialog extends DialogWithActionFooter
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, AttributeEditDialog.class);

	
	private final AttributeEditor editor;
	private final Callback callback;

	public AttributeEditDialog(MessageSource msg, String caption, Callback callback,
			AttributeEditor attributeEditor)
	{
		super(msg::getMessage);
		setHeaderTitle(caption);
		this.editor = attributeEditor;
		this.callback = callback;
		setActionButton(msg.getMessage("ok"), this::onConfirm);
		setWidth("40em");
		setHeight("30em");
		add(editor);
	}

	protected void onConfirm()
	{
		Attribute attribute;
		try
		{
			attribute = editor.getAttribute();
			if (callback.newAttribute(attribute))
				close();
		} catch (FormValidationException e)
		{
			LOG.debug("error editing attribute", e);
		}
	}
	
	public interface Callback
	{
		boolean newAttribute(Attribute newAttribute);
	}
}
