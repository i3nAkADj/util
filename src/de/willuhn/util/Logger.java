/**********************************************************************
 * $Source: /cvsroot/jameica/util/src/de/willuhn/util/Attic/Logger.java,v $
 * $Revision: 1.5 $
 * $Date: 2004/01/06 19:58:29 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;

import de.willuhn.util.Queue.QueueFullException;

/**
 * Kleiner System-Logger.
 * @author willuhn
 */
public class Logger
{

  private ArrayList targets = new ArrayList();

  // maximale Groesse des Log-Puffers (Zeilen-Anzahl)
  private final static int BUFFER_SIZE = 40;

  // Eine Queue mit den letzten Log-Eintraegen. Kann ganz nuetzlich sein,
  // wenn man irgendwo in der Anwendung mal die letzten Zeilen des Logs ansehen will.
  private Queue lastLines = new Queue(BUFFER_SIZE);

	public final static String[] LEVEL_TEXT = new String[] {"DEBUG","INFO","WARN","ERROR"};

	public final static int LEVEL_DEBUG = 0;
	public final static int LEVEL_INFO  = 1;
	public final static int LEVEL_WARN  = 2;
	public final static int LEVEL_ERROR = 3;
  
	private int level = LEVEL_DEBUG;

	private LoggerThread lt = null;

  /**
   * ct.
   */
  public Logger()
  {
  	lt = new LoggerThread();
  	lt.start();
  }
  
	/**
	 * Fuegt der Liste der Ausgabe-Streams einen weiteren hinzu.
   * @param target AusgabeStream.
   */
  public void addTarget(OutputStream target)
	{
		if (target == null)
			return;
		this.targets.add(target);
	}

	/**
	 * Setzt den Log-Level.
   * @param level Log-Level.
   */
  public void setLevel(int level)
	{
		if (level >= 0 && level < LEVEL_TEXT.length)
			this.level = level;
	}

	/**
	 * Setzt den Log-Level basierend auf dem uebergebenen String.
   * @param level Name des Log-Levels (DEBUG,INFO,WARN,ERROR).
   */
  public void setLevel(String level)
	{
		if (level == null || "".equals(level))
			return;
		for (int i=0;i<LEVEL_TEXT.length;++i)
		{
			if (LEVEL_TEXT[i].equalsIgnoreCase(level))
				setLevel(i);
		}
	}

  /**
   * Schreibt eine Message vom Typ "debug" ins Log.
   * @param message zu loggende Nachricht.
   */
  public void debug(String message)
  {
    write(LEVEL_DEBUG,message);
  }

  /**
   * Schreibt eine Message vom Typ "info" ins Log.
   * @param message zu loggende Nachricht.
   */
  public void info(String message)
  {
    write(LEVEL_INFO,message);
  }

  /**
   * Schreibt eine Message vom Typ "warn" ins Log.
   * @param message zu loggende Nachricht.
   */
  public void warn(String message)
  {
    write(LEVEL_WARN,message);
  }

  /**
   * Schreibt eine Message vom Typ "error" ins Log.
   * @param message zu loggende Nachricht.
   */
  public void error(String message)
  {
    write(LEVEL_ERROR,message);
  }

	/**
	 * Schreibt den Fehler ins Log.
	 * @param message zu loggende Nachricht.
   * @param t Exception oder Error.
   */
  public void error(String message, Throwable t)
	{
		write(LEVEL_ERROR,message);
		ByteArrayOutputStream bos = null;
		try {
			bos = new ByteArrayOutputStream();
			t.printStackTrace(new PrintStream(bos));
			write(LEVEL_ERROR,bos.toString());
		}
		finally {
			try {
				bos.close();
			}
			catch (Exception npe) {}
		}
		
	}

  /**
   * Schliesst den Logger und die damit verbundene Log-Datei.
   */
  public void close()
	{
		lt.interrupt();
		OutputStream os = null;
		for (int i=0;i<this.targets.size();++i)
		{
			os = (OutputStream) this.targets.get(i);
			try {
				os.flush();
				os.close();
			}
			catch (IOException io)
			{
			}
		}
	}

  /**
   * Liefert die letzten Zeilen des Logs.
   * @return String-Array mit den letzten Log-Eintraegen (einer pro Index).
   */
  public String[] getLastLines()
  {
    return (String[]) lastLines.toArray(new String[lastLines.size()]);
  }

  /**
   * Interne Methode zum Formatieren und Schreiben der Meldungen.
   * @param level Log-Levels.
   * @param message zu loggende Nachricht.
   */
  private void write(int level, String message)
  {
  	if (level < this.level)
  		return;

		lt.write(level,message);
  }
  
  /**
   * Das eigentliche Schreiben erfolgt in einem extra Thread damit's hoffentlich schneller geht.
   */
  private class LoggerThread extends Thread
  {
  	
  	private final static int maxLines = 100;
  	private Queue messages = new Queue(maxLines);

  	/**
     * ct.
     */
    public LoggerThread()
  	{
  		super(LoggerThread.class.getName());
  	}

		/**
		 * Loggt eine Zeile in's Logfile.
     * @param level Log-Level.
     * @param message Die eigentliche Nachricht.
     */
    public void write(int level, String message)
		{
			String s = "["+new Date().toString()+"] ["+LEVEL_TEXT[level]+"] " + message;
			try
      {
        messages.push(s);
      }
      catch (QueueFullException e)
      {
        System.out.println(s);
      }
		}

    /**
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
    	byte[] message;
			while(true)
			{
				if (messages.size() > 0)
				{
					String s = (String) messages.pop();

					synchronized (lastLines)
					{
						try
						{
							{
								if (lastLines.full())
									lastLines.pop();
								lastLines.push(s);
							}
						}	catch (QueueFullException e1)	{}
					}

					OutputStream os = null;
					message = (s + "\n").getBytes();
					for (int i=0;i<targets.size();++i)
					{
						os = (OutputStream) targets.get(i);
						try
						{
							os.write(message);
						}
						catch (IOException e)
						{
						}
					}
				}
				try
        {
          sleep(100);
        }
        catch (InterruptedException e)
        {
        }
			}
    }

  }
}

/*********************************************************************
 * $Log: Logger.java,v $
 * Revision 1.5  2004/01/06 19:58:29  willuhn
 * @N ArrayEnumeration
 *
 * Revision 1.4  2004/01/06 18:07:07  willuhn
 * *** empty log message ***
 *
 * Revision 1.3  2004/01/05 23:08:04  willuhn
 * *** empty log message ***
 *
 * Revision 1.2  2004/01/05 21:46:29  willuhn
 * @N added queue
 * @N logger writes now in separate thread
 *
 * Revision 1.1  2004/01/03 19:33:59  willuhn
 * *** empty log message ***
 *
 * Revision 1.4  2004/01/03 18:08:05  willuhn
 * @N Exception logging
 * @C replaced bb.util xml parser with nanoxml
 *
 * Revision 1.3  2003/12/10 00:47:12  willuhn
 * @N SearchDialog done
 * @N ErrorView
 *
 * Revision 1.2  2003/11/13 00:37:35  willuhn
 * *** empty log message ***
 *
 * Revision 1.1  2003/10/23 21:49:46  willuhn
 * initial checkin
 *
 **********************************************************************/
