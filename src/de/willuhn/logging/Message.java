/**********************************************************************
 * $Source: /cvsroot/jameica/util/src/de/willuhn/logging/Message.java,v $
 * $Revision: 1.4 $
 * $Date: 2008/06/13 13:48:17 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/
package de.willuhn.logging;

import java.util.Date;

/**
 * Eine zu loggende Nachricht.
 */
public class Message
{
	private Date date 	  = null;
	private Level level   = null;
  private String host   = null;
	private String text   = null;
	private String clazz  = null;
	private String method = null;

  /**
   * ct.
   * @param d
   * @param l
   * @param clazz
   * @param method
   * @param text
   */
  Message(Date d, Level l, String clazz, String method, String text)
  {
    this(d,l,null,clazz,method,text);
  }
  
	/**
	 * ct.
   * @param d
   * @param l
   * @param host
   * @param clazz
   * @param method
   * @param text
   */
  Message(Date d, Level l, String host, String clazz, String method, String text)
	{
		this.date = d;
		this.level = l;
    this.host = host;
		this.clazz = clazz;
		this.method = method;
		this.text = text;
	}
	
	/**
	 * Datum, an dem die Nachricht ausgeloest wurde.
   * @return Datum.
   */
  public Date getDate()
	{
		return date;
	}

	/**
	 * Liefert das LogLevel der Nachricht.
   * @return LogLevel.
   */
  public Level getLevel()
	{
		return level;
	}
  
  /**
   * Liefert den Hostnamen oder <code>null</code> wenn es Localhost ist oder er nicht angegeben ist.
   * @return der Hostname oder <code>null</code>.
   */
  public String getHost()
  {
    return this.host;
  }

	/**
	 * Liefert die eigentliche Nachricht.
   * @return Nachricht.
   */
  public String getText()
	{
		return text;
	}

	/**
	 * Liefert den Namen der loggenden Klasse.
   * @return Name der loggenden Klasse.
   */
  public String getLoggingClass()
	{
		return clazz;
	}

	/**
	 * Liefert den Namen der loggenden Methode.
   * @return Name der loggenden Methode.
   */
  public String getLoggingMethod()
	{
		return method;
	}
  /**
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    if (this.host != null && this.host.length() > 0)
    {
      sb.append("[");
      sb.append(host);
      sb.append("]");
    }
    
    if (this.date != null)
    {
      sb.append("[");
      sb.append(this.date.toString());
      sb.append("]");
    }
    
    if (this.level != null)
    {
      sb.append("[");
      sb.append(this.level.getName());
      sb.append("]");
    }
    
		if (clazz != null && method != null)
    {
      sb.append("[");
      sb.append(clazz);
      sb.append(".");
      sb.append(method);
      sb.append("]");
    }
    
		sb.append(" ");
		sb.append(text);
		return sb.toString();
  }

}


/**********************************************************************
 * $Log: Message.java,v $
 * Revision 1.4  2008/06/13 13:48:17  willuhn
 * @R removed unused import
 *
 * Revision 1.3  2008/06/13 13:48:01  willuhn
 * @N Hostname mit ausgeben
 *
 * Revision 1.2  2008/06/13 13:40:47  willuhn
 * @N Class und Method kann nun explizit angegeben werden
 * @N Hostname kann mitgeloggt werden
 *
 * Revision 1.1  2004/12/31 19:34:22  willuhn
 * @C some logging refactoring
 * @N syslog support for logging
 *
 **********************************************************************/