/*
 * Creates 2000 OAuth token pairs (access + refresh) for the configured OAuth IDP
 * and the default oauth-client. Each access token is a JWT so the admin console
 * can display jwtClaims, jwtInfo, idToken, userInfo and all other viewer fields.
 *
 * Uses post-init trigger so that endpoints are already deployed when this runs.
 *
 * Depends on: oauthDemoInitializer.groovy (creates oauth-client entity),
 *             demoContentInitializer.groovy (creates demo-user as token owner)
 */
import groovy.transform.Field
import pl.edu.icm.unity.base.entity.EntityParam
import pl.edu.icm.unity.base.identity.IdentityParam
import pl.edu.icm.unity.oauth.as.ActiveOAuthScopeDefinition
import pl.edu.icm.unity.oauth.as.OAuthToken
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository
import pl.edu.icm.unity.oauth.as.token.access.OAuthRefreshTokenRepository
import pl.edu.icm.unity.stdext.identity.UsernameIdentity

@Field final int NUM_TOKENS = 2000
@Field final String CLIENT_USERNAME = "oauth-client"
@Field final String CLIENT_NAME = "OAuth client"
@Field final String TOKEN_OWNER_USERNAME = "demo-user"
@Field final String OAUTH_TOKEN_ENDPOINT_TYPE = "OAuth2Token"
@Field final String ISSUER_URI_PROP = "unity.oauth2.as.issuerUri"
@Field final int TOKEN_VALIDITY_SECONDS = 3600 * 24 * 30

if (!isColdStart)
{
	log.debug("Database already initialized with content, skipping mass OAuth token creation...")
	return
}

log.info("Creating $NUM_TOKENS OAuth token pairs (access + refresh + id token)...")

try
{
	String issuerUri = resolveIssuerUri()
	if (issuerUri == null)
	{
		log.error("No OAuth2Token endpoint with $ISSUER_URI_PROP found — skipping token creation")
		return
	}
	log.info("OAuth issuerUri: $issuerUri")

	EntityParam clientParam = new EntityParam(new IdentityParam(UsernameIdentity.ID, CLIENT_USERNAME))
	long clientEntityId = entityManagement.getEntity(clientParam).getId()

	EntityParam owner = new EntityParam(new IdentityParam(UsernameIdentity.ID, TOKEN_OWNER_USERNAME))

	int created = 0
	for (int i = 0; i < NUM_TOKENS; i++)
	{
		storeTokenPair(owner, TOKEN_OWNER_USERNAME, clientEntityId, issuerUri, i + 1)
		created++
	}
	log.info("Created $created OAuth token pairs (${created * 2} tokens total)")

} catch (Exception e)
{
	log.warn("Error creating mass OAuth tokens. This can happen and is not critical." +
			" It means that demonstration contents was not loaded to your database," +
			" usually due to conflict with its existing data", e)
}

String resolveIssuerUri()
{
	def oauthEndpoint = endpointManagement.getDeployedEndpoints()
			.find { it.getType().getName() == OAUTH_TOKEN_ENDPOINT_TYPE }
	if (oauthEndpoint == null)
		return null
	String rawConfig = oauthEndpoint.getEndpoint().getConfiguration().getConfiguration()
	Properties props = new Properties()
	props.load(new java.io.StringReader(rawConfig))
	return props.getProperty(ISSUER_URI_PROP)
}

List<RequestedOAuthScope> buildScopes()
{
	return [new RequestedOAuthScope("openid",
			ActiveOAuthScopeDefinition.builder().withName("openid").withAttributes([]).build(), false)]
}

/**
 * Builds a JWT string that SignedJWT.parse() can decode without signature verification.
 * The viewer calls tryParseJWT(accessToken) to populate jwtClaims and jwtInfo fields.
 * Key stored in the DB is the jti, matching OAuthAccessTokenRepository.getTokenUniqueKey() behaviour.
 */
String buildJwtAccessToken(String jti, String subject, String issuerUri, long iatSec, long expSec)
{
	def b64 = java.util.Base64.getUrlEncoder().withoutPadding()
	String header = '{"alg":"RS256","typ":"JWT"}'
	String payload = """{"jti":"$jti","sub":"$subject","iss":"$issuerUri","aud":["$CLIENT_USERNAME"],""" +
			""""iat":$iatSec,"exp":$expSec,"scope":"openid"}"""
	return b64.encodeToString(header.bytes) + "." +
			b64.encodeToString(payload.bytes) + "." +
			b64.encodeToString("fakesig".bytes)
}

/**
 * userInfo is parsed by the viewer as OIDC UserInfo claims and rendered field-by-field.
 * Must be valid JSON parseable by UserInfo.parse().
 */
String buildUserInfo(String subject, String issuerUri)
{
	return """{"sub":"$subject","name":"Demo User","email":"demo@example.com",""" +
			""""email_verified":true,"iss":"$issuerUri"}"""
}

/**
 * openidInfo is the raw ID token claims JSON. Viewer displays it verbatim in the idToken field.
 */
String buildOpenidInfo(String subject, String issuerUri, long iatSec, long expSec)
{
	return """{"sub":"$subject","iss":"$issuerUri","aud":["$CLIENT_USERNAME"],""" +
			""""iat":$iatSec,"exp":$expSec,"auth_time":$iatSec}"""
}

void storeTokenPair(EntityParam owner, String subject, long clientEntityId, String issuerUri, int tokenNumber)
{
	String jti = UUID.randomUUID().toString()
	String refreshTokenValue = UUID.randomUUID().toString()

	Date now = new Date()
	long iatSec = now.getTime() / 1000
	long expSec = iatSec + TOKEN_VALIDITY_SECONDS

	String jwtAccessToken = buildJwtAccessToken(jti, subject, issuerUri, iatSec, expSec)

	OAuthToken token = new OAuthToken()
	token.setSubject(subject)
	token.setClientId(clientEntityId)
	token.setClientUsername(CLIENT_USERNAME)
	token.setClientName("$CLIENT_NAME #$tokenNumber")
	token.setIssuerUri(issuerUri)
	token.setTokenValidity(TOKEN_VALIDITY_SECONDS)
	token.setMaxExtendedValidity(TOKEN_VALIDITY_SECONDS)
	token.setResponseType("code")
	token.setRequestedScope(["openid"] as String[])
	token.setEffectiveScope(buildScopes())
	token.setAudience([CLIENT_USERNAME])
	token.setRedirectUri("https://localhost:2443/unitygw/oauth2ResponseConsumer")
	token.setAccessToken(jwtAccessToken)
	token.setRefreshToken(refreshTokenValue)
	token.setFirstRefreshRollingToken(refreshTokenValue)
	token.setOpenidToken(buildOpenidInfo(subject, issuerUri, iatSec, expSec))
	token.setUserInfo(buildUserInfo(subject, issuerUri))

	Date accessExpiration = new Date(expSec * 1000L)
	Date refreshExpiration = new Date(now.getTime() + TOKEN_VALIDITY_SECONDS * 2 * 1000L)

	// DB key for JWT access tokens is the jti (mirrors OAuthAccessTokenRepository.getTokenUniqueKey)
	tokensManagement.addToken(OAuthAccessTokenRepository.INTERNAL_ACCESS_TOKEN, jti,
			owner, token.getSerialized(), now, accessExpiration)
	tokensManagement.addToken(OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN, refreshTokenValue,
			owner, token.getSerialized(), now, refreshExpiration)
}
