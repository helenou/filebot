
package net.sourceforge.filebot.resources;


import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;


public class ResourceManager {
	
	private static HashMap<String, String> aliasMap = new HashMap<String, String>();
	
	static {
		aliasMap.put("tab.loading", "tab.loading.gif");
		aliasMap.put("tab.history", "action.find.png");
		
		aliasMap.put("loading", "loading.gif");
	}
	
	
	public static Image getImage(String name) {
		try {
			return ImageIO.read(getResource(name));
		} catch (IOException e) {
			return null;
		}
	}
	

	public static ImageIcon getIcon(String name) {
		return new ImageIcon(getResource(name));
	}
	

	private static URL getResource(String name) {
		String resource = null;
		
		if (aliasMap.containsKey(name))
			resource = aliasMap.get(name);
		else
			resource = name + ".png";
		
		return ResourceManager.class.getResource(resource);
	}
}
