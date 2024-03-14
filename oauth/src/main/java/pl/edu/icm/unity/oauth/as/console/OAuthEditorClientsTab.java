/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.function.SerializablePredicate;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.ServiceEditorComponent;
import io.imunity.vaadin.auth.services.idp.GroupWithIndentIndicator;
import io.imunity.vaadin.auth.services.idp.MandatoryGroupSelection;
import io.imunity.vaadin.auth.services.tabs.GroupedValuesChipsWithDropdown;
import io.imunity.vaadin.auth.services.tabs.WebServiceAuthenticationTab;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.oauth.as.console.OAuthClient.OAuthClientsBean;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

/**
 * OAuth service editor clients tab.
 * 
 * @author P.Piernik
 *
 */
class OAuthEditorClientsTab extends VerticalLayout implements ServiceEditorBase.EditorTab
{
	private final MessageSource msg;
	private final UnityServerConfiguration serverConfig;
	private final SubViewSwitcher subViewSwitcher;
	private final NotificationPresenter notificationPresenter;

	private Binder<DefaultServiceDefinition> oauthTokenBinder;
	private Binder<OAuthServiceConfiguration> configBinder;
	private Binder<OAuthClientsBean> clientsBinder;

	private final List<String> allRealms;
	private final List<String> flows;
	private final List<String> authenticators;
	private final List<String> allUsernames;
	private final Supplier<Set<String>> scopesSupplier;
	private List<Group> groups;

	private MandatoryGroupSelection groupCombo;


	OAuthEditorClientsTab(MessageSource msg, UnityServerConfiguration serverConfig,
			SubViewSwitcher subViewSwitcher,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			List<String> allRealms, List<String> allUsernames,  Supplier<Set<String>> scopesSupplier, String binding,
			NotificationPresenter notificationPresenter)
	{
		this.subViewSwitcher = subViewSwitcher;
		this.msg = msg;
		this.serverConfig = serverConfig;
		this.allRealms = allRealms;
		this.flows = WebServiceAuthenticationTab.filterBindingCompatibleAuthenticationFlow(flows, authenticators, binding);
		this.authenticators = authenticators.stream().filter(a -> a.getSupportedBindings().contains(binding))
				.map(AuthenticatorInfo::getId).collect(Collectors.toList());
		this.allUsernames = allUsernames;
		this.scopesSupplier = scopesSupplier;
		this.notificationPresenter = notificationPresenter;
	}

	void initUI(List<Group> groups, Binder<DefaultServiceDefinition> oauthTokenBinder,
			Binder<OAuthServiceConfiguration> configBinder, Binder<OAuthClientsBean> clientsBinder)
	{
		this.groups = groups;
		this.oauthTokenBinder = oauthTokenBinder;
		this.configBinder = configBinder;
		this.clientsBinder = clientsBinder;
		setPadding(false);
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setPadding(false);
		mainLayout.add(buildClientsSection());
		mainLayout.add(buildAuthenticationSection());
		add(mainLayout);
	}

	private Component buildClientsSection()
	{
		VerticalLayout mainClientLayout = new VerticalLayout();
		mainClientLayout.setPadding(false);
		ClientsComponent clients = new ClientsComponent();
		VerticalLayout clientsWrapper = new VerticalLayout();
		clientsWrapper.setPadding(false);
		clientsWrapper.add(clients);
		FormLayout comboWrapper = new FormLayout();
		groupCombo = new MandatoryGroupSelection(msg);
		groupCombo.setWidth(TEXT_FIELD_BIG.value());
		groupCombo.setItems(groups);
		groupCombo.setRequiredIndicatorVisible(false);
		configBinder.forField(groupCombo).bind("clientGroup");
		groupCombo.addValueChangeListener(e -> {
			clients.filterGroup(e.getValue().group().toString());
		});
		groupCombo.setGroupChangeConfirmationQuestion(
				msg.getMessage("OAuthEditorClientsTab.groupChangeConfirmationQuestion"));
		comboWrapper.addFormItem(groupCombo, msg.getMessage("OAuthEditorClientsTab.clientsGroup"));
		mainClientLayout.add(comboWrapper);
		mainClientLayout.add(clientsWrapper);
		clientsBinder.forField(clients).bind("clients");
		return mainClientLayout;
	}

	private Component buildAuthenticationSection()
	{
		FormLayout mainAuthenticationLayout = new FormLayout();
		mainAuthenticationLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		mainAuthenticationLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		Select<String> realm = new Select<>();
		realm.setItems(allRealms);
		realm.setEmptySelectionAllowed(false);
		oauthTokenBinder.forField(realm).asRequired().bind("realm");
		mainAuthenticationLayout.addFormItem(realm, msg.getMessage("ServiceEditorBase.realm"));

		Map<String, List<String>> labels = new HashMap<>();
		labels.put(msg.getMessage("ServiceEditorBase.flows"), flows);
		labels.put(msg.getMessage("ServiceEditorBase.authenticators"), authenticators);
		GroupedValuesChipsWithDropdown authAndFlows = new GroupedValuesChipsWithDropdown(labels);
		oauthTokenBinder.forField(authAndFlows).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}

			return ValidationResult.ok();

		})
				.withConverter(List::copyOf, HashSet::new)
				.bind(DefaultServiceDefinition::getAuthenticationOptions, DefaultServiceDefinition::setAuthenticationOptions);
		authAndFlows.setRequiredIndicatorVisible(true);
		mainAuthenticationLayout.addFormItem(authAndFlows, msg.getMessage("ServiceEditorBase.authenticatorsAndFlows"));

		AccordionPanel authSection = new AccordionPanel(
				msg.getMessage("OAuthEditorClientsTab.authentication"), mainAuthenticationLayout);
		authSection.setOpened(true);
		return authSection;
	}

	public void refreshGroups()
	{
		groupCombo.refreshCaptions();
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

	private class ClientsComponent extends CustomField<List<OAuthClient>>
	{
		private final SerializablePredicate<OAuthClient> removedFilter = c -> !c.isToRemove();
		private GridWithActionColumn<OAuthClient> clientsList;
		private String group;

		public ClientsComponent()
		{
			setWidthFull();
			initUI();
		}

		@Override
		protected List<OAuthClient> generateModelValue()
		{
			return getValue();
		}

		@Override
		protected void setPresentationValue(List<OAuthClient> oAuthClients)
		{
			setValue(oAuthClients);
		}

		private void initUI()
		{
			VerticalLayout main = new VerticalLayout();
			main.setPadding(false);
			main.setSpacing(false);
			main.setWidthFull();

			Button add = new Button(msg.getMessage("create"));
			add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			add.addClickListener(e -> gotoNew());
			add.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
			main.add(add);
			main.setAlignItems(Alignment.END);

			clientsList = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
			clientsList.setWidthFull();
			clientsList.setAllRowsVisible(true);
			clientsList.addComponentColumn(
					p -> new LinkButton(p.getName(), e -> gotoEdit(p))
			).setHeader(msg.getMessage("ClientsComponent.name"));
			
			clientsList.addColumn(OAuthClient::getType)
					.setHeader(msg.getMessage("ClientsComponent.type"))
					.setAutoWidth(true);
			clientsList.addColumn(p -> p.getFlows() != null ? String.join(",", p.getFlows()) : "")
					.setHeader(msg.getMessage("ClientsComponent.enabledGrants"))
					.setAutoWidth(true);
			clientsList.addFilter(removedFilter);
			main.add(clientsList);
			add(main);
		}

		private List<SingleActionHandler<OAuthClient>> getActionsHandlers()
		{
			SingleActionHandler<OAuthClient> edit = SingleActionHandler.builder4Edit(msg::getMessage, OAuthClient.class)
					.withHandler(r -> {
						OAuthClient edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<OAuthClient> remove = SingleActionHandler
					.builder4Delete(msg::getMessage, OAuthClient.class).withHandler(r -> {

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
			EditOAuthClientSubView subView = new EditOAuthClientSubView(msg, serverConfig,
					getClientsIds(edited), scopesSupplier, edited, c -> {
						onConfirm.accept(c);
						fireChange();
						clientsList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						clientsList.focus();
					}, notificationPresenter);
			subViewSwitcher.goToSubView(subView);
		}

		private Set<String> getClientsIds(OAuthClient edited)
		{
			Set<String> clients = new HashSet<>();
			clients.addAll(clientsList.getElements().stream().filter(c -> c.getEntity() == null)
					.map(OAuthClient::getId).collect(Collectors.toSet()));
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
		public void setValue(List<OAuthClient> value)
		{
			clientsList.setItems(value);

		}

		private void fireChange()
		{
			fireEvent(new ComponentValueChangeEvent<>(this, this, clientsList.getElements(), true));
		}

		public void filterGroup(String path)
		{
			group = path;
			clientsList.clearFilters();
			clientsList.addFilter(removedFilter);
			clientsList.addFilter(c -> c.getGroup().equals(path));
		}
	}

	public void addGroupValueChangeListener(Consumer<GroupWithIndentIndicator> listener)
	{
		groupCombo.addValueChangeListener(event ->  listener.accept(event.getValue()));
	}

	public List<OAuthClient> getActiveClients()
	{
		return clientsBinder.getBean().getClients().stream().filter(
				c -> !c.isToRemove() && c.getGroup().equals(groupCombo.getValue().group().toString()))
				.collect(Collectors.toList());
	}
}
