<#include "headerTop.ftl">


<body>

<script type="text/javascript">
function forceContinue()
{
	document.getElementById("force").value = "true";
	document.getElementById("mainform").submit();
    return true;
}
</script>

<form id="mainform" method="${method}">
    <div>
    	ERROR! It was detected that currently there is an authentication going on with this browser session,
    	most probably in another window or tab of your web browser.
    	<p>
    	You should finish it first, then you can try to continue.
    	</p> 
    	<p>
    	If for whatever reason you want to forcefully stop the previously started authentication and continue
    	with this one, you can force to close the previous session.
    	</p>
    </div>
	<div>
	<#if RelayState??>
		<input type="hidden" name="RelayState" value="${RelayState?xhtml}"/>
	</#if>

	<input type="hidden" name="SAMLRequest" value="${originalRequest?xhtml}"/>
	<input id="force" type="hidden" name="force" value="false"/>
	</div>

	<div>
	
		<input type="submit" value="Try to continue"/>
		<input type="button" value="Forcefully continue" name="button2" onclick="forceContinue()"/>
	</div>
</form>

<#include "footer.ftl">