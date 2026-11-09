package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.stdext.attr.StringAttribute;

@Tag("introspection")
class AttributeIntrospectionTest extends SamlWorkflowTestBase
{
	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement types;

	@Test
	void shouldProcessReceivedPolicyAttributeWithoutDescription() throws Exception
	{
		TestUser user = createUser("Undescribed policy attribute owner");
		deploySaml("", List.of());
		deployAt("AttributeIntrospection", "/introspection", """
				unity.attrintrospection.defaultPolicyAttributes.1=\
				{"name":"name","mandatory":true}
				""", List.of("ui-saml"));
		driver.get(baseUrl + "/introspection");
		VaadinPage page = new VaadinPage(driver);
		page.click(PROVIDER);
		page.signIn(user.username(), user.password());
		page.click(By.className("u-consentConfirmButton"));
		page.visible(By.id("PolicyProcessingSummaryComponent.TryAgain"));
		assertThat(driver.findElement(By.tagName("body")).getText()).contains("All mandatory attributes were provided");
	}

	@Test
	void shouldExplainMissingAttributesAndRefreshResultsAfterAnotherAuthentication() throws Exception
	{
		TestUser user = createUser("Attribute introspection owner");
		deploySaml("", List.of());
		types.addAttributeType(new AttributeType("ui-mandatory", "string"));
		deployAt("AttributeIntrospection", "/introspection", """
				unity.attrintrospection.customAttributePolicies.1.policyName=Selenium requirements
				unity.attrintrospection.customAttributePolicies.1.targetIdps=https://localhost:2443/ui-saml-idp
				unity.attrintrospection.customAttributePolicies.1.attributes.1=\
				{"name":"name","description":"Name","mandatory":true}
				unity.attrintrospection.customAttributePolicies.1.attributes.2=\
				{"name":"ui-mandatory","description":"Required","mandatory":true}
				unity.attrintrospection.customAttributePolicies.1.attributes.3=\
				{"name":"ui-optional","description":"Optional","mandatory":false}
				""", List.of("ui-saml"));
		driver.get(baseUrl + "/introspection");
		VaadinPage page = new VaadinPage(driver);
		page.click(PROVIDER);
		page.signIn(user.username(), user.password());
		page.click(By.className("u-consentConfirmButton"));
		page.visible(By.id("PolicyProcessingSummaryComponent.TryAgain"));
		assertThat(driver.findElement(By.tagName("body")).getText()).contains(
				"50% (1/2) mandatory attributes were provided", "0% (0/1) optional attributes were provided",
				"Missing mandatory attributes", "ui-mandatory", "Missing optional attributes", "ui-optional");

		attributes.setAttribute(user.entity(), StringAttribute.of("ui-mandatory", "/", "Received after retry"));
		page.click(By.id("PolicyProcessingSummaryComponent.TryAgain"));
		page.click(PROVIDER);
		page.click(By.className("u-consentConfirmButton"));
		page.visible(By.id("PolicyProcessingSummaryComponent.TryAgain"));
		assertThat(driver.findElement(By.tagName("body")).getText()).contains(
				"All mandatory attributes were provided", "0% (0/1) optional attributes were provided")
				.doesNotContain("Missing mandatory attributes");
		assertThat(driver.findElement(By.tagName("body")).getText()).contains("Received after retry");
	}
}
