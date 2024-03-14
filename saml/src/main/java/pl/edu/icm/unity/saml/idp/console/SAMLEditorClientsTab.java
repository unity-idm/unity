/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.ServiceEditorComponent;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * SAML service editor clients tab
 * 
 * @author P.Piernik
 *
 */
public class SAMLEditorClientsTab extends VerticalLayout implements ServiceEditorBase.EditorTab
{
	private final MessageSource msg;
	private Binder<SAMLServiceConfiguration> configBinder;
	private final SubViewSwitcher subViewSwitcher;
	private final PKIManagement pkiMan;
	private final UnityServerConfiguration serverConfig;
	private final URIAccessService uriAccessService;
	private final FileStorageService fileStorageService;
	private IndividualTrustedSPComponent trustedSPs;
	private NotificationPresenter notificationPresenter;

	public SAMLEditorClientsTab(MessageSource msg, PKIManagement pkiMan, UnityServerConfiguration serverConfig,
			URIAccessService uriAccessService, FileStorageService fileStorageService,
			SubViewSwitcher subViewSwitcher, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.subViewSwitcher = subViewSwitcher;
		this.pkiMan = pkiMan;
		this.serverConfig = serverConfig;
		this.notificationPresenter = notificationPresenter;
	}

	public void initUI(Binder<SAMLServiceConfiguration> configBinder)
	{
		this.configBinder = configBinder;
		setPadding(false);
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setPadding(false);
		AccordionPanel sps = buildIndividualTrustedSPsSection();
		sps.setWidthFull();
		sps.setOpened(true);

		AccordionPanel federations = buildTrustedFederationsSection();
		federations.setWidthFull();
		federations.setOpened(true);

		mainLayout.add(federations);
		mainLayout.add(sps);
		add(mainLayout);
	}

	private AccordionPanel buildTrustedFederationsSection()
	{
		VerticalLayout trustedFederations = new VerticalLayout();
		trustedFederations.setMargin(false);
		TrustedFederationComponent federations = new TrustedFederationComponent();
		federations.setWidthFull();
		configBinder.forField(federations)
				.bind(SAMLServiceConfiguration::getTrustedFederations, SAMLServiceConfiguration::setTrustedFederations);
		trustedFederations.add(federations);

		return new AccordionPanel(msg.getMessage("SAMLEditorClientsTab.trustedFederations"),
				trustedFederations);
	}

	private AccordionPanel buildIndividualTrustedSPsSection()
	{
		VerticalLayout individualTrustedIdPs = new VerticalLayout();
		individualTrustedIdPs.setMargin(false);
		trustedSPs = new IndividualTrustedSPComponent();
		trustedSPs.setWidthFull();
		configBinder.forField(trustedSPs)
				.bind(SAMLServiceConfiguration::getIndividualTrustedSPs, SAMLServiceConfiguration::setIndividualTrustedSPs);
		individualTrustedIdPs.add(trustedSPs);

		return new AccordionPanel(msg.getMessage("SAMLEditorClientsTab.individualTrustedSps"),
				individualTrustedIdPs);
	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.BULLETS;
	}

	@Override
	public String getType()
	{
		return ServiceEditorComponent.ServiceEditorTab.CLIENTS.toString();
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("IdpServiceEditorBase.clients");
	}

	private class TrustedFederationComponent extends CustomField<List<SAMLServiceTrustedFederationConfiguration>>
	{
		private GridWithActionColumn<SAMLServiceTrustedFederationConfiguration> federationList;

		public TrustedFederationComponent()
		{
			federationList = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
			federationList.addComponentColumn(
					p ->
					{
						Button button = new Button(p.getName(), e -> gotoEdit(p));
						button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
						return button;
					})
							.setHeader(msg.getMessage("TrustedFederationComponent.name"))
							.setAutoWidth(true);
			initContent();
		}

		@Override
		protected List<SAMLServiceTrustedFederationConfiguration> generateModelValue()
		{
			return null;
		}

		@Override
		protected void setPresentationValue(
				List<SAMLServiceTrustedFederationConfiguration> samlServiceTrustedFederationConfigurations)
		{

		}

		private void initContent()
		{
			VerticalLayout main = new VerticalLayout();
			main.setAlignItems(Alignment.END);
			main.setPadding(false);
			Button button = new Button(msg.getMessage("SAMLEditorClientsTab.trustedFederations.button"),
					VaadinIcon.PLUS_CIRCLE_O.create());
			button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			button.addClickListener(e -> gotoNew());
			main.add(button);
			main.add(federationList);
			add(main);
		}

		private List<SingleActionHandler<SAMLServiceTrustedFederationConfiguration>> getActionsHandlers()
		{
			SingleActionHandler<SAMLServiceTrustedFederationConfiguration> edit = SingleActionHandler
					.builder4Edit(msg::getMessage, SAMLServiceTrustedFederationConfiguration.class).withHandler(r -> {
						SAMLServiceTrustedFederationConfiguration edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<SAMLServiceTrustedFederationConfiguration> remove = SingleActionHandler
					.builder4Delete(msg::getMessage, SAMLServiceTrustedFederationConfiguration.class)
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
							.map(SAMLServiceTrustedFederationConfiguration::getName)
							.filter(name -> !Objects.equals(name, edited.getName())).collect(Collectors.toSet()),
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
				notificationPresenter.showError( "Can not init trusted federation editor", e.getMessage());
				return;
			}

			EditTrustedFederationSubView subView = new EditTrustedFederationSubView(msg, uriAccessService, fileStorageService, edited,
					subViewSwitcher, usedNames, validators, certificates, r -> {
						onConfirm.accept(r);
						federationList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						federationList.focus();
					}, notificationPresenter);
			subViewSwitcher.goToSubView(subView);

		}

		@Override
		public List<SAMLServiceTrustedFederationConfiguration> getValue()
		{
			return federationList.getElements();
		}

		@Override
		public void setValue(List<SAMLServiceTrustedFederationConfiguration> value)
		{
			federationList.setItems(value);
		}

		private void fireChange()
		{
			fireEvent(new ComponentValueChangeEvent<>(this, this,
					federationList.getElements(), true));
		}
	}

	private class IndividualTrustedSPComponent extends CustomField<List<SAMLIndividualTrustedSPConfiguration>>
	{
		private GridWithActionColumn<SAMLIndividualTrustedSPConfiguration> spList;

		public IndividualTrustedSPComponent()
		{
			spList = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
			spList.addComponentColumn(p ->
					{
						Button button = new Button(p.getName(), e -> gotoEdit(p));
						button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
						return button;
					}).setHeader(msg.getMessage("IndividualTrustedSPComponent.name"))
					.setAutoWidth(true);
			initContent();
		}

		@Override
		protected List<SAMLIndividualTrustedSPConfiguration> generateModelValue()
		{
			return null;
		}

		@Override
		public void setPresentationValue(
				List<SAMLIndividualTrustedSPConfiguration> samlIndividualTrustedSPConfigurations)
		{
			spList.setItems(samlIndividualTrustedSPConfigurations);
		}

		@Override
		public List<SAMLIndividualTrustedSPConfiguration> getValue()
		{
			return spList.getElements();
		}

		private void initContent()
		{
			VerticalLayout main = new VerticalLayout();
			main.setAlignItems(Alignment.END);
			main.setMargin(false);
			Button button = new Button(msg.getMessage("SAMLEditorClientsTab.individualTrustedSps.button"),
					VaadinIcon.PLUS_CIRCLE_O.create());
			button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			button.addClickListener(e -> gotoNew());
			main.add(button, spList);
			add(main);
		}

		private List<SingleActionHandler<SAMLIndividualTrustedSPConfiguration>> getActionsHandlers()
		{
			SingleActionHandler<SAMLIndividualTrustedSPConfiguration> edit = SingleActionHandler
					.builder4Edit(msg::getMessage, SAMLIndividualTrustedSPConfiguration.class)
					.withHandler(r -> {
						SAMLIndividualTrustedSPConfiguration edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<SAMLIndividualTrustedSPConfiguration> remove = SingleActionHandler
					.builder4Delete(msg::getMessage, SAMLIndividualTrustedSPConfiguration.class)
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
					spList.getElements().stream().map(SAMLIndividualTrustedSPConfiguration::getName)
							.filter(name -> !Objects.equals(name, edited.getName())).collect(Collectors.toSet()),
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
				notificationPresenter.showError("Can not init trusted SP editor", e.getMessage());
				return;
			}

			EditIndividualTrustedSPSubView subView = new EditIndividualTrustedSPSubView(msg, serverConfig,
					uriAccessService, edited, subViewSwitcher, usedNames, certificates, r -> {
						onConfirm.accept(r);
						spList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						spList.focus();
					}, notificationPresenter);
			subViewSwitcher.goToSubView(subView);

		}

		private void fireChange()
		{
			fireEvent(new ComponentValueChangeEvent<>(this, this,
					spList.getElements(), true));
		}

	}

	public Collection<SAMLIndividualTrustedSPConfiguration> getActiveClients()
	{
		return trustedSPs.getValue();
	}

	public void addClientsValueChangeListener(
			Consumer<List<SAMLIndividualTrustedSPConfiguration>> listener)
	{
		trustedSPs.addValueChangeListener(e -> listener.accept(e.getValue()));
		
	}
}
