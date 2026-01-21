<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/2002/REC-xhtml1-20020801/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
<title>SAML HTTP POST binding</title>
</head>
<body onload="document.forms[0].submit()">
<noscript>
	<p>
	<strong>Note:</strong> Since your browser does not support JavaScript,
	you must press the Continue button once to proceed.
	</p>
</noscript>
<form action="${destinationURL}" method="post">
	<div>
	<#if relayState??>
		<input type="hidden" name="RelayState" value="${relayState}"/>
	</#if>
	<input type="hidden" name="${messageType}" value="${samlMessage}"/>
	</div>
	<noscript>
		<div>
		<input type="submit" value="Continue"/>
		</div>
	</noscript>
</form>
</body>
</html>
