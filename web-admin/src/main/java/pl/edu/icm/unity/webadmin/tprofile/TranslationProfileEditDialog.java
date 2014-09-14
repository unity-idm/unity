/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;



/**
 * Responsible for translation profile edit
 * @author P. Piernik
 *
 */
public class TranslationProfileEditDialog extends AbstractDialog
{
	TranslationProfileEditor editor;
	private Callback callback;
	public TranslationProfileEditDialog(UnityMessageSource msg, String caption,
			Callback callback, TranslationProfileEditor editor)
	{
		super(msg, caption);
		this.editor = editor;
		this.callback = callback;
		setWidth(60, Unit.PERCENTAGE);
		setHeight(85, Unit.PERCENTAGE);
	}

	@Override
	protected Component getContents() throws Exception
	{
		VerticalLayout vl = new VerticalLayout();
		vl.addComponent(editor);
		vl.setComponentAlignment(editor, Alignment.TOP_LEFT);
		vl.setHeight(100, Unit.PERCENTAGE);
		return vl;
	}
	@Override
	protected void onConfirm()
	{
		TranslationProfile profile = editor.getProfile();
		if(profile == null)
			return;
			
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