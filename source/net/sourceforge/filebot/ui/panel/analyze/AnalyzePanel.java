
package net.sourceforge.filebot.ui.panel.analyze;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.FileTransferableMessageHandler;
import net.sourceforge.tuned.MessageHandler;


public class AnalyzePanel extends FileBotPanel {
	
	private final FileTreePanel fileTreePanel = new FileTreePanel();
	private final JTabbedPane toolsPanel = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
	
	private final MessageHandler messageHandler = new FileTransferableMessageHandler(this, fileTreePanel.getTransferablePolicy());
	
	
	public AnalyzePanel() {
		super("Analyze", ResourceManager.getIcon("panel.analyze"));
		
		toolsPanel.setBorder(BorderFactory.createTitledBorder("Tools"));
		
		setLayout(new MigLayout("insets 0, gapx 50, fill"));
		
		add(fileTreePanel, "grow, sizegroup column");
		add(toolsPanel, "grow, sizegroup column");
		
		addTool(new TypeTool());
		addTool(new SplitTool());
		
		fileTreePanel.addPropertyChangeListener("filetree", filetreeListener);
	}
	

	private void addTool(Tool<?> tool) {
		toolsPanel.addTab(tool.getName(), tool);
	}
	

	@Override
	public MessageHandler getMessageHandler() {
		return messageHandler;
	}
	
	private final PropertyChangeListener filetreeListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			// stopped loading, refresh tools
			for (int i = 0; i < toolsPanel.getTabCount(); i++) {
				Tool<?> tool = (Tool<?>) toolsPanel.getComponentAt(i);
				tool.setSourceModel(fileTreePanel.getFileTree().getRoot());
			}
		}
	};
	
}
