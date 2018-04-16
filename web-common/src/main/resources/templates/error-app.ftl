<!DOCTYPE html>
<html>
  <head>
    <#include "system/header-error.ftl">
  </head>
 
  <body>

    <H1> <#if subsystem??>${subsystem?html}</#if> ERROR</H1>

    <#if generalInfo??>
      <p>${generalInfo?html}</p>
    </#if>

    <p> ${error?html} </p>


    <#if errorCause??>
      </small>
      <p>Administrator/developer oriented details of the error follows:</p>
	  <p><pre>Caused by: ${errorCause?html}</pre></p>
	  </small>
    </#if>
    
  </body>
</html>