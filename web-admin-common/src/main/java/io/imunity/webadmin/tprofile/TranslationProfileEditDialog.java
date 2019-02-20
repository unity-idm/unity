/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webadmin.tprofile;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.common.AbstractDialog;



/**
 * Responsible for translation profile edit
 * @author P. Piernik
 *
 */
public class TranslationProfileEditDialog extends AbstractDialog
{
	private TranslationProfileEditor editor;
	private Callback callback;
	
	public TranslationProfileEditDialog(UnityMessageSource msg, String caption,
			Callback callback, TranslationProfileEditor editor)
	{
		super(msg, caption);
		this.editor = editor;
		this.callback = callback;
		setSizeMode(SizeMode.LARGE);
	}

	@Override
	protected Component getContents() throws Exception
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setMargin(false);
		vl.setSpacing(false);
		vl.addComponent(editor);
		vl.setComponentAlignment(editor, Alignment.TOP_LEFT);
		vl.setHeight(100, Unit.PERCENTAGE);
		return vl;
	}
	@Override
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
	@Override
	public void show()
	{
		super.show();
		unbindEnterShortcut();
	}
	
	public interface Callback
	{
		public boolean handleProfile(TranslationProfile profile);
	}
	
}