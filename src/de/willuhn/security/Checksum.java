/**********************************************************************
 * $Source: /cvsroot/jameica/util/src/de/willuhn/security/Checksum.java,v $
 * $Revision: 1.3 $
 * $Date: 2009/01/16 16:39:56 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/
package de.willuhn.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.willuhn.util.Base64;

/**
 * Hilfsklasse mit statischen Methoden zur Erzeugung von Checksummen.
 */
public class Checksum
{
  /**
   * Konstante fuer SHA1-Checksumme.
   */
  public final static String SHA1 = "SHA1";

  /**
   * Konstante fuer MD5-Checksumme.
   */
  public final static String MD5 = "MD5";

  private Checksum()
  {
  }

  /**
   * Liefert eine MD5-Checksumme der Daten.
   * @param text
   * @return die Checksumme.
   * @throws NoSuchAlgorithmException
   * @deprecated Bitte Checksum#checksum(byte[],String) verwenden.
   */
  public final static String md5(byte[] text) throws NoSuchAlgorithmException
	{
    return checksum(text,Checksum.MD5);
	}
  
  /**
   * Liefert eine SHA1-Checksumme der Daten.
   * @param text
   * @return die Checksumme.
   * @throws NoSuchAlgorithmException
   */
  public final static String checksum(byte[] text, String alg) throws NoSuchAlgorithmException
  {
    MessageDigest md = MessageDigest.getInstance(alg);
    return Base64.encode(md.digest(text));
  }
  
  /**
   * Liefert eine Checksumme der Daten im Base64-Format.
   * @param data InputStream mit den Daten.
   * Hinweis: Die Funktion kuemmert sich NICHT um das Schliessen des Streams.
   * @param alg Algorithmus.
   * @return die Checksumme.
   * @see Checksum#MD5
   * @see Checksum#SHA1
   * @throws NoSuchAlgorithmException
   * @throws IOException
   */
  public static String checksum(InputStream data, String alg) throws NoSuchAlgorithmException, IOException
  {
    MessageDigest md = MessageDigest.getInstance(alg);
    byte[] buf = new byte[4096];
    int read = 0;
    while ((read = data.read(buf)) != -1)
      md.update(buf,0,read);

    return Base64.encode(md.digest());
  }
}


/**********************************************************************
 * $Log: Checksum.java,v $
 * Revision 1.3  2009/01/16 16:39:56  willuhn
 * @N Funktion zum Erzeugen von SHA1-Checksummen
 * @N Funktion zum Erzeugen von Checksummen aus InputStreams
 *
 * Revision 1.2  2005/03/09 01:06:20  web0
 * @D javadoc fixes
 *
 * Revision 1.1  2005/02/01 17:15:07  willuhn
 * *** empty log message ***
 *
 **********************************************************************/