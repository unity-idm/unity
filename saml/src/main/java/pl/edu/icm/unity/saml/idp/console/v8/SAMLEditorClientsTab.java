/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console.v8;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;

/**
 * SAML service editor clients tab
 * 
 * @author P.Piernik
 *
 */
public class SAMLEditorClientsTab extends CustomComponent implements EditorTab
{
	private MessageSource msg;
	private Binder<SAMLServiceConfiguration> configBinder;
	private SubViewSwitcher subViewSwitcher;
	private PKIManagement pkiMan;
	private UnityServerConfiguration serverConfig;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private IndividualTrustedSPComponent trustedSPs;
	
	public SAMLEditorClientsTab(MessageSource msg, PKIManagement pkiMan, UnityServerConfiguration serverConfig,
			URIAccessService uriAccessService, FileStorageService fileStorageService,
			SubViewSwitcher subViewSwitcher)
	{
		this.msg = msg;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.subViewSwitcher = subViewSwitcher;
		this.pkiMan = pkiMan;
		this.serverConfig = serverConfig;
	}

	public void initUI(Binder<SAMLServiceConfiguration> configBinder)
	{
		this.configBinder = configBinder;
		setCaption(msg.getMessage("IdpServiceEditorBase.clients"));
		setIcon(Images.bullets.getResource());
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setMargin(false);
		CollapsibleLayout sps = buildIndividualTrustedSPsSection();
		sps.expand();
		
		CollapsibleLayout federations = buildTrustedFederationsSection();
		federations.expand();
		federations.setMargin(new MarginInfo(true, false));
		
		mainLayout.addComponent(federations);
		mainLayout.addComponent(sps);
		setCompositionRoot(mainLayout);
	}

	private CollapsibleLayout buildTrustedFederationsSection()
	{
		VerticalLayout trustedFederations = new VerticalLayout();
		trustedFederations.setMargin(false);
		TrustedFederationComponent federations = new TrustedFederationComponent();
		configBinder.forField(federations).bind("trustedFederations");
		trustedFederations.addComponent(federations);

		return new CollapsibleLayout(msg.getMessage("SAMLEditorClientsTab.trustedFederations"),
				trustedFederations);
	}

	private CollapsibleLayout buildIndividualTrustedSPsSection()
	{
		VerticalLayout individualTrustedIdPs = new VerticalLayout();
		individualTrustedIdPs.setMargin(false);
		trustedSPs = new IndividualTrustedSPComponent();
		configBinder.forField(trustedSPs).bind("individualTrustedSPs");
		individualTrustedIdPs.addComponent(trustedSPs);

		return new CollapsibleLayout(msg.getMessage("SAMLEditorClientsTab.individualTrustedSps"),
				individualTrustedIdPs);
	}

	@Override
	public String getType()
	{
		return ServiceEditorTab.CLIENTS.toString();
	}

	@Override
	public CustomComponent getComponent()
	{
		return this;
	}

	private class TrustedFederationComponent extends CustomField<List<SAMLServiceTrustedFederationConfiguration>>
	{
		private GridWithActionColumn<SAMLServiceTrustedFederationConfiguration> federationList;

		public TrustedFederationComponent()
		{
			federationList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
			federationList.addComponentColumn(
					p -> StandardButtonsHelper.buildLinkButton(p.getName(), e -> gotoEdit(p)),
					msg.getMessage("TrustedFederationComponent.name"), 50);
		}

		@Override
		protected Component initContent()
		{
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);
			main.addStyleName(Styles.narrowTable.toString());

			Button add = new Button();
			add.addClickListener(e -> gotoNew());
			add.setIcon(Images.add.getResource());
			main.addComponent(add);
			main.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);
			main.addComponent(federationList);
			return main;
		}

		private List<SingleActionHandler<SAMLServiceTrustedFederationConfiguration>> getActionsHandlers()
		{
			SingleActionHandler<SAMLServiceTrustedFederationConfiguration> edit = SingleActionHandler
					.builder4Edit(msg, SAMLServiceTrustedFederationConfiguration.class).withHandler(r -> {
						SAMLServiceTrustedFederationConfiguration edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<SAMLServiceTrustedFederationConfiguration> remove = SingleActionHandler
					.builder4Delete(msg, SAMLServiceTrustedFederationConfiguration.class)
					.withHandler(r -> {
						federationList.removeElement(r.iterator().next());
						fireChange();
					}).build();

			return Arrays.asList(edit, remove);
		}

		private void gotoNew()
		{
			gotoEditSubView(null, federationList.getElements().stream().map(p -> p.getName())
					.collect(Collectors.toSet()), c -> {
						subViewSwitcher.exitSubViewAndShowUpdateInfo();
						federationList.addElement(c);
						federationList.focus();
						fireChange();
					});
		}

		private void gotoEdit(SAMLServiceTrustedFederationConfiguration edited)
		{
			gotoEditSubView(edited,
					federationList.getElements().stream()
							.filter(p -> p.getName() != edited.getName())
							.map(p -> p.getName()).collect(Collectors.toSet()),
					c -> {
						federationList.replaceElement(edited, c);
						fireChange();
						subViewSwitcher.exitSubViewAndShowUpdateInfo();
					});
		}

		private void gotoEditSubView(SAMLServiceTrustedFederationConfiguration edited, Set<String> usedNames,
				Consumer<SAMLServiceTrustedFederationConfiguration> onConfirm)
		{
			Set<String> validators;
			Set<String> certificates;

			try
			{
				validators = pkiMan.getValidatorNames();
				certificates = pkiMan.getAllCertificateNames();

			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, "Can not init trusted federation editor", e);
				return;
			}

			EditTrustedFederationSubView subView = new EditTrustedFederationSubView(msg, uriAccessService, fileStorageService, edited,
					subViewSwitcher, usedNames, validators, certificates, r -> {
						onConfirm.accept(r);
						federationList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						federationList.focus();
					});
			subViewSwitcher.goToSubView(subView);

		}

		@Override
		public List<SAMLServiceTrustedFederationConfiguration> getValue()
		{
			return federationList.getElements();
		}

		@Override
		protected void doSetValue(List<SAMLServiceTrustedFederationConfiguration> value)
		{
			federationList.setItems(value);
		}

		private void fireChange()
		{
			fireEvent(new ValueChangeEvent<List<SAMLServiceTrustedFederationConfiguration>>(this,
					federationList.getElements(), true));
		}
	}

	private class IndividualTrustedSPComponent extends CustomField<List<SAMLIndividualTrustedSPConfiguration>>
	{
		private GridWithActionColumn<SAMLIndividualTrustedSPConfiguration> spList;

		public IndividualTrustedSPComponent()
		{
			spList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
			spList.addComponentColumn(
					p -> StandardButtonsHelper.buildLinkButton(p.getName(), e -> gotoEdit(p)),
					msg.getMessage("IndividualTrustedSPComponent.name"), 50);
		}

		@Override
		public List<SAMLIndividualTrustedSPConfiguration> getValue()
		{
			return spList.getElements();
		}

		@Override
		protected Component initContent()
		{
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);
			main.addStyleName(Styles.narrowTable.toString());

			Button add = new Button();
			add.addClickListener(e -> gotoNew());
			add.setIcon(Images.add.getResource());
			main.addComponent(add);
			main.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);
			main.addComponent(spList);
			return main;
		}

		private List<SingleActionHandler<SAMLIndividualTrustedSPConfiguration>> getActionsHandlers()
		{
			SingleActionHandler<SAMLIndividualTrustedSPConfiguration> edit = SingleActionHandler
					.builder4Edit(msg, SAMLIndividualTrustedSPConfiguration.class)
					.withHandler(r -> {
						SAMLIndividualTrustedSPConfiguration edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<SAMLIndividualTrustedSPConfiguration> remove = SingleActionHandler
					.builder4Delete(msg, SAMLIndividualTrustedSPConfiguration.class)
					.withHandler(r -> {
						spList.removeElement(r.iterator().next());
						fireChange();
					}).build();

			return Arrays.asList(edit, remove);
		}

		private void gotoNew()
		{
			gotoEditSubView(null,
					spList.getElements().stream().map(p -> p.getName()).collect(Collectors.toSet()),
					c -> {
						subViewSwitcher.exitSubViewAndShowUpdateInfo();
						spList.addElement(c);
						spList.focus();
						fireChange();
					});
		}

		private void gotoEdit(SAMLIndividualTrustedSPConfiguration edited)
		{
			gotoEditSubView(edited,
					spList.getElements().stream().filter(p -> p.getName() != edited.getName())
							.map(p -> p.getName()).collect(Collectors.toSet()),
					c -> {
						spList.replaceElement(edited, c);
						fireChange();
						subViewSwitcher.exitSubViewAndShowUpdateInfo();
					});
		}

		private void gotoEditSubView(SAMLIndividualTrustedSPConfiguration edited, Set<String> usedNames,
				Consumer<SAMLIndividualTrustedSPConfiguration> onConfirm)
		{
			Set<String> certificates;

			try
			{
				certificates = pkiMan.getAllCertificateNames();

			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, "Can not init trusted SP editor", e);
				return;
			}

			EditIndividualTrustedSPSubView subView = new EditIndividualTrustedSPSubView(msg, serverConfig,
					uriAccessService, edited, subViewSwitcher, usedNames, certificates, r -> {
						onConfirm.accept(r);
						spList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						spList.focus();
					});
			subViewSwitcher.goToSubView(subView);

		}

		@Override
		protected void doSetValue(List<SAMLIndividualTrustedSPConfiguration> value)
		{
			spList.setItems(value);
		}

		private void fireChange()
		{
			fireEvent(new ValueChangeEvent<List<SAMLIndividualTrustedSPConfiguration>>(this,
					spList.getElements(), true));
		}

	}

	public Collection<SAMLIndividualTrustedSPConfiguration> getActiveClients()
	{
		return trustedSPs.getValue();
	}

	public void addClientsValueChangeListener(ValueChangeListener<List<SAMLIndividualTrustedSPConfiguration>> listener)
	{
		trustedSPs.addValueChangeListener(listener);
		
	}
}
