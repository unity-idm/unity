/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.bulk;

import java.util.function.Consumer;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.server.bulkops.ProcessingRule;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Editor dialog for rule editing.
 * @author K. Benedyczak
 */
public class RuleEditDialog<T extends ProcessingRule> extends AbstractDialog
{
	private Consumer<T> callback;
	private RuleEditor<T> editor;

	public RuleEditDialog(UnityMessageSource msg, String caption, RuleEditor<T> editor,
			Consumer<T> callback)
	{
		super(msg, caption);
		this.editor = editor;
		this.callback = callback;
	}

	@Override
	protected Component getContents() throws Exception
	{
		return editor;
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			callback.accept(editor.getRule());
		} catch (FormValidationException e)
		{
			//ok - just do not close
			return;
		}
		close();
	}
}
