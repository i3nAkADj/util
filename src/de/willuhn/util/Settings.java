/**********************************************************************
 * $Source: /cvsroot/jameica/util/src/de/willuhn/util/Settings.java,v $
 * $Revision: 1.28 $
 * $Date: 2012/03/20 23:26:19 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/
package de.willuhn.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import de.willuhn.io.IOUtil;
import de.willuhn.logging.Logger;

/**
 * Diese Klasse erweitert Java-Properties um Typsicherheit fuer primitive
 * Typen, Support zum Laden und Speichern von String-Arrays, automatisches
 * Abspeichern beim Aufruf einer Set-Methode und sogar Speichern schon
 * beim Lesen. Das ist nuetzlich, wenn man eine Software ohne properties-Dateien
 * ausliefern will aber dennoch nach dem ersten Start beim Benutzer die
 * Config-Dateien mit den Default-Werten angelegt werden damit dieser
 * nicht in der Dokumentation nach den Schluesselnamen suchen muss
 * sondern sie bereits mit Default-Werten in den Dateien vorfindet.
 * Wird die Properties-Datei von aussen (z.Bsp. mit einem Texteditor)
 * geaendert, wird das automatisch erkannt und die Datei intern neu geladen.
 * @author willuhn
 */
public class Settings
{

  private File file             = null;
  private double lastModified   = 0;
  private Properties properties = null; // Bei Gelegenheit mal auf TypedProperties umstellen
	private boolean storeWhenRead = false;

  /**
   * Erzeugt eine neue Instanz der Settings, die exclusiv
   * nur fuer diese Klasse gelten. Existieren bereits Settings
   * fuer die Klasse, werden sie gleich geladen.
   * Hierbei wird eine Properties-Datei
   * [classname].properties im angegebenen Verzeichnis angelegt.
   * @param path Pfad zu den Einstellungen.
   * @param clazz Klasse, fuer die diese Settings gelten.
   */
  public Settings(String path, Class clazz)
  {
    this(null,path,clazz);
  }

  /**
   * Erzeugt eine neue Instanz der Settings, die exclusiv
   * nur fuer diese Klasse gelten. Existieren bereits Settings
   * fuer die Klasse, werden sie gleich geladen.
   * Hierbei wird eine Properties-Datei
   * [classname].properties im angegebenen Verzeichnis angelegt.
   * @param systemPath Pfad zu ggf vorhandenen System-Presets.
   * @param userPath Pfad zu den User-Einstellungen.
   * @param clazz Klasse, fuer die diese Settings gelten.
   */
  public Settings(String systemPath, String userPath, Class clazz)
  {
    this(
          (systemPath != null ? new File(systemPath + File.separator + clazz.getName() + ".properties") : null),
          new File(userPath + File.separator + clazz.getName() + ".properties")
    );
  }

  /**
   * Erzeugt eine neue Instanz.
   * @param systemFile Properties-Datei mit den System-Vorgaben.
   * @param userFile Properties-Datei des Users, welche die System-Vorgaben ueberschreiben.
   */
  public Settings(File systemFile, File userFile)
  {
    // Filenamen ermitteln
    this.file = userFile;
    this.properties = new Properties();

    // Checken, ob System-Presets existieren
    if (systemFile != null)
    {
      if (systemFile.exists() && systemFile.canRead())
      {
        InputStream is = null;
        try
        {
          // "this.properties" wird initial mit den System-Vorgaben befuellt.
          // Wenn der User eigene Werte definiert hat, ersetzen seine Werte anschliessend
          // in "reload()" die System-Vorgaben
          Logger.debug("loading system presets from " + systemFile.getAbsolutePath());
          Properties presets = new Properties();
          is = new BufferedInputStream(new FileInputStream(systemFile));
          presets.load(is);
          this.properties.putAll(presets);
        }
        catch (Exception e1)
        {
          Logger.error("unable to load system presets from " + systemFile.getAbsolutePath(),e1);
        }
        finally
        {
          IOUtil.close(is);
        }
      }
    }
    
    // Parameter laden, wenn die Datei existiert
    if (this.file != null && this.file.exists())
      reload();
  }

  /**
	 * Legt fest, ob die Einstellungen schon beim Lesen gespeichert werden sollen.
	 * Hintergrund: Jede Get-Funktion (getString(), getBoolean(),..) besitzt einen
	 * Parameter mit dem Default-Wert falls der Parameter noch nicht existiert.
	 * Ist dies der Fall und die zugehoerige Set-Methode wird nie aufgerufen,
	 * dann erscheint der Parameter nie physisch in der properties-Datei.
	 * Diese muesste dann manuell mit den Parametern befuellt werden, um
	 * sie aendern zu koennen. Da die Parameter-Namen aber nur in der
	 * Java-Klasse bekannt sind, wird es einem Fremden schwer fallen, die
	 * Namen der Parameter zu ermitteln. Fuer genau diesen Fall kann der
	 * Parameter auf true gesetzt werden. Alle abgefragten Parameter, werden
	 * dann nach der Abfrage mit dem aktuellen Wert (ggf. dem Default-Wert)
	 * sofort gespeichert.
	 * Der Default-Wert ist "false". Per Default wird beim Lesen also nicht geschrieben.
   * @param b true, wenn sofort geschrieben werden soll.
   */
  public void setStoreWhenRead(boolean b)
	{
		this.storeWhenRead = b;
	}

	/**
	 * Liefert eine Liste aller Attribut-Namen, die in dieser Settings-Instanz gespeichert wurden.
   * @return Liste der Attribut-Namen.
   */
  public String[] getAttributes()
	{
    reload();
		synchronized (properties)
		{
			Iterator it = properties.keySet().iterator();
			String[] attributes = new String[properties.size()];
			int i = 0;
			while (it.hasNext())
			{
				attributes[i++] = (String) it.next();
			}
			return attributes;
		}
	}

	/**
	 * Liefert den Wert des genannten Attributs als Boolean.
	 * Wird das Attribut nicht gefunden oder hat keinen Wert, wird defaultValue zurueckgegeben.
   * @param name Name des Attributs.
	 * @param defaultValue DefaultWert, wenn das Attribut nicht existiert.
   * @return true oder false.
   */
  public boolean getBoolean(String name, boolean defaultValue)
	{
    reload();
		String s = getProperty(name,defaultValue ? "true" : "false");
    if (s != null) s = s.trim(); // BUGZILLA 477
		boolean b = "true".equalsIgnoreCase(s);
		if (storeWhenRead)
			setAttribute(name,b);
		return b;
	}

	/**
	 * Liefert den Wert des genannten Attributs als int.
	 * Wird das Attribut nicht gefunden oder hat keinen Wert, wird defaultValue zurueckgegeben.
	 * Hinweis: Die Funktion wirft keine NumberFormat-Exception, wenn der
	 * Wert nicht in eine Zahl gewandelt werden kann. Stattdessen wird der
	 * Default-Wert zurueckgegeben.
	 * @param name Name des Attributs.
	 * @param defaultValue DefaultWert, wenn das Attribut nicht existiert.
	 * @return der Wert des Attributs.
	 */
	public int getInt(String name, int defaultValue)
	{
    reload();
		String s = getProperty(name,""+defaultValue);
    if (s != null) s = s.trim(); // BUGZILLA 477
		int i = defaultValue;
		try {
			i = Integer.parseInt(s);
		}
		catch (NumberFormatException e)
		{
			Logger.error("unable to parse value of param \"" + name + "\", value: " + s,e);
		}
		if (storeWhenRead)
			setAttribute(name,i);
		return i;
	}

  /**
   * Liefert den Wert des genannten Attributs als long.
   * Wird das Attribut nicht gefunden oder hat keinen Wert, wird defaultValue zurueckgegeben.
   * Hinweis: Die Funktion wirft keine NumberFormat-Exception, wenn der
   * Wert nicht in eine Zahl gewandelt werden kann. Stattdessen wird der
   * Default-Wert zurueckgegeben.
   * @param name Name des Attributs.
   * @param defaultValue DefaultWert, wenn das Attribut nicht existiert.
   * @return der Wert des Attributs.
   */
  public long getLong(String name, long defaultValue)
  {
    reload();
    String s = getProperty(name,""+defaultValue);
    if (s != null) s = s.trim(); // BUGZILLA 477
    long l = defaultValue;
    try {
      l = Long.parseLong(s);
    }
    catch (NumberFormatException e)
    {
      Logger.error("unable to parse value of param \"" + name + "\", value: " + s,e);
    }
    if (storeWhenRead)
      setAttribute(name,l);
    return l;
  }

	/**
	 * Liefert den Wert des genannten Attributs als double.
	 * Wird das Attribut nicht gefunden oder hat keinen Wert, wird defaultValue zurueckgegeben.
	 * Hinweis: Die Funktion wirft keine NumberFormat-Exception, wenn der
	 * Wert nicht in eine Zahl gewandelt werden kann. Stattdessen wird der
	 * Default-Wert zurueckgegeben.
	 * @param name Name des Attributs.
	 * @param defaultValue DefaultWert, wenn das Attribut nicht existiert.
	 * @return der Wert des Attributs.
	 */
	public double getDouble(String name, double defaultValue)
	{
    reload();
		String s = getProperty(name,""+defaultValue);
    if (s != null) s = s.trim(); // BUGZILLA 477
		double d = defaultValue;
		try {
			d = Double.parseDouble(s);
		}
		catch (NumberFormatException e)
		{
			Logger.error("unable to parse value of param \"" + name + "\", value: " + s,e);
		}
		if (storeWhenRead)
			setAttribute(name,d);
		return d;
	}

  /**
   * Liefert den Wert des Attributes.
   * @param name
   * @param defaultValue
   * @return der Wert des Attributes.
   */
  private String getProperty(String name, String defaultValue)
	{
    return properties.getProperty(name, defaultValue);
	}

	/**
	 * Liefert den Wert des Attribute.
	 * Wird das Attribut nicht gefunden oder hat keinen Wert, wird defaultValue zurueckgegeben.
	 * @param name Name des Attributs.
	 * @param defaultValue DefaultWert, wenn das Attribut nicht existiert.
	 * @return der Wert des Attributs.
	 */
	public String getString(String name, String defaultValue)
	{
    reload();
		String s = getProperty(name,defaultValue);
		if (storeWhenRead)
			setAttribute(name,s);
		return s;
	}

  /**
   * Liefert ein Array von Werten.
   * Wird das Attribut nicht gefunden oder hat keinen Wert, wird defaultValue zurueckgegeben.
   * Es koennen maximal 256 Werte gelesen oder gespeichert werden.
   * @param name Name des Attributs.
   * @param defaultValues DefaultWert, wenn das Attribut nicht existiert.
   * @return Werte des Attributs in Form eines String-Arrays.
   */
  public String[] getList(String name, String[] defaultValues)
  {
    reload();
    List<String> l = new ArrayList<String>();
    String s = null;
    for (int i=0;i<255;++i)
    {
      s = getProperty(name + "." + i,null);
      if (s == null) continue;
      l.add(s);
    }
    if (l.size() == 0)
    {
      if (storeWhenRead)
        setAttribute(name,defaultValues);
      return defaultValues;
    }
    String[] result = l.toArray(new String[l.size()]);
    if (storeWhenRead)
      setAttribute(name,result);
    return result;
  }

	/**
	 * Speichert einen boolschen Wert.
   * @param name Name des Attributs.
   * @param value Wert des Attributs.
   */
  public void setAttribute(String name, boolean value)
	{
		setAttribute(name, value ? "true" : "false");
	}
	
	/**
	 * Speichert einen Integer-Wert.
   * @param name Name des Attributs.
   * @param value Wert des Attributs.
   */
  public void setAttribute(String name, int value)
	{
		setAttribute(name,""+value);
	}

	/**
	 * Speichert einen Double-Wert.
	 * @param name Name des Attributs.
	 * @param value Wert des Attributs.
	 */
	public void setAttribute(String name, double value)
	{
		setAttribute(name,""+value);
	}

  /**
   * Speichert einen Long-Wert.
   * @param name Name des Attributs.
   * @param value Wert des Attributs.
   */
  public void setAttribute(String name, long value)
  {
    setAttribute(name,""+value);
  }

	/**
   * Speichert das Attribut <name> mit dem zugehoerigen Wert <value>.
   * Wenn ein gleichnamiges Attribut bereits existiert, wird es ueberschrieben.
   * Ist der Wert des Attributes <code>null</code>, wird es entfernt.
   * @param name Name des Attributs.
   * @param value Wert des Attributs.
   */
  public void setAttribute(String name, String value)
  {
    // Wir speichern nur, wenn etwas geaendert wurde
    if (value == null)
  	{
      String prev = (String) properties.remove(name);
      
      // Wir haben wirklich was geloescht oder sollen immer speichern.
      if (prev != null || this.storeWhenRead)
        store();
  	}
  	else
  	{
      String prev = (String) properties.setProperty(name,value);
      
      // vorher existierte der Parameter nicht, oder er war anders oder wir sollen immer speichern
      if (prev == null || !value.equals(prev) || this.storeWhenRead)
        store();
  	}
  }

  /**
   * Speichert das Attribut <name> mit der zugehoerigen Liste von Werten <value>.
   * Wenn ein gleichnamiges Attribut bereits existiert, werden dessen Werte ueberschrieben.
   * Ist der Wert des Attributes <code>null</code>, wird es entfernt.
   * Von dem Array werden die ersten maximal 256 Elemente gespeichert.
   * Alle darueber hinausgehenden Werte, werden ignoriert.
   * @param name Name des Attributs.
   * @param values Werte des Attributs.
   */
  public void setAttribute(String name, String[] values)
  {
    // Aktuelle Werte laden, um zu checken, ob sich was geaendert hat
    // Aber nur, wenn wir nicht immer speichern sollen
    if (!this.storeWhenRead)
    {
      String[] prev = this.getList(name,null);

      // Diese Funktion macht den kompletten Vergleich selbst
      // Vor dem Vergleich der Elemente prueft sie auch, ob eines
      // von beiden NULL ist und ob beide die gleiche Groesse haben
      // Der Inhaltsvergleich findet da drin nur statt, wenn beide
      // nicht NULL sind und die gleiche Groesse haben.
      if (Arrays.equals(prev,values))
        return;
    }
    
    // Wir entfernen immer erst alle Werte. Denn wenn vorher
    // ein laengeres Array drin steht, als wir jetzt reinschreiben,
    // wuerden die alten Werte am Ende des grossen Arrays nicht mehr
    // entfernt.
    for (int i=0;i<255;++i)
    {
      properties.remove(name + "." + i);
    }
    
    if (values == null || values.length == 0)
    {
      store();
      return;
    }
      
    for (int i=0;i<values.length;++i)
    {
      if (i >= 255)
        break; // Schluss jetzt. Das waren genug Werte ;)
      if (values[i] == null)
        continue; // NULL-Werte ueberspringen
      properties.setProperty(name + "." + i,values[i]);
    }
    store();
  }
  
  /**
   * Schreibt die Properties in die Datei.
   * Hinweis: Die Funktion wirft keine IOException, wenn die Datei nicht
   * gespeichert werden kann. Stattdessen wird der Fehler lediglich geloggt.
   */
  private synchronized void store()
  {
    // Die Datei existiert noch nicht und wir haben noch keine Werte.
    // Dann muessen wir die Datei auch nicht sinnlos anlegen
    if (this.file == null || !this.file.exists() && this.properties.size() == 0)
      return;
    
    OutputStream os = null;
    try
    {
      os = new BufferedOutputStream(new FileOutputStream(this.file));
      this.properties.store(os,null);
    }
    catch (Exception e1)
    {
      Logger.error("unable to store settings. Do you have write permissions in " + this.file.getAbsolutePath() + " ?",e1);
    }
    finally
    {
      this.lastModified = this.file.lastModified();
      if (os != null)
      {
        try
        {
          os.close();
        }
        catch (Exception e)
        {
          Logger.error("unable to close settings file: " + this.file.getAbsolutePath(),e);
        }
      }
    }
  }

  /**
   * Laedt die Datei neu.
   */
  private synchronized void reload()
  {
    if (this.file == null)
      return;
    
    long modified = this.file.lastModified();

    if (this.lastModified == modified)
      return; // Kein Reload noetig

    InputStream is = null;
    try
    {
      if (this.lastModified > 0) // wenn lastModified 0 ist, wurde die Datei noch gar nicht geladen
        Logger.debug(this.file.getAbsolutePath() + " has changed, reloading");
      
      is = new BufferedInputStream(new FileInputStream(this.file));
      this.properties.load(is);
    }
    catch (FileNotFoundException nfe)
    {
      Logger.warn("file " + this.file.getAbsolutePath() + " has been deleted");
      this.properties.clear();
    }
    catch (Exception e1)
    {
      Logger.error("unable to (re)load settings. Do you have read permissions in " + this.file.getAbsolutePath() + " ?",e1);
    }
    finally
    {
      this.lastModified = modified;
      IOUtil.close(is);
    }
  }
}

/*********************************************************************
 * $Log: Settings.java,v $
 * Revision 1.28  2012/03/20 23:26:19  willuhn
 * @N Support fuer "readonly" Settings, die nur aus User-Presets bestehen (BUGZILLA 1209)
 *
 * Revision 1.27  2011-08-10 09:43:39  willuhn
 * @N TypedProperties
 *
 * Revision 1.26  2011-05-27 15:18:13  willuhn
 * @N Aenderungen nur speichern, wenn wirklich was geaendert wurde oder das Speichern forciert wird
 *
 * Revision 1.25  2010/04/06 11:27:05  willuhn
 * *** empty log message ***
 *
 * Revision 1.24  2010/03/11 07:46:35  willuhn
 * @N Properties-Datei nur anlegen, wenn tatsaechlich Werte vorliegen
 *
 * Revision 1.23  2009/10/28 11:23:00  willuhn
 * @N getLong()
 *
 * Revision 1.22  2009/09/18 10:37:20  willuhn
 * @N Neuer Konstruktor, mit dem der Dateiname der Config-Datei nun auch explizit angegeben werden kann.
 *
 * Revision 1.21  2009/01/16 16:39:56  willuhn
 * @N Funktion zum Erzeugen von SHA1-Checksummen
 * @N Funktion zum Erzeugen von Checksummen aus InputStreams
 *
 * Revision 1.20  2008/06/17 10:51:06  willuhn
 * @C User-Parameter ueberschreiben System-Parameter
 * @C properties-Dateien nicht sofort anlegen - erzeugt sonst eine Fuelle von leeren Config-Dateien
 *
 * Revision 1.19  2008/06/16 22:04:20  willuhn
 * @N System-Presets nur uebernehmen, wenn noch keine User-Config vorhanden
 *
 * Revision 1.18  2008/06/16 22:01:36  willuhn
 * @B Uebernehmen der System-Presets
 *
 * Revision 1.17  2008/04/02 21:16:30  willuhn
 * @B OutputStream not closed in store()
 **********************************************************************/