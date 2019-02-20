/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.simplefiledownloader.SimpleFileDownloader;

import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webadmin.tprofile.ActionParameterComponentProvider;
import io.imunity.webadmin.tprofile.TranslationProfileEditDialog;
import io.imunity.webadmin.tprofile.TranslationProfileEditor;
import io.imunity.webadmin.tprofile.TranslationProfileViewer;
import io.imunity.webadmin.tprofile.dryrun.DryRunWizardProvider;
import io.imunity.webadmin.tprofile.wizard.ProfileWizardProvider;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webadmin.WebAdminEndpointFactory;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.webui.sandbox.wizard.SandboxWizardDialog;

/**
 * Responsible for translation profiles management.
 * @author P. Piernik
 */
@PrototypeComponent
public class TranslationProfilesComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private TranslationProfileManagement profileMan;
	private GenericElementsTable<TranslationProfile> table;
	private TranslationProfileViewer viewer;
	private com.vaadin.ui.Component main;
	private RadioButtonGroup<ProfileType> profileType;
	
	private InputTranslationActionsRegistry inputActionsRegistry;
	private OutputTranslationActionsRegistry outputActionsRegistry;

	private SandboxAuthnNotifier sandboxNotifier;
	private String sandboxURL;
	private ActionParameterComponentProvider actionComponentFactory;

	private SingleActionHandler<TranslationProfile> dryRunAction;
	private SingleActionHandler<TranslationProfile> wizardAction;	

	@Autowired
	public TranslationProfilesComponent(UnityMessageSource msg,
			TranslationProfileManagement profileMan, EndpointManagement endpointMan,
			InputTranslationActionsRegistry inputTranslationActionsRegistry,
			OutputTranslationActionsRegistry outputTranslationActionsRegistry,
			ActionParameterComponentProvider actionComponentFactory)
	{
		this.msg = msg;
		this.profileMan = profileMan;
		inputActionsRegistry = inputTranslationActionsRegistry;
		outputActionsRegistry = outputTranslationActionsRegistry;
		this.actionComponentFactory = actionComponentFactory;

		setCaption(msg.getMessage("TranslationProfilesComponent.capion"));
		setMargin(false);
		setSpacing(false);

		try
		{
			actionComponentFactory.init();
			establishSandboxURL(endpointMan);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage(
					"TranslationProfilesComponent.errorGetEndpoints"), e);
			removeAllComponents();
			addComponent(error);
			return;
		}

		buildUI();

		refresh();
	}

	private void establishSandboxURL(EndpointManagement endpointMan) throws EngineException
	{
		List<ResolvedEndpoint> endpointList = endpointMan.getEndpoints();
		for (ResolvedEndpoint endpoint : endpointList)
		{
			if (endpoint.getType().getName().equals(WebAdminEndpointFactory.NAME))
			{
				sandboxURL = endpoint.getEndpoint().getContextAddress()
						+ VaadinEndpoint.SANDBOX_PATH_TRANSLATION;
				break;
			}
		}
	}

	private GenericElementsTable<TranslationProfile> createTable()
	{
		table = new GenericElementsTable<>(
				msg.getMessage("TranslationProfilesComponent.profilesTable"),
				element -> element.getName());
		table.setStyleGenerator(element -> element.getProfileMode() == ProfileMode.READ_ONLY
				? Styles.readOnlyTableElement.toString()
				: "");

		table.setMultiSelect(true);
		table.setWidth(90, Unit.PERCENTAGE);
		table.addSelectionListener(event -> {
			Collection<TranslationProfile> items = event.getAllSelectedItems();
			if (items.size() > 1 || items.isEmpty())
			{
				viewer.setInput(null, getCurrentActionsRegistry());
				return;
			}
			TranslationProfile item = items.iterator().next();
			viewer.setInput(item, getCurrentActionsRegistry());
		});

		table.addActionHandler(getRefreshAction());
		table.addActionHandler(getAddAction());
		table.addActionHandler(getEditAction());
		table.addActionHandler(getCopyAction());
		table.addActionHandler(getDeleteAction());
		wizardAction = getWizardAction();
		table.addActionHandler(wizardAction);
		dryRunAction = getDryRunAction();
		table.addActionHandler(dryRunAction);
		table.addActionHandler(getExportAction());

		return table;
	}

	private void buildUI()
	{
		addStyleName(Styles.visibleScroll.toString());
		HorizontalLayout hl = new HorizontalLayout();
		table = createTable();

		profileType = new RadioButtonGroup<>();
		profileType.setItems(ProfileType.INPUT, ProfileType.OUTPUT);
		Map<ProfileType, String> captions = new HashMap<>();
		captions.put(ProfileType.INPUT,
				msg.getMessage("TranslationProfilesComponent.inputProfileType"));
		captions.put(ProfileType.OUTPUT,
				msg.getMessage("TranslationProfilesComponent.outputProfileType"));
		profileType.setItemCaptionGenerator(p -> captions.get(p));
		profileType.setValue(ProfileType.INPUT);

		Toolbar<TranslationProfile> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		table.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		tableWithToolbar.setHeight(100, Unit.PERCENTAGE);

		profileType.addValueChangeListener(e -> {
			dryRunAction.setDisabled(!isInputProfileSelection());
			wizardAction.setDisabled(!isInputProfileSelection());
			toolbar.refresh();
			table.deselectAll();
			refresh();
		});

		viewer = new TranslationProfileViewer(msg);

		VerticalLayout left = new VerticalLayout();
		left.setMargin(false);
		left.setSizeFull();
		left.addComponents(profileType, tableWithToolbar);

		hl.addComponents(left, viewer);
		hl.setSizeFull();
		hl.setMargin(new MarginInfo(true, false, true, false));
		main = hl;
		hl.setExpandRatio(left, 0.3f);
		hl.setExpandRatio(viewer, 0.7f);
	}

	private void refresh()
	{
		try
		{
			switch (profileType.getValue())
			{
			case INPUT:
				Collection<TranslationProfile> inprofiles = profileMan
						.listInputProfiles().values();
				table.setInput(inprofiles);
				break;
			case OUTPUT:
				Collection<TranslationProfile> outprofiles = profileMan
						.listOutputProfiles().values();
				table.setInput(outprofiles);
				break;
			default:
				throw new IllegalStateException("unknown profile type");
			}
			viewer.setInput(null, getCurrentActionsRegistry());
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage(
					"TranslationProfilesComponent.errorGetProfiles"), e);
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
			NotificationPopup.showError(msg,
					msg.getMessage("TranslationProfilesComponent.errorUpdate"),
					e);
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
			NotificationPopup.showError(msg,
					msg.getMessage("TranslationProfilesComponent.errorAdd"), e);
			return false;
		}
	}

	private boolean removeProfile(TranslationProfile profile)
	{
		try
		{
			profileMan.removeProfile(profileType.getValue(), profile.getName());
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("TranslationProfilesComponent.errorRemove"),
					e);
			return false;
		}
	}

	private SingleActionHandler<TranslationProfile> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg, TranslationProfile.class)
				.withHandler(selection -> refresh()).build();
	}

	private TranslationProfileEditor getProfileEditor(TranslationProfile toEdit)
			throws EngineException
	{
		actionComponentFactory.init();
		TranslationProfileEditor editor = new TranslationProfileEditor(msg,
				getCurrentActionsRegistry(), profileType.getValue(),
				actionComponentFactory);
		if (toEdit != null)
			editor.setValue(toEdit);
		return editor;
	}

	private TypesRegistryBase<? extends TranslationActionFactory<?>> getCurrentActionsRegistry()
	{
		return isInputProfileSelection() ? inputActionsRegistry : outputActionsRegistry;
	}

	private SingleActionHandler<TranslationProfile> getAddAction()
	{
		return SingleActionHandler.builder4Add(msg, TranslationProfile.class)
				.withHandler(this::showAddDialog).build();
	}

	private void showAddDialog(Set<TranslationProfile> target)
	{
		TranslationProfileEditor editor;
		try
		{
			editor = getProfileEditor(null);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"TranslationProfilesComponent.errorReadData"), e);
			return;
		}

		TranslationProfileEditDialog dialog = new TranslationProfileEditDialog(msg,
				msg.getMessage("TranslationProfilesComponent.addAction"),
				p -> addProfile(p), editor);
		dialog.show();
	}

	private SingleActionHandler<TranslationProfile> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, TranslationProfile.class)
				.withDisabledPredicate(
						tp -> tp.getProfileMode() == ProfileMode.READ_ONLY)
				.withHandler(this::showEditDialog).build();
	}

	private void showEditDialog(Collection<TranslationProfile> target)
	{

		TranslationProfile profile = target.iterator().next();
		profile = profile.clone();
		TranslationProfileEditor editor;
		try
		{
			editor = getProfileEditor(profile);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"TranslationProfilesComponent.errorReadData"), e);
			return;
		}
		TranslationProfileEditDialog dialog = new TranslationProfileEditDialog(msg,
				msg.getMessage("TranslationProfilesComponent.editAction"),
				p -> updateProfile(p), editor);
		dialog.show();

	}

	private SingleActionHandler<TranslationProfile> getCopyAction()
	{
		return SingleActionHandler.builder4Copy(msg, TranslationProfile.class)
				.withHandler(this::showCopyDialog).build();
	}

	private void showCopyDialog(Collection<TranslationProfile> items)
	{
		TranslationProfile profile = items.iterator().next();
		profile = profile.clone();
		TranslationProfileEditor editor;
		try
		{
			editor = getProfileEditor(profile);
			editor.setCopyMode();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"TranslationProfilesComponent.errorReadData"), e);
			return;
		}
		TranslationProfileEditDialog dialog = new TranslationProfileEditDialog(msg,
				msg.getMessage("TranslationProfilesComponent.copyAction"),
				p -> addProfile(p), editor);
		dialog.show();
	}

	private SingleActionHandler<TranslationProfile> getExportAction()
	{
		return SingleActionHandler.builder(TranslationProfile.class)
				.withCaption(msg.getMessage(
						"TranslationProfilesComponent.exportAction"))
				.withIcon(Images.export.getResource())
				.withHandler(this::exportHandler).multiTarget().build();

	}

	private void exportHandler(Collection<TranslationProfile> items)
	{
		SimpleFileDownloader downloader = new SimpleFileDownloader();
		addExtension(downloader);
		StreamResource resource = null;
		try
		{
			if (items.size() == 1)
			{
				TranslationProfile item = items.iterator().next();
				byte[] content = Constants.MAPPER.writeValueAsBytes(item);
				resource = new StreamResource(
						() -> new ByteArrayInputStream(content),
						item.getName() + ".json");
			} else
			{

				byte[] content = Constants.MAPPER.writeValueAsBytes(items);
				resource = new StreamResource(
						() -> new ByteArrayInputStream(content),
						"translationProfiles.json");
			}
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("TranslationProfilesComponent.errorExport"),
					e);
			return;
		}

		downloader.setFileDownloadResource(resource);
		downloader.download();
	}

	private SingleActionHandler<TranslationProfile> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, TranslationProfile.class)
				.withDisabledPredicate(
						tp -> tp.getProfileMode() == ProfileMode.READ_ONLY)
				.withHandler(this::deleteHandler).build();
	}

	private void deleteHandler(Collection<TranslationProfile> items)
	{
		String confirmText = MessageUtils.createConfirmFromNames(msg, items);
		new ConfirmDialog(msg, msg.getMessage("TranslationProfilesComponent.confirmDelete",
				confirmText), () -> items.forEach(this::removeProfile)).show();
	}

	private SingleActionHandler<TranslationProfile> getWizardAction()
	{
		return SingleActionHandler.builder(TranslationProfile.class)
				.withCaption(msg.getMessage(
						"TranslationProfilesComponent.wizardAction"))
				.withDisabledPredicate(t -> !isInputProfileSelection())
				.withIcon(Images.wizard.getResource())
				.withHandler(this::wizardHandler).hideIfInactive()
				.dontRequireTarget().build();

	}

	private void wizardHandler(Collection<TranslationProfile> items)
	{
		TranslationProfileEditor editor;
		try
		{
			editor = getProfileEditor(null);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"TranslationProfilesComponent.errorReadData"), e);
			return;
		}

		ProfileWizardProvider wizardProvider = new ProfileWizardProvider(msg, sandboxURL,
				sandboxNotifier, editor, t -> addProfile(t));
		SandboxWizardDialog dialog = new SandboxWizardDialog(
				wizardProvider.getWizardInstance(), wizardProvider.getCaption());
		dialog.show();
	}

	private SingleActionHandler<TranslationProfile> getDryRunAction()
	{
		return SingleActionHandler.builder(TranslationProfile.class)
				.withCaption(msg.getMessage(
						"TranslationProfilesComponent.dryrunAction"))
				.withDisabledPredicate(t -> !isInputProfileSelection())
				.withIcon(Images.dryrun.getResource())
				.withHandler(this::dryRunHandler).hideIfInactive()
				.dontRequireTarget().build();
	}

	private void dryRunHandler(Collection<TranslationProfile> items)
	{
		DryRunWizardProvider provider = new DryRunWizardProvider(msg, sandboxURL,
				sandboxNotifier, profileMan, inputActionsRegistry);
		SandboxWizardDialog dialog = new SandboxWizardDialog(provider.getWizardInstance(),
				provider.getCaption());
		dialog.show();
	}

	private boolean isInputProfileSelection()
	{
		boolean isInputProfile = false;
		if (profileType != null)
		{
			if (profileType.getValue() == ProfileType.INPUT)
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
