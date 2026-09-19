package pl.edu.icm.unity.engine.server;

public interface HttpSessionsService
{
	void invalidateSession(String sessionId);

	void removeAttribute(String sessionId, String attributeName);
}
