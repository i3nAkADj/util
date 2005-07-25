/**********************************************************************
 * $Source: /cvsroot/jameica/util/src/de/willuhn/util/Session.java,v $
 * $Revision: 1.6 $
 * $Date: 2005/07/25 22:12:45 $
 * $Author: web0 $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/
package de.willuhn.util;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Observable;

import de.willuhn.logging.Logger;

/**
 * Implementierung eines Session-Containers.
 * @author willuhn
 */
public class Session extends Observable
{

  private Worker worker = null;
  private long timeout;
  private Hashtable data = new Hashtable();

  /**
   * Erzeugt eine Session mit dem Default-Timeout von 30 Minuten.
   */
  public Session()
  {
    this(1000l * 60 * 30);
  }

  /**
   * ct.
   * @param timeout Anzahl der Millisekunden, nach deren Ablauf ein Element wieder entfernt werden soll.
   */
  public Session(long timeout)
  {
    Logger.debug("creating new session. default timeout: " + timeout + " millis");
    this.timeout = timeout;
    this.worker = new Worker();
    this.worker.start();
  }

  /**
   * Speichert einen Wert unter dem angegebenen Schluessel in der Session mit dem Default-Timeout.
   * @param key Name des Schluessels.
   * @param value Wert des Schluessels.
   */
  public void put(Object key, Object value)
  {
    put(key,value,this.timeout);
  }

  /**
   * Liefert eine Liste aller in der Session vorhandenen Schluessel.
   * @return Liste der Schluessel.
   */
  public Enumeration keys()
  {
    return data.keys();
  }

  /**
   * Speichert einen Wert unter dem angegebenen Schluessel in der Session mit einem separaten Timeoout.
   * @param key Name des Schluessels.
   * @param value Wert des Schluessels.
   * @param timeout Timeout in Millisekunden.
   */
  public void put(Object key, Object value, long timeout)
  {
    put(key,value,new Date(System.currentTimeMillis() + timeout));
  }

  /**
   * Speichert einen Wert unter dem angegebenen Schluessel in der Session mit einem konkreten Ziel-Datum fuer das Timeout.
   * @param key Name des Schluessels.
   * @param value Wert des Schluessels.
   * @param timeout Timeout als Datum.
   */
  public void put(Object key, Object value, Date timeout)
  {
    synchronized(data)
    {
      data.put(key,new SessionObject(value, timeout.getTime()));
      Logger.debug("added object to session, object: " + key.toString() + ", timeout: " + timeout.toString());
    }
  }

  
  /**
   * Liefert Wert aus der Session, der unter dem angegebenen Namen gespeichert ist.
   * @param key Name des Schluessels in der Session.
   * @return Wert des Schluessels.
   */
  public Object get(Object key)
  {
    synchronized(data)
    {
      SessionObject o = (SessionObject) data.get(key);
      return o == null ? null : o.value;
    }
  }

  /**
   * Liefert Wert aus der Session, der unter dem angegebenen Namen gespeichert ist
   * und entfernt den Wert gleichzeitig.
   * @param key Name des Schluessels in der Session.
   * @return Wert des Schluessels.
   */
  public Object remove(Object key)
  {
    synchronized(data)
    {
      SessionObject o = (SessionObject) data.remove(key);
      return o == null ? null : o.value;
    }
  }

  /**
   * Liefert die Anzahl der Elemente in der Session.
   * @return Anzahl der Elemente.
   */
  public int size()
  {
    synchronized(data)
    {
      return data.size();
    }
  }

  /**
   * @see java.lang.Object#finalize()
   */
  protected void finalize() throws Throwable
  {
    worker.interrupt();
    super.finalize();
  }

  private class SessionObject
  {
    private Object value;
    private long myTimeout;

    private SessionObject(Object value, long t)
    {
      this.value = value;
      this.myTimeout = t;
    }
  }

  private class Worker extends Thread
  {
    /**
     * ct.
     */
    public Worker()
    {
      super("Worker Thread for Session " + Session.this.hashCode());
      Logger.debug("Created Worker Thread for Session " + Session.this.hashCode());
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
      Logger.debug("starting worker thread");
      while (true)
      {
        long current = System.currentTimeMillis();
        try
        {
          synchronized(data)
          {
            Enumeration e = data.keys();
            while (e.hasMoreElements())
            {
              Object key          = e.nextElement();
              SessionObject value = (SessionObject) data.get(key);
              if (current > value.myTimeout)
              {
                Logger.debug("removing object " + key + " from session");
                data.remove(key);
                Session.this.setChanged();
                Session.this.notifyObservers(value.value);
              }
            }
          }
          sleep(1000l);
        }
        catch (InterruptedException e)
        {
          Logger.info("worker thread for session " + Session.this.hashCode() + " interrupted");
        }
      }
    }

}
}

/*********************************************************************
 * $Log: Session.java,v $
 * Revision 1.6  2005/07/25 22:12:45  web0
 * *** empty log message ***
 *
 * Revision 1.5  2005/07/24 16:59:17  web0
 * @B fix in settings watcher
 *
 * Revision 1.4  2005/07/08 16:42:31  web0
 * *** empty log message ***
 *
 * Revision 1.3  2005/04/28 15:43:33  web0
 * *** empty log message ***
 *
 * Revision 1.2  2005/04/04 17:51:09  web0
 * @N new Session
 *
 **********************************************************************/