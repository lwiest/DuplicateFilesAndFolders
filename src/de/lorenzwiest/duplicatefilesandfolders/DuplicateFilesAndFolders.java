/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Lorenz Wiest
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package de.lorenzwiest.duplicatefilesandfolders;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import de.lorenzwiest.duplicatefilesandfolders.WrappedLabel.Type;

public class DuplicateFilesAndFolders extends ApplicationWindow {
	final public static int INDENT = 5;
	final private static int INDENT2 = INDENT * 2;

	final private static Color COLOR_EVEN_LINE = new Color(Display.getCurrent(), 240, 240, 255);
	final private static Color COLOR_ODD_LINE = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
	final private static Color COLOR_BLACK = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
	final private static Color COLOR_GREY = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);

	final private static Class<DuplicateFilesAndFolders> CLAZZ = DuplicateFilesAndFolders.class;
	final public static Image IMG_ICON = ImageDescriptor.createFromFile(CLAZZ, "icons/icon.png").createImage();
	final private static Image IMG_FOLDER = ImageDescriptor.createFromFile(CLAZZ, "icons/folder.png").createImage();
	final private static Image IMG_FOLDER_WARNING = ImageDescriptor.createFromFile(CLAZZ, "icons/folder.warning.png").createImage();
	final private static Image IMG_FILE = ImageDescriptor.createFromFile(CLAZZ, "icons/file.png").createImage();
	final private static Image IMG_FILE_WARNING = ImageDescriptor.createFromFile(CLAZZ, "icons/file.warning.png").createImage();
	final private static Image IMG_FOLDER_DIS = ImageDescriptor.createFromFile(CLAZZ, "icons/folder_disabled.png").createImage();
	final private static Image IMG_FOLDER_WARNING_DIS = ImageDescriptor.createFromFile(CLAZZ, "icons/folder.warning_disabled.png").createImage();
	final private static Image IMG_FILE_DIS = ImageDescriptor.createFromFile(CLAZZ, "icons/file_disabled.png").createImage();
	final private static Image IMG_FILE_WARNING_DIS = ImageDescriptor.createFromFile(CLAZZ, "icons/file.warning_disabled.png").createImage();

	private MessageDigest messageDigest;
	private Node rootNode;
	private Map<String /* md5 hash */, List<Node>> mapDuplicates = new HashMap<String, List<Node>>();
	private FolderTableElement[] folderTableElements = new FolderTableElement[0];
	private FileTableElement[] fileTableElements = new FileTableElement[0];
	private Map<Node, FolderTableElement> nodeToFolderTableElement = new HashMap<Node, FolderTableElement>();
	private Map<Node, FileTableElement> nodeToFileTableElement = new HashMap<Node, FileTableElement>();
	private long totalFilesSize;

	private Text txtFolderToScan;
	private Button btnFindDuplicates;
	private WrappedLabel warningLabel;
	private TabItem tabItemFolders;
	private CheckboxTableViewer tbvFolders;
	private TabItem tabItemFiles;
	private CheckboxTableViewer tbvFiles;
	private WrappedLabel infoLabel;
	private Button btnDelete;

	public DuplicateFilesAndFolders() {
		super(null);

		try {
			this.messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// empty
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Duplicate Files & Folders 1.0");
		shell.setImage(IMG_ICON);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(650, 600);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite);
		GridLayoutFactory.swtDefaults().margins(0, 0).extendedMargins(INDENT2, INDENT2, INDENT, INDENT2).spacing(SWT.DEFAULT, INDENT2).applyTo(composite);

		createFindDuplicatesControlsComposite(composite);
		createSeparator(composite);
		createMessageField(composite);
		createTabFolder(composite);
		createInfoAndDeleteButton(composite);

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				String initialPath = Utils.getInitialFolderToScanPath();
				DuplicateFilesAndFolders.this.txtFolderToScan.setText(initialPath);
				DuplicateFilesAndFolders.this.txtFolderToScan.setSelection(initialPath.length());
			}
		});

		return composite;
	}

	private void createFindDuplicatesControlsComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(composite);
		GridLayoutFactory.swtDefaults().margins(0, 0).extendedMargins(0, 0, INDENT, 0).applyTo(composite);

		createFindDuplicatesControls(composite);
	}

	private void createSeparator(Composite composite) {
		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(separator);
	}

	private void createFindDuplicatesControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(composite);
		GridLayoutFactory.swtDefaults().numColumns(4).margins(0, 0).applyTo(composite);

		Label lblFolderToScan = new Label(composite, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lblFolderToScan);
		lblFolderToScan.setText("Folder to scan:");

		this.txtFolderToScan = new Text(composite, SWT.BORDER);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(this.txtFolderToScan);
		this.txtFolderToScan.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateFindDuplicatesSelected();
			}
		});

		Button btnBrowse = new Button(composite, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).applyTo(btnBrowse);
		btnBrowse.setText(" ... ");
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseSelected();
			}
		});

		this.btnFindDuplicates = new Button(composite, SWT.NONE);
		this.btnFindDuplicates.setText("Find Duplicates");
		GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).hint(Utils.getButtonSize(this.btnFindDuplicates)).indent(2 * INDENT2, 0).applyTo(this.btnFindDuplicates);
		this.btnFindDuplicates.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				findDuplicatesSelected();
			}
		});

		getShell().setDefaultButton(this.btnFindDuplicates);
	}

	private void updateFindDuplicatesSelected() {
		File file = new File(this.txtFolderToScan.getText());
		boolean isFolderExisting = (file != null) && file.exists() && file.isDirectory();
		this.btnFindDuplicates.setEnabled(isFolderExisting);
	}

	private void browseSelected() {
		DirectoryDialog folderDialog = new DirectoryDialog(getShell());
		folderDialog.setFilterPath(this.txtFolderToScan.getText());
		folderDialog.setText("Open Folder");
		folderDialog.setMessage("Select a folder to scan");
		String strFolder = folderDialog.open();
		if (strFolder != null) {
			this.txtFolderToScan.setText(strFolder);
		}
		updateFindDuplicatesSelected();
	}

	private void findDuplicatesSelected() {
		InspectionDialog dialog = new InspectionDialog(getShell(), this.txtFolderToScan.getText());
		if (dialog.open() == Window.OK) {
			this.rootNode = dialog.getRootNode();
			this.mapDuplicates = dialog.getMapDuplicates();

			updateUi();

			autoSelectTab();
			autoSizeViewerColumns();
		}
	}

	private void hashFoldersRecursively(Node node) {
		if (node.getFile().isFile()) {
			return;
		}

		long totalSize = 0;
		int totalNumChildren = 0;
		List<String> sortedHashes = new ArrayList<String>();
		for (Node child : node.getChildren()) {
			hashFoldersRecursively(child);

			sortedHashes.add(child.getHash());
			totalSize += child.getSize();

			if (child.getFile().isFile()) {
				totalNumChildren++;
			} else {
				totalNumChildren += child.getTotalChildrenCount() + 1; // children + this folder
			}
		}
		node.setSize(totalSize);
		node.setNumTotalChildren(totalNumChildren);

		this.messageDigest.reset();
		this.messageDigest.update("<folder>".getBytes(Charset.defaultCharset())); // folder's MD5 seed, includes illegal characters for filepaths
		Collections.sort(sortedHashes);
		for (String sortedHash : sortedHashes) {
			this.messageDigest.update(sortedHash.getBytes(Charset.defaultCharset()));
		}
		String strHash = Utils.getHash(node.getFile(), this.messageDigest);
		node.setHash(strHash);
	}

	private void calculateTotalFilesSize() {
		this.totalFilesSize = Utils.calcTotalFilesSizeRecursively(this.rootNode);
	}

	private void clearViewers() {
		this.mapDuplicates.clear();
		this.nodeToFolderTableElement.clear();
		this.nodeToFileTableElement.clear();
		this.folderTableElements = new FolderTableElement[0];
		this.fileTableElements = new FileTableElement[0];
		updateViewers();
	}

	private void searchDuplicates(Node node) {
		searchDuplicatesRecursively(node);
		pruneNonDuplicates();
	}

	private void searchDuplicatesRecursively(Node node) {
		String strHash = node.getHash();
		if (this.mapDuplicates.containsKey(strHash) == false) {
			this.mapDuplicates.put(strHash, new ArrayList<Node>());
		}
		this.mapDuplicates.get(strHash).add(node);
		for (Node childNode : node.getChildren()) {
			searchDuplicatesRecursively(childNode);
		}
	}

	private void pruneNonDuplicates() {
		String[] copyOfStrHashes = this.mapDuplicates.keySet().toArray(new String[0]);
		for (String strHash : copyOfStrHashes) {
			List<Node> duplicatesList = this.mapDuplicates.get(strHash);
			if (duplicatesList.size() < 2) {
				this.mapDuplicates.remove(strHash);
			}
		}
	}

	private void populateViewers() {
		List<List<Node>> duplicateFolderLists = new ArrayList<List<Node>>();
		List<List<Node>> duplicateFileLists = new ArrayList<List<Node>>();

		for (Map.Entry<String, List<Node>> entry : this.mapDuplicates.entrySet()) {
			List<Node> duplicateList = entry.getValue();
			boolean isFile = duplicateList.get(0).getFile().isFile();
			if (isFile) {
				duplicateFileLists.add(duplicateList);
			} else {
				duplicateFolderLists.add(duplicateList);
			}
		}

		// pre-sort duplicate folders by size and total child count
		Collections.sort(duplicateFolderLists, new Comparator<List<Node>>() {
			@Override
			public int compare(List<Node> list1, List<Node> list2) {
				Node node1 = list1.get(0);
				Node node2 = list2.get(0);

				int diffSize = (int) (node2.getSize() - node1.getSize());
				if (diffSize != 0) {
					return diffSize;
				}

				int diffTotalChildrenCount = node2.getTotalChildrenCount() - node1.getTotalChildrenCount();
				return diffTotalChildrenCount;
			}
		});

		// pre-sort duplicate files by size
		Collections.sort(duplicateFileLists, new Comparator<List<Node>>() {
			@Override
			public int compare(List<Node> list1, List<Node> list2) {
				Node node1 = list1.get(0);
				Node node2 = list2.get(0);

				int diffSize = (int) (node2.getSize() - node1.getSize());
				return diffSize;
			}
		});

		int folderGroupIndex = 0;
		List<FolderTableElement> folderTableElements = new ArrayList<FolderTableElement>();
		for (List<Node> duplicateFolderList : duplicateFolderLists) {
			for (Node node : duplicateFolderList) {
				FolderTableElement fElement = new FolderTableElement(node, folderGroupIndex);
				this.nodeToFolderTableElement.put(node, fElement);
				folderTableElements.add(fElement);
			}
			folderGroupIndex++;
		}
		this.folderTableElements = folderTableElements.toArray(new FolderTableElement[0]);

		int fileGroupIndex = 0;
		List<FileTableElement> fileTableElements = new ArrayList<FileTableElement>();
		for (List<Node> duplicateFileList : duplicateFileLists) {
			for (Node node : duplicateFileList) {
				FileTableElement fElement = new FileTableElement(node, fileGroupIndex);
				this.nodeToFileTableElement.put(node, fElement);
				fileTableElements.add(fElement);
			}
			fileGroupIndex++;
		}
		this.fileTableElements = fileTableElements.toArray(new FileTableElement[0]);

		updateViewers();
	}

	private void autoSelectTab() {
		TabFolder tabFolder = this.tabItemFolders.getParent();
		TabItem selectedTab = tabFolder.getSelection()[0];
		if ((selectedTab == this.tabItemFolders) && (this.folderTableElements.length == 0)) {
			if (this.fileTableElements.length > 0) {
				tabFolder.setSelection(this.tabItemFiles);
			}
		} else if ((selectedTab == this.tabItemFiles) && (this.fileTableElements.length == 0)) {
			if (this.folderTableElements.length > 0) {
				tabFolder.setSelection(this.tabItemFolders);
			}
		}
	}

	private void autoSizeViewerColumns() {
		autoSizeViewerColumns(this.tbvFolders, this.tabItemFolders);
		autoSizeViewerColumns(this.tbvFiles, this.tabItemFiles);
	}

	private void autoSizeViewerColumns(final TableViewer viewer, final TabItem tabItem) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				Table table = viewer.getTable();
				table.setRedraw(false);

				for (int col = 0; col < table.getColumnCount(); col++) {
					TableColumn tableColumn = table.getColumn(col);
					if (tableColumn.getResizable()) {
						tableColumn.pack();
					}
				}

				// hack to update non-visible tabs
				TabItem[] selection = tabItem.getParent().getSelection();
				tabItem.getParent().setSelection(tabItem);

				int tableWidth = table.getClientArea().width;
				int totalColumnWidth = 0;
				for (int col = 0; col < table.getColumnCount(); col++) {
					totalColumnWidth += table.getColumn(col).getWidth();
				}
				int lastColumnIndex = table.getColumnCount() - 1;
				int lastColumnWidth = table.getColumn(lastColumnIndex).getWidth();
				int newLastColumnWidth = lastColumnWidth + (tableWidth - totalColumnWidth);
				table.getColumn(lastColumnIndex).setWidth(newLastColumnWidth);

				tabItem.getParent().setSelection(selection);

				table.setRedraw(true);
			}
		});
	}

	private void createMessageField(Composite parent) {
		this.warningLabel = new WrappedLabel(parent);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(this.warningLabel.getControl());
	}

	private void createTabFolder(Composite parent) {
		TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(tabFolder);

		this.tabItemFolders = new TabItem(tabFolder, SWT.NONE);
		this.tabItemFolders.setText("Duplicate Folders");
		this.tabItemFolders.setImage(IMG_FOLDER);
		this.tabItemFolders.setControl(createTabItemDuplicateFoldersContent(tabFolder));

		this.tabItemFiles = new TabItem(tabFolder, SWT.NONE);
		this.tabItemFiles.setText("Duplicate Files");
		this.tabItemFiles.setImage(IMG_FILE);
		this.tabItemFiles.setControl(createTabItemDuplicateFilesContent(tabFolder));
	}

	private Control createTabItemDuplicateFoldersContent(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite);
		GridLayoutFactory.swtDefaults().applyTo(composite);

		this.tbvFolders = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(this.tbvFolders.getTable());
		this.tbvFolders.getTable().setHeaderVisible(true);
		this.tbvFolders.getTable().setLinesVisible(true);
		this.tbvFolders.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				tbvFoldersCheckStateChanged(event);
			}
		});
		this.tbvFolders.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isGrayed(Object element) {
				return ((FolderTableElement) element).isGrayed();
			}

			@Override
			public boolean isChecked(Object element) {
				return ((FolderTableElement) element).isChecked();
			}
		});

		this.tbvFolders.setLabelProvider(new FolderTableLabelProvider());
		this.tbvFolders.setContentProvider(new ArrayContentProvider());
		this.tbvFolders.setSorter(new FolderTableViewerSorter());
		this.tbvFolders.setUseHashlookup(true);
		this.tbvFolders.setInput(this.folderTableElements);

		String[] columnNames = new String[] {
			"", "Total Size", "Items", "Folder" };
		int[] columnAlignments = new int[] {
			SWT.LEFT, SWT.RIGHT, SWT.RIGHT, SWT.LEFT };
		int[] columnWidths = new int[] {
			48, 100, 100, 100 };
		boolean[] isColumnResizable = new boolean[] {
			false, true, true, true };

		for (int col = 0; col < columnNames.length; col++) {
			TableColumn tableColumn = new TableColumn(this.tbvFolders.getTable(), columnAlignments[col]);
			tableColumn.setText(columnNames[col]);
			tableColumn.setResizable(isColumnResizable[col]);
			tableColumn.setWidth(columnWidths[col]);
		}
		autoSizeViewerColumns(this.tbvFolders, this.tabItemFolders);

		addOpenFolderActionContextMenu(this.tbvFolders);

		return composite;
	}

	private void tbvFoldersCheckStateChanged(CheckStateChangedEvent event) {
		FolderTableElement fElement = (FolderTableElement) event.getElement();

		boolean isGrayed = DuplicateFilesAndFolders.this.tbvFolders.getGrayed(fElement);
		if (isGrayed) {
			DuplicateFilesAndFolders.this.tbvFolders.refresh(fElement);
			return;
		}

		this.tbvFolders.getTable().setRedraw(false);

		boolean isChecked = event.getChecked();
		fElement.setChecked(isChecked);

		if (fElement.isGrayed() == false) {
			forceGrayed(fElement.getNode(), isChecked);
		}

		String strHash = fElement.getNode().getHash();
		List<Node> duplicateNodes = DuplicateFilesAndFolders.this.mapDuplicates.get(strHash);

		boolean isWarning = true;
		for (Node duplicateNode : duplicateNodes) {
			boolean isSelected = DuplicateFilesAndFolders.this.nodeToFolderTableElement.get(duplicateNode).isChecked();
			if (isSelected == false) {
				isWarning = false;
				break;
			}
		}

		for (Node duplicateNode : duplicateNodes) {
			FolderTableElement fElement2 = DuplicateFilesAndFolders.this.nodeToFolderTableElement.get(duplicateNode);
			fElement2.setWarning(isWarning);
			DuplicateFilesAndFolders.this.tbvFolders.refresh(fElement2);
		}

		this.tbvFolders.getTable().setRedraw(true);

		updateMessage();
	}

	private void forceGrayed(Node folderNode, boolean isChecked) {
		for (Node childNode : folderNode.getChildren()) {
			forceChildrenGrayedRecursively(childNode, isChecked);
		}
	}

	private void forceChildrenGrayedRecursively(Node node, boolean isChecked) {
		if (node.getFile().isFile()) {
			if (this.nodeToFileTableElement.containsKey(node)) {
				FileTableElement fElement = this.nodeToFileTableElement.get(node);
				fElement.setGrayed(isChecked);
				fElement.setChecked(isChecked);

				String strHash = fElement.getNode().getHash();
				List<Node> duplicateNodes = DuplicateFilesAndFolders.this.mapDuplicates.get(strHash);

				boolean isWarning = true;
				for (Node duplicateNode : duplicateNodes) {
					boolean isSelected = DuplicateFilesAndFolders.this.nodeToFileTableElement.get(duplicateNode).isChecked();
					if (isSelected == false) {
						isWarning = false;
						break;
					}
				}

				for (Node duplicateNode : duplicateNodes) {
					FileTableElement fElement2 = DuplicateFilesAndFolders.this.nodeToFileTableElement.get(duplicateNode);
					fElement2.setWarning(isWarning);
					DuplicateFilesAndFolders.this.tbvFiles.refresh(fElement2);
				}

				this.tbvFiles.refresh(fElement);
			}
			return;
		}

		if (this.nodeToFolderTableElement.containsKey(node)) {
			FolderTableElement fElement = this.nodeToFolderTableElement.get(node);
			fElement.setGrayed(isChecked);
			fElement.setChecked(isChecked);

			String strHash = fElement.getNode().getHash();
			List<Node> duplicateNodes = DuplicateFilesAndFolders.this.mapDuplicates.get(strHash);

			boolean isWarning = true;
			for (Node duplicateNode : duplicateNodes) {
				boolean isSelected = DuplicateFilesAndFolders.this.nodeToFolderTableElement.get(duplicateNode).isChecked();
				if (isSelected == false) {
					isWarning = false;
					break;
				}
			}

			for (Node duplicateNode : duplicateNodes) {
				FolderTableElement fElement2 = DuplicateFilesAndFolders.this.nodeToFolderTableElement.get(duplicateNode);
				fElement2.setWarning(isWarning);
				DuplicateFilesAndFolders.this.tbvFolders.refresh(fElement2);
			}

			this.tbvFolders.refresh(fElement);
		}

		for (Node childNode : node.getChildren()) {
			forceChildrenGrayedRecursively(childNode, isChecked);
		}
	}

	private Control createTabItemDuplicateFilesContent(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite);

		this.tbvFiles = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(this.tbvFiles.getTable());
		this.tbvFiles.getTable().setHeaderVisible(true);
		this.tbvFiles.getTable().setLinesVisible(true);
		this.tbvFiles.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				tbvFilesCheckStateChanged(event);
			}
		});
		this.tbvFiles.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isGrayed(Object element) {
				return ((FileTableElement) element).isGrayed();
			}

			@Override
			public boolean isChecked(Object element) {
				return ((FileTableElement) element).isChecked();
			}
		});

		this.tbvFiles.setLabelProvider(new FileTableLabelProvider());
		this.tbvFiles.setContentProvider(new ArrayContentProvider());
		this.tbvFiles.setSorter(new FileTableViewerSorter());
		this.tbvFiles.setUseHashlookup(true);
		this.tbvFiles.setInput(this.fileTableElements);

		String[] columnNames = new String[] {
			"", "Size", "Folder" };
		int[] columnAlignments = new int[] {
			SWT.LEFT, SWT.RIGHT, SWT.LEFT, };
		int[] columnWidths = new int[] {
			48, 100, 100 };
		boolean[] isColumnResizable = new boolean[] {
			false, true, true };

		for (int col = 0; col < columnNames.length; col++) {
			TableColumn tableColumn = new TableColumn(this.tbvFiles.getTable(), columnAlignments[col]);
			tableColumn.setText(columnNames[col]);
			tableColumn.setResizable(isColumnResizable[col]);
			tableColumn.setWidth(columnWidths[col]);
		}
		autoSizeViewerColumns(this.tbvFiles, this.tabItemFiles);

		addOpenFolderActionContextMenu(this.tbvFiles);

		return composite;
	}

	private void tbvFilesCheckStateChanged(CheckStateChangedEvent event) {
		FileTableElement fElement = (FileTableElement) event.getElement();

		boolean isGrayed = DuplicateFilesAndFolders.this.tbvFiles.getGrayed(fElement);
		if (isGrayed) {
			DuplicateFilesAndFolders.this.tbvFiles.refresh(fElement);
			return;
		}

		this.tbvFiles.getTable().setRedraw(false);

		fElement.setChecked(event.getChecked());

		String strHash = fElement.getNode().getHash();
		List<Node> duplicateNodes = DuplicateFilesAndFolders.this.mapDuplicates.get(strHash);

		boolean isWarning = true;
		for (Node duplicateNode : duplicateNodes) {
			boolean isChecked = DuplicateFilesAndFolders.this.nodeToFileTableElement.get(duplicateNode).isChecked();
			if (isChecked == false) {
				isWarning = false;
				break;
			}
		}
		for (Node duplicateNode : duplicateNodes) {
			FileTableElement fElement2 = DuplicateFilesAndFolders.this.nodeToFileTableElement.get(duplicateNode);
			fElement2.setWarning(isWarning);
			DuplicateFilesAndFolders.this.tbvFiles.refresh(fElement2);
		}

		this.tbvFiles.getTable().setRedraw(true);

		updateMessage();
	}

	private void updateViewers() {
		this.tbvFiles.setInput(this.fileTableElements);
		this.tbvFolders.setInput(this.folderTableElements);
	}

	private void addOpenFolderActionContextMenu(final TableViewer tbvViewer) {
		final OpenFolderAction openFolderAction = new OpenFolderAction();

		final MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				IStructuredSelection selection = tbvViewer.getStructuredSelection();
				if (selection.isEmpty() == false) {
					Object firstElement = selection.getFirstElement();
					String path = ((TableElement) selection.getFirstElement()).getPath();
					if (firstElement instanceof FileTableElement) {
						openFolderAction.setText("Open Containing Folder");
					} else if (firstElement instanceof FolderTableElement) {
						openFolderAction.setText("Open Folder");
					}
					openFolderAction.setPath(path);
					menuManager.add(openFolderAction);
				}
			}
		});
		tbvViewer.getControl().setMenu(menuManager.createContextMenu(tbvViewer.getControl()));
	}

	private void updateMessage() {
		boolean isWarningDuplicateFolder = false;
		for (FolderTableElement fElement : this.folderTableElements) {
			if (fElement.isWarning()) {
				isWarningDuplicateFolder = true;
				break;
			}
		}

		boolean isWarningDuplicateFile = false;
		for (FileTableElement fElement : this.fileTableElements) {
			if (fElement.isWarning()) {
				isWarningDuplicateFile = true;
				break;
			}
		}

		if (isWarningDuplicateFolder || isWarningDuplicateFile) {
			StringBuffer sb = new StringBuffer();
			sb.append("You are about to delete all copies of ");
			if (isWarningDuplicateFile && (isWarningDuplicateFolder == false)) {
				sb.append("a file");
			} else if ((isWarningDuplicateFile == false) && isWarningDuplicateFolder) {
				sb.append("a folder");
			} else if (isWarningDuplicateFile && isWarningDuplicateFolder) {
				sb.append("a folder and a file");
			}
			sb.append(".");
			this.warningLabel.setMessage(sb.toString(), Type.WARNING);
		} else {
			this.warningLabel.setMessage(null, Type.WARNING);
		}

		int numFolders = 0;
		for (FolderTableElement fElement : this.folderTableElements) {
			if (fElement.isChecked()) {
				numFolders++;
			}
		}

		int numFiles = 0;
		for (FileTableElement fElement : this.fileTableElements) {
			if (fElement.isChecked()) {
				numFiles++;
			}
		}

		int numItems = numFiles + numFolders;
		if (numItems > 0) {
			long selectedFileSize = Utils.calcSelectedFilesSizeRecursively(this.rootNode, this.nodeToFolderTableElement, this.nodeToFileTableElement);
			float percentage = ((float) selectedFileSize / this.totalFilesSize) * 100;

			StringBuffer sb = new StringBuffer();
			sb.append("You have selected ");
			sb.append(String.format("%s %s ", Utils.formatCount(numItems), (numItems == 1) ? "item" : "items"));
			sb.append("(");
			if (numFolders == 0) {
				sb.append(String.format("%s %s", Utils.formatCount(numFiles), (numFiles == 1) ? "file" : "files"));
			} else if (numFiles == 0) {
				sb.append(String.format("%s %s", Utils.formatCount(numFolders), (numFolders == 1) ? "folder" : "folders"));
			} else {
				sb.append(String.format("%s %s and %s %s", Utils.formatCount(numFolders), (numFolders == 1) ? "folder" : "folders", Utils.formatCount(numFiles), (numFiles == 1) ? "file" : "files"));
			}
			sb.append(") ");
			sb.append("containing ");
			String formatSelectedFileSize = Utils.formatMemorySize(selectedFileSize);
			String formatTotalFileSize = Utils.formatMemorySize(this.totalFilesSize);
			String formatPercentage = Utils.formatPercentage(percentage);
			sb.append(String.format("%s of %s (%s%%).", formatSelectedFileSize, formatTotalFileSize, formatPercentage));
			this.infoLabel.setMessage(sb.toString(), Type.INFO);
		} else {
			this.infoLabel.setMessage(null, null);
		}

		this.btnDelete.setEnabled(numItems > 0);
	}

	private void createInfoAndDeleteButton(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(composite);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite);

		this.infoLabel = new WrappedLabel(composite);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(this.infoLabel.getControl());

		this.btnDelete = new Button(composite, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).hint(Utils.getButtonSize(this.btnDelete)).applyTo(this.btnDelete);
		this.btnDelete.setText("Delete...");
		this.btnDelete.setEnabled(false);
		this.btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteSelected();
			}
		});
	}

	private void deleteSelected() {
		final DeleteConfirmationDialog dialog = new DeleteConfirmationDialog(getShell(), this.rootNode, this.folderTableElements, this.fileTableElements, this.nodeToFolderTableElement, this.nodeToFileTableElement);
		final int[] result = new int[1];

		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			@Override
			public void run() {
				result[0] = dialog.open();
			}
		});

		if (result[0] == Window.OK) {
			updateUi();
		}
	}

	private void updateUi() {
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			@Override
			public void run() {
				hashFoldersRecursively(DuplicateFilesAndFolders.this.rootNode);
				calculateTotalFilesSize();
				clearViewers();
				searchDuplicates(DuplicateFilesAndFolders.this.rootNode);
				populateViewers();
				updateMessage();
			}
		});
	}

	//////////////////////////////////////////////////////////////////////////////

	private static class FolderTableLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		@Override
		public Image getColumnImage(Object element, int index) {
			FolderTableElement fElement = (FolderTableElement) element;
			if (index == 0) {
				if (fElement.isGrayed()) {
					return fElement.isWarning() ? IMG_FOLDER_WARNING_DIS : IMG_FOLDER_DIS;
				}
				return fElement.isWarning() ? IMG_FOLDER_WARNING : IMG_FOLDER;
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int index) {
			FolderTableElement fElement = (FolderTableElement) element;
			switch (index) {
				case 0:
					return "";
				case 1:
					return Utils.formatMemorySize(fElement.getTotalSize());
				case 2:
					return Utils.formatCount(fElement.getTotalChildrenCount());
				case 3:
					return fElement.getPath();
				default:
					return null;
			}
		}

		@Override
		public Color getBackground(Object element, int index) {
			FolderTableElement fElement = (FolderTableElement) element;
			return ((fElement.getDuplicatesGroupIndex() % 2) == 0) ? COLOR_EVEN_LINE : COLOR_ODD_LINE;
		}

		@Override
		public Color getForeground(Object element, int index) {
			FolderTableElement fElement = (FolderTableElement) element;
			return fElement.isGrayed() ? COLOR_GREY : COLOR_BLACK;
		}
	}

	//////////////////////////////////////////////////////////////////////////////

	private static class FolderTableViewerSorter extends ViewerSorter {
		@Override
		public int compare(Viewer viewer, Object o1, Object o2) {
			FolderTableElement fElement1 = (FolderTableElement) o1;
			FolderTableElement fElement2 = (FolderTableElement) o2;

			int diffSize = (int) (fElement2.getTotalSize() - fElement1.getTotalSize());
			if (diffSize != 0) {
				return diffSize;
			}

			int diffTotalChildrenCount = fElement2.getTotalChildrenCount() - fElement1.getTotalChildrenCount();
			if (diffTotalChildrenCount != 0) {
				return diffTotalChildrenCount;
			}

			int diffDuplicateGroup = fElement1.getDuplicatesGroupIndex() - fElement2.getDuplicatesGroupIndex();
			if (diffDuplicateGroup != 0) {
				return diffDuplicateGroup;
			}

			return Collator.getInstance().compare(fElement1.getPath(), fElement2.getPath());
		}
	}

	//////////////////////////////////////////////////////////////////////////////

	private static class TableElement {
		protected Node node;
		private int duplicatesGroupIndex;
		private boolean isChecked;
		private boolean isGrayed;
		private boolean isWarning;

		public TableElement(Node node, int duplicatesGroupIndex) {
			this.node = node;
			this.duplicatesGroupIndex = duplicatesGroupIndex;
			this.isChecked = false;
			this.isGrayed = false;
			this.isWarning = false;
		}

		public Node getNode() {
			return this.node;
		}

		public String getPath() {
			return this.node.getFile().getAbsolutePath();
		}

		public int getDuplicatesGroupIndex() {
			return this.duplicatesGroupIndex;
		}

		public void setChecked(boolean isChecked) {
			this.isChecked = isChecked;
		}

		public boolean isChecked() {
			return this.isChecked;
		}

		public void setGrayed(boolean isGrayed) {
			this.isGrayed = isGrayed;
		}

		public boolean isGrayed() {
			return this.isGrayed;
		}

		public void setWarning(boolean isWarning) {
			this.isWarning = isWarning;
		}

		public boolean isWarning() {
			return this.isWarning;
		}
	}

	//////////////////////////////////////////////////////////////////////////////

	public static class FolderTableElement extends TableElement {

		public FolderTableElement(Node node, int duplicateGroupIndex) {
			super(node, duplicateGroupIndex);
		}

		public long getTotalSize() {
			return this.node.getSize();
		}

		public int getTotalChildrenCount() {
			return this.node.getTotalChildrenCount();
		}
	}

	//////////////////////////////////////////////////////////////////////////////

	private static class FileTableLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {
		@Override
		public Image getColumnImage(Object element, int index) {
			FileTableElement fElement = (FileTableElement) element;
			if (index == 0) {
				if (fElement.isGrayed()) {
					return fElement.isWarning() ? IMG_FILE_WARNING_DIS : IMG_FILE_DIS;
				}
				return fElement.isWarning() ? IMG_FILE_WARNING : IMG_FILE;
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int index) {
			FileTableElement fElement = (FileTableElement) element;
			switch (index) {
				case 0:
					return "";
				case 1:
					return Utils.formatMemorySize(fElement.getSize());
				case 2:
					return fElement.getPath();
				default:
					return null;
			}
		}

		@Override
		public Color getBackground(Object element, int index) {
			FileTableElement fElement = (FileTableElement) element;
			return ((fElement.getDuplicatesGroupIndex() % 2) == 0) ? COLOR_EVEN_LINE : COLOR_ODD_LINE;
		}

		@Override
		public Color getForeground(Object element, int index) {
			FileTableElement fElement = (FileTableElement) element;
			return fElement.isGrayed() ? COLOR_GREY : COLOR_BLACK;
		}
	}

	//////////////////////////////////////////////////////////////////////////////

	private static class FileTableViewerSorter extends ViewerSorter {
		@Override
		public int compare(Viewer viewer, Object o1, Object o2) {
			FileTableElement fElement1 = (FileTableElement) o1;
			FileTableElement fElement2 = (FileTableElement) o2;

			int diffSize = (int) (fElement2.getSize() - fElement1.getSize());
			if (diffSize != 0) {
				return diffSize;
			}

			int diffDuplicatesGroup = fElement1.getDuplicatesGroupIndex() - fElement2.getDuplicatesGroupIndex();
			if (diffDuplicatesGroup != 0) {
				return diffDuplicatesGroup;
			}

			return Collator.getInstance().compare(fElement1.getPath(), fElement2.getPath());
		}
	}

	//////////////////////////////////////////////////////////////////////////////

	public static class FileTableElement extends TableElement {

		public FileTableElement(Node node, int duplicatesGroupIndex) {
			super(node, duplicatesGroupIndex);
		}

		public long getSize() {
			return this.node.getSize();
		}
	}

	//////////////////////////////////////////////////////////////////////////////

	private class OpenFolderAction extends Action {
		private String path;

		public OpenFolderAction() {
			super("");
		}

		public void setPath(String path) {
			this.path = path;
		}

		@Override
		public void run() {
			File file = new File(this.path);
			String cmdLine = Utils.getExplorerCommandLine(file);
			try {
				Runtime.getRuntime().exec(cmdLine);
			} catch (IOException e) {
				MessageDialog.openError(getShell(), "Cannot Open Folder", "An input/output problem occured when opening folder \"" + this.path + "\" in a file browser. Anyway, it is safe to proceed.");
			}
		}
	}

	//----------------------------------------------------------------------------

	public static void main(String[] args) {
		new DuplicateFilesAndFolders().run();
	}

	public void run() {
		setBlockOnOpen(true);
		open();
		Display.getCurrent().dispose();
	}
}
