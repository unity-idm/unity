/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Toolbar;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Responsible for message templates management
 * @author P. Piernik
 *
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TranslationProfilesComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private TranslationProfileManagement profileMan;
	private GenericElementsTable<TranslationProfile> table;
	private TranslationProfileViewer viewer;
	private com.vaadin.ui.Component main;
	private TranslationActionsRegistry tc;
	
	@Autowired
	public TranslationProfilesComponent(UnityMessageSource msg,
			TranslationProfileManagement profileMan,
			TranslationActionsRegistry tc)
	{
		this.msg = msg;
		this.profileMan = profileMan;
		this.tc = tc;
		
		HorizontalLayout hl = new HorizontalLayout();
		setCaption(msg.getMessage("TranslationProfilesComponent.capion"));
		table = new GenericElementsTable<TranslationProfile>(msg.getMessage("TranslationProfilesComponent.profilesTable"),
				TranslationProfile.class,new GenericElementsTable.NameProvider<TranslationProfile>()
				{
					@Override
					public Label toRepresentation(TranslationProfile element)
					{
						return new Label(element.getName());
					}
				});
		
		table.setWidth(90, Unit.PERCENTAGE);
		viewer = new TranslationProfileViewer(msg, tc);
		table.addValueChangeListener(new ValueChangeListener()
		{
			
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				GenericItem<TranslationProfile> item = (GenericItem<TranslationProfile>)table.getValue();
				if (item!=null)
				{
					TranslationProfile profile = item.getElement();
					viewer.setInput(profile);
				}else
				{
					viewer.setInput(null);
				}
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		
		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		
		hl.addComponents(tableWithToolbar, viewer);
		hl.setSizeFull();
		hl.setMargin(true);
		hl.setSpacing(true);
		hl.setMargin(new MarginInfo(true, false, true, false));
		main = hl;
		hl.setExpandRatio(tableWithToolbar, 0.3f);
		hl.setExpandRatio(viewer, 0.7f);
		refresh();
	}
	
	private void refresh()
	{
		try
		{
			Collection<TranslationProfile> profiles = profileMan.listProfiles().values();
			table.setInput(profiles);
			viewer.setInput(null);
			table.select(null);
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("TranslationProfilesComponent.errorGetProfiles"), e);
			removeAllComponents();
			addComponent(error);
		}
		
	}
	
	private boolean updateProfile(TranslationProfile updatedProfile)
	{
		try
		{
			profileMan.updateProfile(updatedProfile);
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorUpdate"), e);
			return false;
		}
	}
		
	private boolean addProfile(TranslationProfile profile)
	{
		try
		{
			profileMan.addProfile(profile);
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorAdd"), e);
			return false;
		}
	}
	
	private boolean removeProfile(String name)
	{
		try
		{
			profileMan.removeProfile(name);
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorRemove"), e);
			return false;
		}
	}
	
	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.refreshAction"), Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}
	
	private class AddActionHandler extends SingleActionHandler
	{
		public AddActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.addAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			TranslationProfileEditor editor;
			
			editor = new TranslationProfileEditor(msg, tc, null);
			
			TranslationProfileEditDialog dialog = new TranslationProfileEditDialog(msg, 
					msg.getMessage("TranslationProfilesComponent.addAction"), new TranslationProfileEditDialog.Callback()
					{
						@Override
						public boolean newProfile(TranslationProfile profile)
						{
							return addProfile(profile);
						}
					}, editor);
			dialog.show();
		}
	}
	
	private class EditActionHandler extends SingleActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.editAction"), Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			@SuppressWarnings("unchecked")
			GenericItem<TranslationProfile> item = (GenericItem<TranslationProfile>) target;
			TranslationProfileEditor editor;
			
			editor = new TranslationProfileEditor(msg, tc, item.getElement());
			
			TranslationProfileEditDialog dialog = new TranslationProfileEditDialog(msg, 
					msg.getMessage("TranslationProfilesComponent.editAction"), new TranslationProfileEditDialog.Callback()
					{
						@Override
						public boolean newProfile(TranslationProfile profile)
						{
							return updateProfile(profile);
						}
					}, editor);
			dialog.show();
		}
	}

	private class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.deleteAction"),
					Images.delete.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			@SuppressWarnings("unchecked")
			GenericItem<TranslationProfile> item = (GenericItem<TranslationProfile>) target;
			final TranslationProfile profile = item.getElement();
			new ConfirmDialog(msg, msg.getMessage(
					"TranslationProfilesComponent.confirmDelete",
					profile.getName()), new ConfirmDialog.Callback()
			{

				@Override
				public void onConfirm()
				{
					removeProfile(profile.getName());

				}
			}).show();
		}
	}
}
