/************************************************************************************************
 *  _________          _     ____          _           __        __    _ _      _   _   _ ___
 * |__  / ___|__ _ ___| |__ / ___|_      _(_)_ __   __ \ \      / /_ _| | | ___| |_| | | |_ _|
 *   / / |   / _` / __| '_ \\___ \ \ /\ / / | '_ \ / _` \ \ /\ / / _` | | |/ _ \ __| | | || |
 *  / /| |__| (_| \__ \ | | |___) \ V  V /| | | | | (_| |\ V  V / (_| | | |  __/ |_| |_| || |
 * /____\____\__,_|___/_| |_|____/ \_/\_/ |_|_| |_|\__, | \_/\_/ \__,_|_|_|\___|\__|\___/|___|
 *                                                 |___/
 *
 * Copyright (c) 2016 Ivan Vaklinov <ivan@vaklinov.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 **********************************************************************************/
package com.vaklinov.zcashui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * Utilities - generally reusable across classes.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class Util
{
	// Compares two string arrays (two dimensional).
	public static boolean arraysAreDifferent(String ar1[][], String ar2[][])
	{
		if (ar1 == null)
		{
			if (ar2 != null)
			{
				return true;
			}
		} else if (ar2 == null)
		{
			return true;
		}
		
		if (ar1.length != ar2.length)
		{
			return true;
		}
		
		for (int i = 0; i < ar1.length; i++)
		{
			if (ar1[i].length != ar2[i].length)
			{
				return true;
			}
			
			for (int j = 0; j < ar1[i].length; j++)
			{
				String s1 = ar1[i][j];
				String s2 = ar2[i][j];
				
				if (s1 == null)
				{
					if (s2 != null)
					{
						return true;
					}
				} else if (s2 == null)
				{
					return true;
				} else
				{
					if (!s1.equals(s2))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	
	// Turns a 1.0.7+ error message to a an old JSOn style message
	// info - new style error message
	public static JsonObject getJsonErrorMessage(String info)
	    throws IOException
	{
    	JsonObject jInfo = new JsonObject();
    	
    	// Error message here comes from ZCash 1.0.7+ and is like:
    	//zcash-cli getinfo
    	//error code: -28
    	//error message:
    	//Loading block index...
    	LineNumberReader lnr = new LineNumberReader(new StringReader(info));
    	int errCode =  Integer.parseInt(lnr.readLine().substring(11).trim());
    	jInfo.set("code", errCode);
    	lnr.readLine();
    	jInfo.set("message", lnr.readLine().trim());
    	
    	return jInfo;
	}
	
	
	/**
	 * Escapes a text value to a form suitable to be displayed in HTML content. Important control
	 * characters are replaced with entities.
	 * 
	 * @param inputValue th value to escape
	 * 
	 * @return the "safe" value to display.
	 */
	public static String escapeHTMLValue(String inputValue) 
	{
	    StringBuilder outputValue = new StringBuilder();
	    for (char c : inputValue.toCharArray()) 
	    {
	        if ((c > 127) || (c == '"') || (c == '<') || (c == '>') || (c == '&')) 
	        {
	            outputValue.append("&#");
	            outputValue.append((int)c);
	            outputValue.append(';');
	        } else 
	        {
	            outputValue.append(c);
	        }
	    }
	    return outputValue.toString();
	}
	
	
	public static boolean stringIsEmpty(String s)
	{
		return (s == null) || (s.length() <= 0);
	}
	
	
	public static String decodeHexMemo(String memoHex)
		throws UnsupportedEncodingException
	{
        // Skip empty memos
        if (memoHex.startsWith("f600000000"))
        {
        	return null;
        }
        
        // should be read with UTF-8
        byte[] bytes = new byte[(memoHex.length() / 2) + 1];
        int count = 0;
        
        for (int j = 0; j < memoHex.length(); j += 2)
        {
            String str = memoHex.substring(j, j + 2);
            bytes[count++] = (byte)Integer.parseInt(str, 16);
        }
        
        // Remove zero padding
        // TODO: may cause problems if UNICODE chars have trailing ZEROS
        while (count > 0)
        {
        	if (bytes[count - 1] == 0)
        	{
        		count--;
        	} else
        	{
        		break;
        	}
        }
        
        return new String(bytes, 0, count, "UTF-8");
	}
	
	
	public static String encodeHexString(String str)
		throws UnsupportedEncodingException
	{
		return encodeHexArray(str.getBytes("UTF-8"));
	}
	
	
	public static String encodeHexArray(byte array[])
	{
		StringBuilder encoded = new StringBuilder();
		for (byte c : array)
		{
			String hexByte = Integer.toHexString((int)c);
			if (hexByte.length() < 2)
			{
				hexByte = "0" + hexByte;
			} else if (hexByte.length() > 2)
			{
				hexByte = hexByte.substring(hexByte.length() - 2, hexByte.length()); 
			}
			encoded.append(hexByte);
		}
		
		return encoded.toString();
	}
	
	
	/**
	 * Maintains a set of old copies for a file.
	 * For a file dir/file, the old versions are dir/file.1, dir/file.2 etc. up to 9.
	 * 
	 * @param dir base directory
	 * 
	 * @param file name of the original file
	 */
	public static void renameFileForMultiVersionBackup(File dir, String file)
	{
		final int VERSION_COUNT = 9;
		
		// Delete last one if it exists
		File last = new File(dir, file + "." + VERSION_COUNT);
		if (last.exists())
		{
			last.delete();
		}
		
		// Iterate and rename
		for (int i = VERSION_COUNT - 1; i >= 1; i--)
		{
			File f = new File(dir, file + "." + i);
			int newIndex = i + 1;
			if (f.exists())
			{
				f.renameTo(new File(dir, file + "." + newIndex));
			}
		}
		
		// Rename last one
		File orig = new File(dir, file);
		if (orig.exists())
		{
			orig.renameTo(new File(dir, file + ".1"));
		}
	}
	
	
	public static JsonObject parseJsonObject(String json)
		throws IOException
	{
		try
		{
			return Json.parse(json).asObject();
		} catch (RuntimeException rte)
		{
			throw new IOException(rte);
		}
	}
	
	
	public static JsonObject parseJsonObject(Reader r)
		throws IOException
	{
		try
		{
			return Json.parse(r).asObject();
		} catch (RuntimeException rte)
		{
			throw new IOException(rte);
		}
	}

	
	public static byte[] calculateSHA256Digest(byte[] input)
		throws IOException 
	{
        try 
        {
        	MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        	DigestInputStream dis = new DigestInputStream(new ByteArrayInputStream(input), sha256);
            byte [] temp = new byte[0x1 << 13];
            byte[] digest;
            while(dis.read(temp) >= 0);
            {
            	digest = sha256.digest();
            }
            
            return digest;
        } catch (NoSuchAlgorithmException impossible) 
        {
            throw new IOException(impossible);
        }
	}
	
	
	public static String convertGroupPhraseToZPrivateKey(String phrase)
		throws IOException
	{
		byte phraseBytes[] = phrase.getBytes("UTF-8");
		byte phraseDigest[] = calculateSHA256Digest(phraseBytes);
		
		phraseDigest[0] &= (byte)0x0f;
		
		//System.out.println(encodeHexArray(phraseDigest));
		
		byte base58Input[] = new byte[phraseDigest.length + 2];
		base58Input[0] = (byte)0xab;
		base58Input[1] = (byte)0x36;
		System.arraycopy(phraseDigest, 0, base58Input, 2, phraseDigest.length);
		
		// Do a double SHA356 to get a checksum for the data to encode
		byte shaStage1[] = calculateSHA256Digest(base58Input);
		byte checksum[] = calculateSHA256Digest(shaStage1);
		
		byte base58CheckInput[] = new byte[base58Input.length + 4];
		System.arraycopy(base58Input, 0, base58CheckInput, 0, base58Input.length);
		System.arraycopy(checksum, 0, base58CheckInput, base58Input.length, 4);
		
		// Call BitcoinJ via reflection - and report error if missing
		try
		{
			Class base58Class = Class.forName("org.bitcoinj.core.Base58");
			Method encode = base58Class.getMethod("encode", byte[].class);
			return (String)encode.invoke(null, base58CheckInput);
		} catch (Exception e)
		{
			throw new IOException(
				"There was a problem invoking the BitcoinJ library to do Base58 encoding. " +
			    "Make sure the bitcoinj-core-0.14.5.jar is available!", e);
		}
	}
}
