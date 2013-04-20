header {
package pl.edu.icm.unity.ldaputils.generated;
import pl.edu.icm.unity.ldaputils.LDAPAttributeType;
import pl.edu.icm.unity.ldaputils.LDAPObjectClass;
import pl.edu.icm.unity.ldaputils.LDAPSchema;
import java.util.List;
import java.util.ArrayList;
}


class LdapParser extends Parser;

options { k=5; }

/* Key definition from RFC 2252:

	    AttributeTypeDescription = "(" whsp
            numericoid whsp              ; AttributeType identifier
          [ "NAME" qdescrs ]             ; name used in AttributeType
          [ "DESC" qdstring ]            ; description
          [ "OBSOLETE" whsp ]
          [ "SUP" woid ]                 ; derived from this other
                                         ; AttributeType
          [ "EQUALITY" woid              ; Matching Rule name
          [ "ORDERING" woid              ; Matching Rule name
          [ "SUBSTR" woid ]              ; Matching Rule name
          [ "SYNTAX" whsp noidlen whsp ] ; see section 4.3
          [ "SINGLE-VALUE" whsp ]        ; default multi-valued
          [ "COLLECTIVE" whsp ]          ; default not collective
          [ "NO-USER-MODIFICATION" whsp ]; default user modifiable
          [ "USAGE" whsp AttributeUsage ]; default userApplications
          whsp ")"
AttributeUsage =
          "userApplications"     /
          "directoryOperation"   /
          "distributedOperation" / ; DSA-shared
          "dSAOperation"          ; DSA-specific, value depends on server
*/	


startRule returns [LDAPSchema ret]
  { 
    ret = new LDAPSchema();
    LDAPAttributeType at;
    LDAPObjectClass oc;
  }:
	(at=atrtype 
	{
		ret.addAttributeType(at);
	}
	| oc=objclass
    {
        ret.addObjectClass(oc);
    } 
    )+;
	
atrtype returns [LDAPAttributeType at]
{ 
	at = new LDAPAttributeType();
	List<String> names = null;
	String usage = null;
	String sup=null;
	String eq = null;
	String ordering = null;
	String substr = null;
} : ( "attributetype" whsp LPAREN whsp
			oid:NUMOID whsp
			("NAME" WHSP names=qdescrs)? whsp
			(d:DESCR whsp)? whsp
			(obsolete:"OBSOLETE" whsp)?  whsp
			("SUP" sup=woid)? whsp
			("EQUALITY" eq=woid)? whsp
			("ORDERING" ordering=woid)? whsp
			("SUBSTR" substr=woid)? whsp
			("SYNTAX" whsp syntax:NUMOID whsp)? whsp
			(singleValue:"SINGLE-VALUE" whsp)? whsp
			(collective:"COLLECTIVE" whsp)? whsp
			(noModify:"NO-USER-MODIFICATION" whsp)? whsp
			("USAGE" whsp usage=attrusage whsp)? 
			 RPAREN )
{  
	if (d != null) at.setDescription(d.getText());
	at.setOid(oid.getText());
	at.setNames(names);
	at.setUsage(usage);
	at.setSuperclass(sup);
	at.setOrdering(ordering);
	at.setEquality(eq);
	at.setSubstring(substr);
	if (syntax != null) at.setSyntax(syntax.getText());
	if (obsolete != null) at.setObsolete(true);
	if (singleValue != null) at.setSingleValue(true);
	if (noModify != null) at.setNoUserModify(true);
	if (collective != null) at.setCollective(true);
};

/*
ObjectClassDescription = "(" whsp
          numericoid whsp      ; ObjectClass identifier
          [ "NAME" qdescrs ]
          [ "DESC" qdstring ]
          [ "OBSOLETE" whsp ]
          [ "SUP" oids ]       ; Superior ObjectClasses
          [ ( "ABSTRACT" / "STRUCTURAL" / "AUXILIARY" ) whsp ]
                               ; default structural
          [ "MUST" oids ]      ; AttributeTypes
          [ "MAY" oids ]       ; AttributeTypes
      whsp ")"
*/
objclass  returns [LDAPObjectClass oc]
    {
	oc = new LDAPObjectClass();
	List<String> supOids = null;
	List<String> mustOids = null;
	List<String> mayOids = null;
	List<String> names = null;
    } 
    : ( "objectclass" whsp LPAREN whsp 
			oid:NUMOID whsp
			("NAME" WHSP names=qdescrs)? 
			(d:DESCR  whsp)?  
			("OBSOLETE" whsp)?  
			("SUP" oids )? 
			(clstype whsp)? 
			("MUST" oids)?   
			("MAY" oids)? 
			 RPAREN )
	{
	  oc.setOid(oid.getText());
	  if (names != null) oc.setNames(names);
	  oc.setSuperclasses (supOids);
	  oc.setRequiredAttributes (mustOids);
	  oc.setOptionalAttributes (mayOids);
    };
			

			
qdescrs  returns [List<String> ret] 
	{
		ret = new ArrayList<String>();
		String n;
	}:
	n=qdescr {
			if (n!= null);
				ret.add(n);
		}
	| ( LPAREN whsp ret=qdescrlist RPAREN whsp) ;

qdescr returns [String ret=null] : 
	APO r:KEYSTR APO whsp
	{
		ret=r.getText();
	}; 

qdescrlist returns [List<String> ret] 
	{
		ret = new ArrayList<String>();
		String n;
	}:
	 ( ( n=qdescr 
	 {
	 	if (n!=null)
		 	ret.add(n);	
	 })+ )?;

	
whsp : (options {greedy=true;} : WHSP)?;

oid returns [String ret=null] : 
	t:KEYSTR
	{
		ret = t.getText();
	}
	| p:NUMOID
	{
		ret = p.getText();
	};

woid returns [String ret=null] : whsp ret=oid whsp;

oids : whsp (oid | (LPAREN oidlist RPAREN)) whsp;

oidlist : woid (whsp DOL whsp woid )*;

attrusage returns [String ret="def"] : 
	"userApplications" 
	{
    	ret="userApplications";
    }
    | "directoryOperation"
    {
    	ret="directoryOperation";
    } 
    | "distributedOperation"     
    {
    	ret="distributedOperation";
    }
    | "dSAOperation"
    {
    	ret="dSAOperation";
    };
    
clstype: "ABSTRACT" | "STRUCTURAL" | "AUXILIARY";
	
	
// #################### LEXER #############################
	
class LdapLexer extends Lexer;

options { k=10; }

DOL: '$';

LPAREN: '(' ;


RPAREN: ')' ;

APO: '\'';


NEWLINE   :  ( "\r\n" // DOS
               | '\r'   // MAC
               | '\n'   // Unix
             )
             { newline(); 
               $setType(Token.SKIP);
             }
          ;

protected
A:	( 'a'..'z' | 'A'..'Z');

protected
DIGIT:	'0'..'9';

protected
ALPHA: (A | DIGIT);

protected
K: (ALPHA | '-' | ';');

KEYSTR: A (K)*;

protected
APOINSTR: (~'\'')*;

DESCR: "DESC" (WHSP)? APO t:APOINSTR APO (WHSP)?   
  {
    $setText(t.getText());
  };

WHSP :	(options {greedy=true;} : (' ' | '\t'))+;

NUMOID: (DIGIT)+ ('.' (DIGIT)+)* (LEN)?;

LEN: '{' (DIGIT)+ '}';

COMMENT: '#' (~('\n'))* '\n' { newline(); $setType(Token.SKIP); };

