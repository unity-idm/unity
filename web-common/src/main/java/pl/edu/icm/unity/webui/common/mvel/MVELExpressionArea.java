/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.mvel;

import com.vaadin.data.Binder;
import com.vaadin.data.ValueProvider;
import com.vaadin.server.Setter;
import com.vaadin.ui.TextArea;

import pl.edu.icm.unity.MessageSource;

/**
 * Text area allowing for editing an MVEL expression
 * 
 * @author K. Benedyczak
 */
public class MVELExpressionArea extends TextArea
{
	private MVELExpressionEditor editor;

	public MVELExpressionArea(MessageSource msg, String caption, String description)
	{
		this.editor = new MVELExpressionEditor(this, msg, caption, description);
	}

	public void configureBinding(Binder<?> binder, String fieldName, boolean mandatory)
	{
		editor.configureBinding(binder, fieldName, mandatory);
	}

	public <T> void configureBinding(Binder<String> binder,
			ValueProvider<String, String> getter, Setter<String, String> setter,
			boolean mandatory)
	{
		editor.configureBinding(binder, getter, setter, mandatory);
	}
}
