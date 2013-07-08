/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credreq;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.webadmin.credreq.CredentialRequirementEditDialog.Callback;
import pl.edu.icm.unity.webadmin.generic.GenericElementsTable;
import pl.edu.icm.unity.webadmin.generic.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Provides {@link CredentialRequirements} management UI
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CredentialRequirementsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private AuthenticationManagement authenticationMan;
	private EventsBus bus;
	
	private GenericElementsTable<CredentialRequirements> table;
	private CredentialRequirementViewer viewer;
	private com.vaadin.ui.Component main;
	
	@Autowired
	public CredentialRequirementsComponent(UnityMessageSource msg,
			AuthenticationManagement authenticationMan)
	{
		super();
		this.msg = msg;
		this.authenticationMan = authenticationMan;
		this.bus = WebSession.getCurrent().getEventBus();
		
		init();
	}
	
	private void init()
	{
		setCaption(msg.getMessage("CredentialRequirements.caption"));
		table =  new GenericElementsTable<CredentialRequirements>(
				msg.getMessage("CredentialRequirements.credentialRequirementsHeader"), 
				CredentialRequirements.class, new GenericElementsTable.NameProvider<CredentialRequirements>()
				{
					@Override
					public String toString(CredentialRequirements element)
					{
						return element.getName();
					}
				});
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				@SuppressWarnings("unchecked")
				GenericItem<CredentialRequirements> item = (GenericItem<CredentialRequirements>)table.getValue();
				if (item != null)
				{
					CredentialRequirements at = item.getElement();
					viewer.setInput(at);
				} else
					viewer.setInput(null);
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		table.setWidth(90, Unit.PERCENTAGE);
		
		viewer = new CredentialRequirementViewer(msg);
		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponents(table, viewer);
		hl.setSizeFull();
		hl.setMargin(new MarginInfo(true, false, true, false));
		hl.setSpacing(true);
		main = hl;
		refresh();
	}
	
	public void refresh()
	{
		try
		{
			Collection<CredentialRequirements> crs = authenticationMan.getCredentialRequirements();
			table.setInput(crs);
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("CredentialRequirements.errorGetCredentialRequirements"), e);
			removeAllComponents();
			addComponent(error);
		}
		
	}

	public Collection<CredentialRequirements> getCredentialRequirements()
	{
		try
		{
			return authenticationMan.getCredentialRequirements();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("CredentialRequirements.errorGetCredentialRequirements"), e);
			return null;
		}
	}

	private Collection<CredentialDefinition> getCredentials()
	{
		try
		{
			return authenticationMan.getCredentialDefinitions();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("CredentialRequirements.errorGetCredentials"), e);
			return null;
		}
		
	}

	private boolean updateCR(CredentialRequirements cr)
	{
		try
		{
			authenticationMan.updateCredentialRequirement(cr);
			refresh();
			bus.fireEvent(new CredentialRequirementChangedEvent());
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("CredentialRequirements.errorUpdate"), e);
			return false;
		}
	}

	private boolean addCR(CredentialRequirements cr)
	{
		try
		{
			authenticationMan.addCredentialRequirement(cr);
			refresh();
			bus.fireEvent(new CredentialRequirementChangedEvent());
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("CredentialRequirements.errorAdd"), e);
			return false;
		}
	}

	private boolean removeCR(String toRemove, String replacementId)
	{
		try
		{
			authenticationMan.removeCredentialRequirement(toRemove, replacementId);
			refresh();
			bus.fireEvent(new CredentialRequirementChangedEvent());
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("CredentialRequirements.errorRemove"), e);
			return false;
		}
	}

	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("CredentialRequirements.refreshAction"), Images.refresh.getResource());
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
			super(msg.getMessage("CredentialRequirements.addAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			Collection<CredentialDefinition> allCredentials = getCredentials();
			if (allCredentials == null)
				return;
			CredentialRequirementEditor editor = new CredentialRequirementEditor(msg, allCredentials);
			CredentialRequirementEditDialog dialog = new CredentialRequirementEditDialog(msg, 
					msg.getMessage("CredentialRequirements.addAction"), editor, 
					new Callback()
					{
						@Override
						public boolean newCredentialRequirement(CredentialRequirements cr)
						{
							return addCR(cr);
						}
					});
			dialog.show();
		}
	}
	
	private class EditActionHandler extends SingleActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("CredentialRequirements.editAction"), Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			Collection<CredentialDefinition> allCredentials = getCredentials();
			if (allCredentials == null)
				return;
			@SuppressWarnings("unchecked")
			GenericItem<CredentialRequirements> item = (GenericItem<CredentialRequirements>)target;
			CredentialRequirements cr = item.getElement();
			CredentialRequirements crClone = new CredentialRequirements();
			crClone.setDescription(cr.getDescription());
			crClone.setName(cr.getName());
			crClone.setRequiredCredentials(new HashSet<String>(cr.getRequiredCredentials()));
			CredentialRequirementEditor editor = new CredentialRequirementEditor(msg, allCredentials, crClone);
			CredentialRequirementEditDialog dialog = new CredentialRequirementEditDialog(msg, 
					msg.getMessage("CredentialRequirements.editAction"), editor, 
					new Callback()
					{
						@Override
						public boolean newCredentialRequirement(CredentialRequirements cr)
						{
							return updateCR(cr);
						}
					});
			dialog.show();
		}
	}
	
	private class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("CredentialRequirements.deleteAction"), 
					Images.delete.getResource());
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			@SuppressWarnings("unchecked")
			GenericItem<CredentialRequirements> item = (GenericItem<CredentialRequirements>)target;
			final CredentialRequirements at = item.getElement();
			Collection<CredentialRequirements> allCRs = getCredentialRequirements();
			new CredentialRequirementRemovalDialog(msg, at.getName(), allCRs, 
					new CredentialRequirementRemovalDialog.Callback()
			{
				@Override
				public void onConfirm(String replacementCR)
				{
					removeCR(at.getName(), replacementCR);
				}
			}).show();
		}
	}

}
