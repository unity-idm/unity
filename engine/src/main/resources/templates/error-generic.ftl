<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title> Error </title>
  </head>
 
  <body>

    <h1 style="color: red;">Error: ${errorCode?html}</h1>
    
    <p>Error reason:</p>
    <pre>${error?html}</pre>
    
  </body>
</html>