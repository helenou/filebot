
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import net.sourceforge.filebot.FileBotUtil;


public class SaveableExportHandler implements ExportHandler {
	
	private Saveable saveable;
	
	private String tmpdir = System.getProperty("java.io.tmpdir");
	
	
	public SaveableExportHandler(Saveable saveable) {
		this.saveable = saveable;
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public void exportDone(JComponent source, Transferable data, int action) {
		try {
			List<File> files = (List<File>) data.getTransferData(DataFlavor.javaFileListFlavor);
			
			for (File file : files) {
				if (file.exists())
					file.deleteOnExit();
			}
		} catch (Exception e) {
			// should not happen
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString());
		}
	}
	

	@Override
	public int getSourceActions(JComponent c) {
		if ((saveable == null) || !saveable.isSaveable())
			return TransferHandler.NONE;
		
		return TransferHandler.MOVE | TransferHandler.COPY;
	}
	

	@Override
	public Transferable createTransferable(JComponent c) {
		try {
			File temporaryFile = new File(tmpdir, FileBotUtil.validateFileName(saveable.getDefaultFileName()));
			temporaryFile.createNewFile();
			
			saveable.save(temporaryFile);
			return new FileTransferable(temporaryFile);
		} catch (IOException e) {
			// should not happen
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString());
		}
		
		return null;
	}
}
