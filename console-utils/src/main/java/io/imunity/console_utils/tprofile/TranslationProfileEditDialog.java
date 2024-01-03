/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console_utils.tprofile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;


public class TranslationProfileEditDialog extends Dialog
{
	private final TranslationProfileEditor editor;
	private final Callback callback;
	
	public TranslationProfileEditDialog(MessageSource msg, String caption,
			Callback callback, TranslationProfileEditor editor)
	{
		this.editor = editor;
		this.callback = callback;
		setHeaderTitle(caption);
		add(getContents());
		getFooter().add(new Button(msg.getMessage("cancel"), e -> close()), new Button(msg.getMessage("ok"), e -> onConfirm()));
	}

	protected Component getContents()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setMargin(false);
		vl.setSpacing(false);
		vl.add(editor);
		vl.setAlignItems(FlexComponent.Alignment.START);
		vl.setHeightFull();
		return vl;
	}
	protected void onConfirm()
	{
		TranslationProfile profile;
		try
		{
			profile = editor.getProfile();
		} catch (Exception e)
		{
			return;
		}
			
		if (callback.handleProfile(profile))
			close();
	}
	
	public interface Callback
	{
		public boolean handleProfile(TranslationProfile profile);
	}
	
}