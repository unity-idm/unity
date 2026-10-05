/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.otp.HashFunction;
import io.imunity.otp.OTPCredential;
import io.imunity.otp.OTPGenerationParams;
import io.imunity.otp.TOTPCodeGenerator;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

class RedirectedFirstFactorSecondFactorTest extends SeleniumTestBase
{
	private static final String USERNAME = "demo-user";
	private static final String PASSWORD = "the!test12";
	private static final String OTP_SECRET = "JBSWY3DPEHPK3PXP";
	private static final OTPGenerationParams OTP_PARAMS = new OTPGenerationParams(6, HashFunction.SHA1, 30);

	@Autowired
	@Qualifier("insecure")
	private EntityCredentialManagement entityCredentialManagement;

	@BeforeEach
	void setUpOtpCredential() throws Exception
	{
		EntityParam user = new EntityParam(new IdentityTaV(UsernameIdentity.ID, USERNAME));
		OTPCredential credential = new OTPCredential(OTP_SECRET, OTP_PARAMS);
		entityCredentialManagement.setEntityCredential(user, "mfa_otp", JsonUtil.toJsonString(credential));
	}

	@Test
	void shouldCompleteSecondFactorAfterRedirectedFirstFactor()
	{
		openOAuthLoginFromUserHome();
		authenticateWithSamlFirstFactor();
		authenticateWithOtpSecondFactor();
		acceptOAuthConsent();

		assertThat(waitForPageLoad(By.id("MainHeader.loggedAs")).getText()).contains("Demo user");
	}

	private void openOAuthLoginFromUserHome()
	{
		driver.get(baseUrl + "/home");
		waitForPageLoadByURL("/home/authentication");
		waitForPageLoad(By.className("u-idpAuthentication-oauth-local")).click();
		waitForPageLoadByURL("/oauth2-as/authentication");
	}

	private void authenticateWithSamlFirstFactor()
	{
		waitForPageLoad(By.className("u-idpAuthentication-samlMfa-1")).click();
		waitForPageLoadByURL("/saml-idp/authentication");
		waitForElement(By.className("u-passwordUsernameField")).sendKeys(USERNAME);
		waitForElement(By.className("u-passwordField")).sendKeys(PASSWORD);
		waitForElement(By.className("u-passwordSignInButton")).click();
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();
	}

	private void authenticateWithOtpSecondFactor()
	{
		waitForPageLoad(By.className("u-otpCodeField")).sendKeys(generateCurrentOtp());
		waitForElement(By.className("u-otpSignInButton")).click();
	}

	private String generateCurrentOtp()
	{
		return TOTPCodeGenerator.generateTOTP(OTP_SECRET, Instant.now().getEpochSecond(), OTP_PARAMS);
	}

	private void acceptOAuthConsent()
	{
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();
	}
}
