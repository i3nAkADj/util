/**********************************************************************
 * $Source: /cvsroot/jameica/util/src/de/willuhn/sql/version/Updater.java,v $
 * $Revision: 1.1 $
 * $Date: 2007/10/01 23:16:56 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.sql.version;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.willuhn.logging.Logger;
import de.willuhn.sql.ScriptExecutor;
import de.willuhn.util.ApplicationException;


/**
 * Ein generisches Update-Utility.
 */
public class Updater
{
  private UpdateProvider provider = null;
  private MyClassloader loader    = null;

  /**
   * ct.
   * @param provider der zu verwendende Provider.
   */
  public Updater(UpdateProvider provider)
  {
    this.provider = provider;
    this.loader   = new MyClassloader();
  }
  
  /**
   * Fuehrt das Update durch.
   * @throws ApplicationException wenn ein Fehler beim Update auftrat.
   */
  public void execute() throws ApplicationException
  {
    int currentVersion = provider.getCurrentVersion();
    Logger.info("current version: " + currentVersion);

    Logger.info("searching for available updates");
    
    // Wir ermitteln eine Liste aller Dateien im Update-Verzeichnis.
    File baseDir = provider.getUpdatePath();
    if (baseDir == null || !baseDir.exists() || !baseDir.canRead() || !baseDir.isDirectory())
    {
      Logger.warn("no update dir given or not readable");
      return;
    }
    
    File[] files = baseDir.listFiles();
    
    // Jetzt sortieren wir die Dateien und holen uns anschliessend
    // nur die, welche sich hinter der aktuellen Versionsnummer
    // befinden.
    List l = Arrays.asList(files);
    Collections.sort(l);
    
    files = (File[]) l.toArray(new File[l.size()]);
    
    
    // wir iterieren ueber die Liste, und ueberspringen alle
    // bis zur aktuellen Version.
    ArrayList updates = new ArrayList();
    for (int i=0;i<files.length;++i)
    {
      File current = files[i];
      
      // Unterverzeichnisse (z.Bsp. "CVS") ignorieren wir.
      if (current.isDirectory())
        continue;

      // Update-Datei nicht lesbar.
      if (!current.canRead() || !current.isFile())
      {
        Logger.warn("update file " + current + " not readable, skipping");
        continue;
      }
      
      String name = current.getName();
      
      // Dateiendung abschneiden
      name = name.substring(0,name.lastIndexOf("."));
      
      // Wir versuchen den Dateinamen als Zahl zu parsen
      try
      {
        int number = Integer.parseInt(name);
        
        // Wir uebernehmen das Update nur, wenn dessen
        // Versionsnummer hoeher als die aktuelle ist.
        if (number > currentVersion)
          updates.add(current);
      }
      catch (Exception e)
      {
        Logger.error("invalid update filename: " + current.getName() + ", skipping");
        continue;
      }
    }

    // Keine Updates gefunden
    if (updates.size() == 0)
    {
      Logger.info("no new updates found");
      return;
    }

    // Wir fuehren die Updates aus.
    Logger.info("found " + updates.size() + " update files");
    for (int i=0;i<updates.size();++i)
    {
      
      File f = (File) updates.get(i);
      execute(f); // Update ausfuehren

      // Neue Versionsnummer an Provider mitteilen
      try
      {
        String name = f.getName();
        int number = Integer.parseInt(name.substring(0,name.lastIndexOf(".")));
        provider.setNewVersion(number);
      }
      catch (Exception e)
      {
        throw new ApplicationException(e);
      }
    }
    Logger.info("update completed");
  }
  
  /**
   * Fuehrt ein einzelnes Update durch.
   * @param update das auszufuehrende Update.
   * @throws ApplicationException
   */
  private void execute(File update) throws ApplicationException
  {
    String filename = update.getName();
    
    // SQL-Script direkt ausfuehren.
    if (filename.endsWith(".sql"))
    {
      Reader reader = null;
      try
      {
        reader = new FileReader(update);
        Logger.info("  executing " + filename);
        ScriptExecutor.execute(reader,provider.getConnection(),provider.getProgressMonitor());
      }
      catch (Exception e)
      {
        throw new ApplicationException(e);
      }
      finally
      {
        if (reader != null)
        {
          try {
            reader.close();
          } catch (Exception e) {/*useless*/}
        }
      }
      return;
    }

    // .class-File. Also Klasse laden, auf "Update" casten
    // und ausfuehren.
    if (filename.endsWith(".class"))
    {
      try
      {
        Class clazz = this.loader.findClass(update.getAbsolutePath());
        Update u = (Update) clazz.newInstance();
        Logger.info("  executing " + u.getName());
        u.execute(this.provider);
      }
      catch (ApplicationException ae)
      {
        throw ae;
      }
      catch (Exception e)
      {
        throw new ApplicationException(e);
      }
    }

    Logger.error("unknown update format: " + filename);
  }
  
  /**
   * Ein eigener Classloader, um aus einer .class-Datei
   * die Klasse zu laden.
   */
  private class MyClassloader extends ClassLoader
  {
    /**
     * @see java.lang.ClassLoader#findClass(java.lang.String)
     */
    public Class findClass(String name)
    {
      InputStream is = null;
      try
      {
        // Datei in Byte-Array laden
        File f = new File(name);
        is = new FileInputStream(f);
        byte[] data = new byte[(int)f.length()];
        is.read(data);
        
        // ".class" abschneiden
        name = name.substring(0,name.lastIndexOf("."));
        
        // Byte-Array von Parent-Classloader laden
        return defineClass(name, data, 0, data.length);
      }
      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
      finally
      {
        if (is != null)
        {
          try {
            is.close();
          } catch (Exception e) {/* useless */};
        }
      }
    }
  }

}


/**********************************************************************
 * $Log: Updater.java,v $
 * Revision 1.1  2007/10/01 23:16:56  willuhn
 * @N Erste voellig ungetestete Version eines generischen Updaters.
 *
 **********************************************************************/