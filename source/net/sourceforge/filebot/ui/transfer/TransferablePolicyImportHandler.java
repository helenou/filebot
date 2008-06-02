
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.Transferable;
import java.awt.dnd.InvalidDnDOperationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;

import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicy;


public class TransferablePolicyImportHandler implements ImportHandler {
	
	private final TransferablePolicy transferablePolicy;
	
	
	public TransferablePolicyImportHandler(TransferablePolicy transferablePolicy) {
		this.transferablePolicy = transferablePolicy;
	}
	
	private boolean canImportCache = false;
	
	
	@Override
	public boolean canImport(TransferSupport support) {
		if (support.isDrop())
			support.setShowDropLocation(false);
		
		Transferable t = support.getTransferable();
		
		try {
			canImportCache = transferablePolicy.accept(t);
		} catch (InvalidDnDOperationException e) {
			// for some reason the last transferable has no drop current
		}
		
		return canImportCache;
	}
	

	@Override
	public boolean importData(TransferSupport support) {
		boolean add = false;
		
		if (support.isDrop() && (support.getDropAction() == TransferHandler.COPY))
			add = true;
		
		Transferable t = support.getTransferable();
		
		try {
			transferablePolicy.handleTransferable(t, add);
		} catch (Exception e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
		}
		
		return true;
	}
	
}
