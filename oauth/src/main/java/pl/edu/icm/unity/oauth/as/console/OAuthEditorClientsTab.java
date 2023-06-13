/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.data.ValidationResult;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.oauth.as.console.OAuthClient.OAuthClientsBean;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.chips.GroupedValuesChipsWithDropdown;
import pl.edu.icm.unity.webui.common.groups.GroupWithIndentIndicator;
import pl.edu.icm.unity.webui.common.groups.MandatoryGroupSelection;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.console.services.tabs.WebServiceAuthenticationTab;

/**
 * OAuth service editor clients tab.
 * 
 * @author P.Piernik
 *
 */
class OAuthEditorClientsTab extends CustomComponent implements EditorTab
{
	private MessageSource msg;
	private URIAccessService uriAccessService;
	private UnityServerConfiguration serverConfig;
	private SubViewSwitcher subViewSwitcher;

	private Binder<DefaultServiceDefinition> oauthTokenBinder;
	private Binder<OAuthServiceConfiguration> configBinder;
	private Binder<OAuthClientsBean> clientsBinder;

	private List<String> allRealms;
	private List<String> flows;
	private List<String> authenticators;
	private List<Group> groups;
	private List<String> allUsernames;

	private MandatoryGroupSelection groupCombo;

	private Supplier<Set<String>> scopesSupplier;
	
	OAuthEditorClientsTab(MessageSource msg, UnityServerConfiguration serverConfig,
			URIAccessService uriAccessService, SubViewSwitcher subViewSwitcher,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			List<String> allRealms, List<String> allUsernames,  Supplier<Set<String>> scopesSupplier, String binding)
	{
		this.subViewSwitcher = subViewSwitcher;
		this.msg = msg;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
		this.allRealms = allRealms;
		this.flows = WebServiceAuthenticationTab.filterBindingCompatibleAuthenticationFlow(flows, authenticators, binding);
		this.authenticators = authenticators.stream().filter(a -> a.getSupportedBindings().contains(binding))
				.map(a -> a.getId()).collect(Collectors.toList());
		this.allUsernames = allUsernames;
		this.scopesSupplier = scopesSupplier;
	}

	void initUI(List<Group> groups, Binder<DefaultServiceDefinition> oauthTokenBinder,
			Binder<OAuthServiceConfiguration> configBinder, Binder<OAuthClientsBean> clientsBinder)
	{
		this.groups = groups;
		this.oauthTokenBinder = oauthTokenBinder;
		this.configBinder = configBinder;
		this.clientsBinder = clientsBinder;
		setCaption(msg.getMessage("IdpServiceEditorBase.clients"));
		setIcon(Images.bullets.getResource());
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setMargin(false);
		mainLayout.addComponent(buildClientsSection());
		mainLayout.addComponent(buildAuthenticationSection());
		setCompositionRoot(mainLayout);
	}

	private Component buildClientsSection()
	{
		VerticalLayout mainClientLayout = new VerticalLayout();
		mainClientLayout.setMargin(false);
		ClientsComponent clients = new ClientsComponent();
		VerticalLayout clientsWrapper = new VerticalLayout();
		clientsWrapper.setMargin(true);
		clientsWrapper.addComponent(clients);
		FormLayoutWithFixedCaptionWidth comboWrapper = new FormLayoutWithFixedCaptionWidth();
		groupCombo = new MandatoryGroupSelection(msg);
		groupCombo.setWidth(30, Unit.EM);
		groupCombo.setCaption(msg.getMessage("OAuthEditorClientsTab.clientsGroup"));
		groupCombo.setItems(groups);
		groupCombo.setRequiredIndicatorVisible(false);
		configBinder.forField(groupCombo).bind("clientGroup");
		groupCombo.addValueChangeListener(e -> {
			clients.filterGroup(e.getValue().group.toString());
		});
		groupCombo.setGroupChangeConfirmationQuestion(
				msg.getMessage("OAuthEditorClientsTab.groupChangeConfirmationQuestion"));
		comboWrapper.addComponent(groupCombo);
		mainClientLayout.addComponent(comboWrapper);
		mainClientLayout.addComponent(clientsWrapper);
		clientsBinder.forField(clients).bind("clients");
		return mainClientLayout;
	}

	private Component buildAuthenticationSection()
	{
		FormLayoutWithFixedCaptionWidth mainAuthenticationLayout = new FormLayoutWithFixedCaptionWidth();
		ComboBox<String> realm = new ComboBox<>();
		realm.setCaption(msg.getMessage("ServiceEditorBase.realm"));
		realm.setItems(allRealms);
		realm.setEmptySelectionAllowed(false);
		oauthTokenBinder.forField(realm).asRequired().bind("realm");
		mainAuthenticationLayout.addComponent(realm);

		Map<String, List<String>> labels = new HashMap<>();
		labels.put(msg.getMessage("ServiceEditorBase.flows"), flows);
		labels.put(msg.getMessage("ServiceEditorBase.authenticators"), authenticators);
		GroupedValuesChipsWithDropdown authAndFlows = new GroupedValuesChipsWithDropdown(labels);
		authAndFlows.setCaption(msg.getMessage("ServiceEditorBase.authenticatorsAndFlows"));
		oauthTokenBinder.forField(authAndFlows).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}

			return ValidationResult.ok();

		}).bind("authenticationOptions");
		authAndFlows.setRequiredIndicatorVisible(true);
		mainAuthenticationLayout.addComponent(authAndFlows);

		CollapsibleLayout authSection = new CollapsibleLayout(
				msg.getMessage("OAuthEditorClientsTab.authentication"), mainAuthenticationLayout);
		authSection.expand();
		return authSection;
	}

	public void refreshGroups()
	{
		groupCombo.refreshCaptions();
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

	private class ClientsComponent extends CustomField<List<OAuthClient>>
	{
		private GridWithActionColumn<OAuthClient> clientsList;
		private VerticalLayout main;
		private SerializablePredicate<OAuthClient> removedFilter = c -> !c.isToRemove();
		private String group;

		public ClientsComponent()
		{
			initUI();
		}

		private void initUI()
		{
			main = new VerticalLayout();
			main.setMargin(false);

			Button add = new Button(msg.getMessage("create"));
			add.addClickListener(e -> {
				setComponentError(null);
				gotoNew();
			});
			add.setIcon(Images.add.getResource());
			add.setStyleName(Styles.buttonAction.toString());
			main.addComponent(add);
			main.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);

			clientsList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
			
			clientsList.addComponentColumn(
					p -> StandardButtonsHelper.buildLinkButton(p.getName(), e -> gotoEdit(p)),
					msg.getMessage("ClientsComponent.name"), 20);
			
			clientsList.addColumn(p -> p.getType(), msg.getMessage("ClientsComponent.type"), 30);
			clientsList.addColumn(p -> p.getFlows() != null ? String.join(",", p.getFlows()) : "",
					msg.getMessage("ClientsComponent.enabledGrants"), 40);
			clientsList.addFilter(removedFilter);
			main.addComponent(clientsList);
		}

		private List<SingleActionHandler<OAuthClient>> getActionsHandlers()
		{
			SingleActionHandler<OAuthClient> edit = SingleActionHandler.builder4Edit(msg, OAuthClient.class)
					.withHandler(r -> {
						OAuthClient edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<OAuthClient> remove = SingleActionHandler
					.builder4Delete(msg, OAuthClient.class).withHandler(r -> {

						OAuthClient client = r.iterator().next();
						if (client.getEntity() == null)
						{
							clientsList.removeElement(client);
						} else
						{
							client.setToRemove(true);
						}

						filterGroup(group);
						fireChange();
					}).build();

			return Arrays.asList(edit, remove);
		}

		private void gotoNew()
		{
			gotoEditSubView(null, c -> {
				subViewSwitcher.exitSubViewAndShowUpdateInfo();
				c.setGroup(group);
				clientsList.addElement(c);
			});

		}

		private void gotoEdit(OAuthClient edited)
		{
			gotoEditSubView(edited, c -> {
				c.setUpdated(true);
				clientsList.replaceElement(edited, c);
				subViewSwitcher.exitSubViewAndShowUpdateInfo();
			});
		}

		private void gotoEditSubView(OAuthClient edited, Consumer<OAuthClient> onConfirm)
		{
			EditOAuthClientSubView subView = new EditOAuthClientSubView(msg, uriAccessService, serverConfig,
					getClientsIds(edited), scopesSupplier, edited, c -> {
						onConfirm.accept(c);
						fireChange();
						clientsList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						clientsList.focus();
					});
			subViewSwitcher.goToSubView(subView);
		}

		private Set<String> getClientsIds(OAuthClient edited)
		{
			Set<String> clients = new HashSet<>();
			clients.addAll(clientsList.getElements().stream().filter(c -> c.getEntity() == null)
					.map(c -> c.getId()).collect(Collectors.toSet()));
			clients.addAll(allUsernames);
			if (edited != null)
			{
				clients.remove(edited.getId());
			}
			return clients;
		}

		@Override
		public List<OAuthClient> getValue()
		{
			return clientsList.getElements();
		}

		@Override
		protected Component initContent()
		{
			return main;
		}

		@Override
		protected void doSetValue(List<OAuthClient> value)
		{
			clientsList.setItems(value);

		}

		private void fireChange()
		{
			fireEvent(new ValueChangeEvent<List<OAuthClient>>(this, clientsList.getElements(), true));
		}

		public void filterGroup(String path)
		{
			group = path;
			clientsList.clearFilters();
			clientsList.addFilter(removedFilter);
			clientsList.addFilter(c -> c.getGroup().equals(path));
		}
	}

	public void addGroupValueChangeListener(ValueChangeListener<GroupWithIndentIndicator> listener)
	{
		groupCombo.addValueChangeListener(listener);
	}

	public List<OAuthClient> getActiveClients()
	{
		return clientsBinder.getBean().getClients().stream().filter(
				c -> !c.isToRemove() && c.getGroup().equals(groupCombo.getValue().group.toString()))
				.collect(Collectors.toList());
	}
}
