/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.sandbox.wizard.SandboxWizardDialog;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webadmin.WebAdminEndpointFactory;
import pl.edu.icm.unity.webadmin.tprofile.dryrun.DryRunWizardProvider;
import pl.edu.icm.unity.webadmin.tprofile.wizard.ProfileWizardProvider;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;

/**
 * Responsible for translation profiles management.
 * @author P. Piernik
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
	private OptionGroup profileType;
	
	private TranslationActionsRegistry tc;
	private AttributesManagement attrsMan;
	private IdentitiesManagement idMan;
	private AuthenticationManagement authnMan;
	private GroupsManagement groupsMan;

	private SandboxAuthnNotifier sandboxNotifier;
	private String sandboxURL;
	
	@Autowired
	public TranslationProfilesComponent(UnityMessageSource msg, TranslationProfileManagement profileMan,
			TranslationActionsRegistry tc, AttributesManagement attrsMan, IdentitiesManagement idMan, 
			AuthenticationManagement authnMan, GroupsManagement groupsMan, EndpointManagement endpointMan)
	{
		this.msg = msg;
		this.profileMan = profileMan;
		this.tc = tc;
		this.attrsMan = attrsMan;
		this.idMan = idMan;
		this.authnMan = authnMan;
		this.groupsMan = groupsMan;

		setCaption(msg.getMessage("TranslationProfilesComponent.capion"));		
		
		try 
		{
			establishSandboxURL(endpointMan);
		} catch (EngineException e) 
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("TranslationProfilesComponent.errorGetEndpoints"), e);
			removeAllComponents();
			addComponent(error);
			return;
		}

		buildUI();
		
		refresh();
	}
	
	private void establishSandboxURL(EndpointManagement endpointMan) throws EngineException
	{
			List<EndpointDescription> endpointList = endpointMan.getEndpoints();
			for (EndpointDescription endpoint : endpointList) {
				if (endpoint.getType().getName().equals(WebAdminEndpointFactory.NAME))
				{
					sandboxURL = endpoint.getContextAddress() + 
							VaadinEndpoint.SANDBOX_PATH_TRANSLATION;
					break;
				}
			}
	}
	
	
	private void buildUI()
	{
		addStyleName(Styles.visibleScroll.toString());
		HorizontalLayout hl = new HorizontalLayout();
		table = new GenericElementsTable<TranslationProfile>(msg.getMessage("TranslationProfilesComponent.profilesTable"),
				TranslationProfile.class, new GenericElementsTable.NameProvider<TranslationProfile>()
				{
					@Override
					public Label toRepresentation(TranslationProfile element)
					{
						return new Label(element.getName());
					}
				});
		
		table.setMultiSelect(true);
		table.setWidth(90, Unit.PERCENTAGE);
		viewer = new TranslationProfileViewer(msg, tc);
		table.addValueChangeListener(new ValueChangeListener()
		{
			
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<TranslationProfile> items = getItems(table.getValue());
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setInput(null);
					return;
				}	
				TranslationProfile item = items.iterator().next();
				viewer.setInput(item);
			}
		});
		
		profileType = new OptionGroup();
		profileType.setImmediate(true);
		profileType.addItem(ProfileType.INPUT);
		profileType.setItemCaption(ProfileType.INPUT, 
				msg.getMessage("TranslationProfilesComponent.inputProfileType"));
		profileType.addItem(ProfileType.OUTPUT);
		profileType.setItemCaption(ProfileType.OUTPUT, 
				msg.getMessage("TranslationProfilesComponent.outputProfileType"));
		profileType.setNullSelectionAllowed(false);
		profileType.select(ProfileType.INPUT);
		profileType.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				refresh();
			}
		});
		
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		table.addActionHandler(new WizardActionHandler());
		table.addActionHandler(new DryRunActionHandler());
		
		
		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		profileType.addValueChangeListener(toolbar.getValueChangeListener());
		
		VerticalLayout left = new VerticalLayout();
		left.setSpacing(true);
		left.addComponents(profileType, tableWithToolbar);
		
		hl.addComponents(left, viewer);
		hl.setSizeFull();
		hl.setMargin(true);
		hl.setSpacing(true);
		hl.setMargin(new MarginInfo(true, false, true, false));
		main = hl;
		hl.setExpandRatio(left, 0.3f);
		hl.setExpandRatio(viewer, 0.7f);
	}
	
	private void refresh()
	{
		try
		{
			ProfileType pt = (ProfileType) profileType.getValue();
			Collection<? extends TranslationProfile> profiles = null;
			
			switch (pt)
			{
			case INPUT:
				profiles = profileMan.listInputProfiles().values();
				break;
			case OUTPUT:
				profiles = profileMan.listOutputProfiles().values();
				break;
			}
			if (profiles == null)
				throw new IllegalStateException("unknown profile type");
			
			table.setInput(profiles);
			viewer.setInput(null);
			//table.select(null);
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
			NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorUpdate"), e);
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
			NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorAdd"), e);
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
			NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorRemove"), e);
			return false;
		}
	}
	
	private Collection<TranslationProfile> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
		Collection<TranslationProfile> items = new ArrayList<TranslationProfile>();
		for (Object o: c)
		{
			GenericItem<?> i = (GenericItem<?>) o;
			items.add((TranslationProfile) i.getElement());	
		}	
		return items;
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
	
	private TranslationProfileEditor getProfileEditor(TranslationProfile toEdit) throws EngineException
	{
		ProfileType pt = (ProfileType) profileType.getValue();
		switch (pt)
		{
		case INPUT:
			return new InputTranslationProfileEditor(msg, tc, toEdit, attrsMan, 
					idMan, authnMan, groupsMan);
		case OUTPUT:
			return new OutputTranslationProfileEditor(msg, tc, toEdit, attrsMan, 
					idMan, authnMan, groupsMan);
		}
		throw new IllegalStateException("not implemented");
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
			try
			{
				editor = getProfileEditor(null);				
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorReadData"),
						e);
				return;
			}
			
			TranslationProfileEditDialog dialog = new TranslationProfileEditDialog(msg, 
					msg.getMessage("TranslationProfilesComponent.addAction"), 
					new TranslationProfileEditDialog.Callback()
					{
						@Override
						public boolean handleProfile(TranslationProfile profile)
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
			
			try
			{
				editor = getProfileEditor(item.getElement());
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorReadData"),
						e);
				return;
			}
			TranslationProfileEditDialog dialog = new TranslationProfileEditDialog(msg, 
					msg.getMessage("TranslationProfilesComponent.editAction"), 
					new TranslationProfileEditDialog.Callback()
					{
						@Override
						public boolean handleProfile(TranslationProfile profile)
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
			setMultiTarget(true);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final Collection<TranslationProfile> items = getItems(target);
			String confirmText = MessageUtils.createConfirmFromNames(msg, items);
			new ConfirmDialog(msg, msg.getMessage(
					"TranslationProfilesComponent.confirmDelete",
					confirmText), new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					for (TranslationProfile item : items)
					{
						removeProfile(item.getName());
					}

				}
			}).show();
		}
	}
	
	private class WizardActionHandler extends SingleActionHandler
	{
		private TranslationProfileEditDialog.Callback addCallback;
		
		public WizardActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.wizardAction"), Images.wizard.getResource());
			setNeedsTarget(false);
			callback = new SingleActionHandler.ActionButtonCallback() 
			{
				@Override
				public boolean showActionButton() 
				{
					return isInputProfileSelection();
				}
			};
			addCallback = new TranslationProfileEditDialog.Callback()
			{
				@Override
				public boolean handleProfile(TranslationProfile profile)
				{
					return addProfile(profile);
				}
			};			
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			TranslationProfileEditor editor;
			try
			{
				editor = getProfileEditor(null);				
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorReadData"),
						e);
				return;
			}

			ProfileWizardProvider wizardProvider = new ProfileWizardProvider(msg, sandboxURL, 
					sandboxNotifier, editor, addCallback);
			SandboxWizardDialog dialog = new SandboxWizardDialog(wizardProvider.getWizardInstance(),
					wizardProvider.getCaption());
			dialog.show();
		}
	}
	
	private class DryRunActionHandler extends SingleActionHandler
	{
		public DryRunActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.dryrunAction"), Images.dryrun.getResource());
			setNeedsTarget(false);
			callback = new SingleActionHandler.ActionButtonCallback() 
			{
				@Override
				public boolean showActionButton() 
				{
					return isInputProfileSelection();
				}
			};
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			DryRunWizardProvider provider = new DryRunWizardProvider(msg, sandboxURL, sandboxNotifier, 
					tc, profileMan);
			SandboxWizardDialog dialog = new SandboxWizardDialog(provider.getWizardInstance(),
					provider.getCaption());
			dialog.show();
		}
	}	
	
	private boolean isInputProfileSelection()
	{
		boolean isInputProfile = false;
		if (profileType != null) 
		{
			ProfileType pt = (ProfileType) profileType.getValue();
			if (pt == ProfileType.INPUT) 
			{
				isInputProfile = true;
			}
		}
		return isInputProfile;		
	}

	public void setSandboxNotifier(SandboxAuthnNotifier sandboxNotifier) 
	{
		this.sandboxNotifier = sandboxNotifier;
	}
}
