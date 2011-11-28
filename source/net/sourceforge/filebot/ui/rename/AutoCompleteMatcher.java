
package net.sourceforge.filebot.ui.rename;


import java.awt.Component;
import java.io.File;
import java.util.List;
import java.util.Locale;

import net.sourceforge.filebot.similarity.Match;


interface AutoCompleteMatcher {
	
	List<Match<File, ?>> match(List<File> files, Locale locale, boolean autodetection, Component parent) throws Exception;
}
