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
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import de.lorenzwiest.duplicatefilesandfolders.WrappedLabel.Type;

public class DuplicateFilesAndFolders extends ApplicationWindow {
	public static final int INDENT = 5;
	private static final int INDENT2 = INDENT * 2;

	private static final Color COLOR_EVEN_LINE = new Color(Display.getCurrent(), 240, 240, 255);
	private static final Color COLOR_ODD_LINE = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
	private static final Color COLOR_BLACK = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
	private static final Color COLOR_GREY = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);

	public static final Image IMG_ICON = Utils.readImage("icons/icon.png");
	private static final Image IMG_FOLDER = Utils.readImage("icons/folder.png");
	private static final Image IMG_FOLDER_WARNING = Utils.readImage("icons/folder.warning.png");
	private static final Image IMG_FILE = Utils.readImage("icons/file.png");
	private static final Image IMG_FILE_WARNING = Utils.readImage("icons/file.warning.png");
	private static final Image IMG_FOLDER_DIS = Utils.readImage("icons/folder_disabled.png");
	private static final Image IMG_FOLDER_WARNING_DIS = Utils.readImage("icons/folder.warning_disabled.png");
	private static final Image IMG_FILE_DIS = Utils.readImage("icons/file_disabled.png");
	private static final Image IMG_FILE_WARNING_DIS = Utils.readImage("icons/file.warning_disabled.png");
	private static final Image IMG_MENU = Utils.readImage("icons/menu.png");

	private MessageDigest messageDigest;
	private Node rootNode;
	private Map<String /* md5 hash */, List<Node>> mapDuplicates = new HashMap<String, List<Node>>();
	private FolderTableElement[] folderTableElements = new FolderTableElement[0];
	private FileTableElement[] fileTableElements = new FileTableElement[0];
	private Map<Node, FolderTableElement> nodeToFolderTableElement = new HashMap<Node, FolderTableElement>();
	private Map<Node, FileTableElement> nodeToFileTableElement = new HashMap<Node, FileTableElement>();

	private Text txtFolderToScan;
	private Button btnFindDuplicates;
	private WrappedLabel warningLabel;
	private CTabItem tabItemFolders;
	private CheckboxTableViewer tbvFolders;
	private CTabItem tabItemFiles;
	private CheckboxTableViewer tbvFiles;
	private WrappedLabel infoLabel;
	private Button btnDelete;
	private CTabFolder tabFolder;
	private MenuItem itemSelectAll;
	private MenuItem itemSelectAllButOneOfEachDuplicate;
	private MenuItem itemDeselectAll;
	private MenuItem itemInvertSelection;

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
		shell.setText("Duplicate Files & Folders 1.3");
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

	private long getTotalFilesSize() {
		return this.rootNode.getSize();
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
		CTabFolder tabFolder = this.tabItemFolders.getParent();
		CTabItem selectedTab = tabFolder.getSelection();
		if ((selectedTab == this.tabItemFolders) && (this.folderTableElements.length == 0)) {
			if (this.fileTableElements.length > 0) {
				tabFolder.setSelection(this.tabItemFiles);
			}
		} else if ((selectedTab == this.tabItemFiles) && (this.fileTableElements.length == 0)) {
			if (this.folderTableElements.length > 0) {
				tabFolder.setSelection(this.tabItemFolders);
			}
		}
		updatePopUpMenu();
	}

	private void autoSizeViewerColumns() {
		autoSizeViewerColumns(this.tbvFolders, this.tabItemFolders);
		autoSizeViewerColumns(this.tbvFiles, this.tabItemFiles);
	}

	private void autoSizeViewerColumns(final TableViewer viewer, final CTabItem tabItem) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				Table table = viewer.getTable();
				table.setRedraw(false);

				// don't resize last column, it will flex as needed
				for (int col = 0; col < (table.getColumnCount() - 1); col++) {
					TableColumn tableColumn = table.getColumn(col);
					if (tableColumn.getResizable()) {
						tableColumn.pack();
					}
				}

				// hack to update non-visible tabs
				CTabItem saveSelection = tabItem.getParent().getSelection();
				tabItem.getParent().setSelection(tabItem);
				if (saveSelection != null) {
					tabItem.getParent().setSelection(saveSelection);
				}

				table.setRedraw(true);
			}
		});
	}

	private void createMessageField(Composite parent) {
		this.warningLabel = new WrappedLabel(parent);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(this.warningLabel.getControl());
	}

	private void createTabFolder(Composite parent) {
		this.tabFolder = new CTabFolder(parent, SWT.BORDER);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(this.tabFolder);

		createPopUpMenu(this.tabFolder);

		this.tabItemFolders = new CTabItem(this.tabFolder, SWT.NONE);
		this.tabItemFolders.setText("Duplicate Folders");
		this.tabItemFolders.setImage(IMG_FOLDER);
		this.tabItemFolders.setControl(createTabItemDuplicateFoldersContent(this.tabFolder));

		this.tabItemFiles = new CTabItem(this.tabFolder, SWT.NONE);
		this.tabItemFiles.setText("Duplicate Files");
		this.tabItemFiles.setImage(IMG_FILE);
		this.tabItemFiles.setControl(createTabItemDuplicateFilesContent(this.tabFolder));
	}

	private void createPopUpMenu(CTabFolder tabFolder) {
		final ToolBar toolBar = new ToolBar(tabFolder, SWT.FLAT);
		final ToolItem toolItem = new ToolItem(toolBar, SWT.PUSH);
		tabFolder.setTopRight(toolBar, SWT.RIGHT);

		final Menu menu = new Menu(getShell(), SWT.POP_UP);

		this.itemSelectAll = new MenuItem(menu, SWT.PUSH);
		this.itemSelectAll.setText("Select All");
		this.itemSelectAll.setEnabled(false);
		this.itemSelectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				itemSelectAllSelected();
			}
		});

		this.itemSelectAllButOneOfEachDuplicate = new MenuItem(menu, SWT.PUSH);
		this.itemSelectAllButOneOfEachDuplicate.setText("Select All But One of Each Duplicate");
		this.itemSelectAllButOneOfEachDuplicate.setEnabled(false);
		this.itemSelectAllButOneOfEachDuplicate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				itemSelectAllButOneOfEachItemSelected();
			}
		});

		this.itemDeselectAll = new MenuItem(menu, SWT.PUSH);
		this.itemDeselectAll.setText("Deselect All");
		this.itemDeselectAll.setEnabled(false);
		this.itemDeselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				itemDeselectAllSelected();
			}
		});

		this.itemInvertSelection = new MenuItem(menu, SWT.PUSH);
		this.itemInvertSelection.setText("Invert Selection");
		this.itemInvertSelection.setEnabled(false);
		this.itemInvertSelection.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				itemInvertSelectionSelected();
			}
		});

		toolItem.setImage(IMG_MENU);
		toolItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Rectangle rect = toolItem.getBounds();
				Point point = new Point(rect.x, rect.y + rect.height);
				point = toolBar.toDisplay(point);
				menu.setLocation(point.x, point.y);
				menu.setVisible(true);
			}
		});

		tabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updatePopUpMenu();
			}
		});

		tabFolder.setTabHeight(Math.max(toolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).y, tabFolder.getTabHeight()));
	}

	private void itemSelectAllSelected() {
		final CTabItem selectedTab = this.tabFolder.getSelection();
		final CTabItem tabItemFolders = this.tabItemFolders;
		final CheckboxTableViewer tbv = (selectedTab == this.tabItemFolders) ? this.tbvFolders : this.tbvFiles;

		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			@Override
			public void run() {
				for (TableItem tableItem : tbv.getTable().getItems()) { // tableItem(s) are in visual sort order
					TableElement tableElement = (TableElement) tableItem.getData();
					CheckStateChangedEvent event = new CheckStateChangedEvent(tbv, tableElement, true);
					if (selectedTab == tabItemFolders) {
						tbvFoldersCheckStateChanged(event);
					} else {
						tbvFilesCheckStateChanged(event);
					}
				}
			}
		});
	}

	private void itemSelectAllButOneOfEachItemSelected() {
		final CTabItem selectedTab = this.tabFolder.getSelection();
		final CTabItem tabItemFolders = this.tabItemFolders;
		final CheckboxTableViewer tbv = (selectedTab == this.tabItemFolders) ? this.tbvFolders : this.tbvFiles;

		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			@Override
			public void run() {
				String strHash = "";
				for (TableItem tableItem : tbv.getTable().getItems()) { // tableItem(s) are in visual sort order
					TableElement tableElement = (TableElement) tableItem.getData();
					if (tableElement.isGrayed()) {
						continue;
					}

					String hashToCompare = tableElement.getNode().getHash();
					boolean isSelected = hashToCompare.equals(strHash);
					if (isSelected == false) {
						strHash = hashToCompare;
					}

					CheckStateChangedEvent event = new CheckStateChangedEvent(tbv, tableElement, isSelected);
					if (selectedTab == tabItemFolders) {
						tbvFoldersCheckStateChanged(event);
					} else {
						tbvFilesCheckStateChanged(event);
					}
				}
			}
		});
	}

	private void itemDeselectAllSelected() {
		final CTabItem selectedTab = this.tabFolder.getSelection();
		final CTabItem tabItemFolders = this.tabItemFolders;
		final CheckboxTableViewer tbv = (selectedTab == this.tabItemFolders) ? this.tbvFolders : this.tbvFiles;

		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			@Override
			public void run() {
				for (TableItem tableItem : tbv.getTable().getItems()) { // tableItem(s) are in visual sort order
					TableElement tableElement = (TableElement) tableItem.getData();
					CheckStateChangedEvent event = new CheckStateChangedEvent(tbv, tableElement, false);
					if (selectedTab == tabItemFolders) {
						tbvFoldersCheckStateChanged(event);
					} else {
						tbvFilesCheckStateChanged(event);
					}
				}
			}
		});
	}

	private void itemInvertSelectionSelected() {
		final CTabItem selectedTab = this.tabFolder.getSelection();
		final CTabItem tabItemFolders = this.tabItemFolders;
		final CheckboxTableViewer tbv = (selectedTab == this.tabItemFolders) ? this.tbvFolders : this.tbvFiles;

		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			@Override
			public void run() {
				for (TableItem tableItem : tbv.getTable().getItems()) { // tableItem(s) are in visual sort order
					TableElement tableElement = (TableElement) tableItem.getData();
					boolean isChecked = tableElement.isChecked();
					CheckStateChangedEvent event = new CheckStateChangedEvent(tbv, tableElement, !isChecked);
					if (selectedTab == tabItemFolders) {
						tbvFoldersCheckStateChanged(event);
					} else {
						tbvFilesCheckStateChanged(event);
					}
				}
			}
		});
	}

	private void updatePopUpMenu() {
		CTabItem selectedTab = this.tabFolder.getSelection();
		TableElement[] tableElements = (selectedTab == this.tabItemFolders) ? this.folderTableElements : this.fileTableElements;
		boolean isEnabled = (tableElements.length > 0);
		this.itemSelectAll.setEnabled(isEnabled);
		this.itemSelectAllButOneOfEachDuplicate.setEnabled(isEnabled);
		this.itemDeselectAll.setEnabled(isEnabled);
		this.itemInvertSelection.setEnabled(isEnabled);
	}

	private Control createTabItemDuplicateFoldersContent(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		composite.setLayout(tableColumnLayout);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite);

		this.tbvFolders = CheckboxTableViewer.newCheckList(composite, SWT.FULL_SELECTION);
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

		this.tbvFolders.setContentProvider(new ArrayContentProvider());
		this.tbvFolders.setComparator(new FolderTableViewerComparator());
		this.tbvFolders.setUseHashlookup(true);
		this.tbvFolders.setInput(this.folderTableElements);

		Table table = this.tbvFolders.getTable();
		TableColumn tc0 = new TableColumn(table, SWT.LEFT);
		tc0.setText("");
		tableColumnLayout.setColumnData(tc0, new ColumnPixelData(48));
		tc0.setResizable(false);
		TableViewerColumn tbvColumn0 = new TableViewerColumn(this.tbvFolders, tc0);
		tbvColumn0.setLabelProvider(new FolderTableLabelProvider(0));

		TableColumn tc1 = new TableColumn(table, SWT.RIGHT);
		tc1.setText("Total Size");
		tableColumnLayout.setColumnData(tc1, new ColumnPixelData(10));
		tc1.setResizable(true);
		TableViewerColumn tbvColumn1 = new TableViewerColumn(this.tbvFolders, tc1);
		tbvColumn1.setLabelProvider(new FolderTableLabelProvider(1));

		TableColumn tc2 = new TableColumn(table, SWT.RIGHT);
		tc2.setText("Items");
		tableColumnLayout.setColumnData(tc2, new ColumnPixelData(10));
		tc2.setResizable(true);
		TableViewerColumn tbvColumn2 = new TableViewerColumn(this.tbvFolders, tc2);
		tbvColumn2.setLabelProvider(new FolderTableLabelProvider(2));

		TableColumn tc3 = new TableColumn(table, SWT.LEFT);
		tc3.setText("Folder");
		tableColumnLayout.setColumnData(tc3, new ColumnWeightData(10));
		tc3.setResizable(true);
		TableViewerColumn tbvColumn3 = new TableViewerColumn(this.tbvFolders, tc3);
		tbvColumn3.setLabelProvider(new FolderTableLabelProvider(3));

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
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		composite.setLayout(tableColumnLayout);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite);

		this.tbvFiles = CheckboxTableViewer.newCheckList(composite, SWT.FULL_SELECTION);
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

		this.tbvFiles.setContentProvider(new ArrayContentProvider());
		this.tbvFiles.setComparator(new FileTableViewerComparator());
		this.tbvFiles.setUseHashlookup(true);
		this.tbvFiles.setInput(this.fileTableElements);

		Table table = this.tbvFiles.getTable();

		TableColumn tc0 = new TableColumn(table, SWT.LEFT);
		tc0.setText("");
		tableColumnLayout.setColumnData(tc0, new ColumnPixelData(48));
		tc0.setResizable(false);
		TableViewerColumn tbvColumn0 = new TableViewerColumn(this.tbvFiles, tc0);
		tbvColumn0.setLabelProvider(new FileTableLabelProvider(0));

		TableColumn tc1 = new TableColumn(table, SWT.RIGHT);
		tc1.setText("Size");
		tableColumnLayout.setColumnData(tc1, new ColumnPixelData(100));
		tc1.setResizable(true);
		TableViewerColumn tbvColumn1 = new TableViewerColumn(this.tbvFiles, tc1);
		tbvColumn1.setLabelProvider(new FileTableLabelProvider(1));

		TableColumn tc2 = new TableColumn(table, SWT.LEFT);
		tc2.setText("Folder");
		tableColumnLayout.setColumnData(tc2, new ColumnWeightData(1));
		tc2.setResizable(true);
		TableViewerColumn tbvColumn2 = new TableViewerColumn(this.tbvFiles, tc2);
		tbvColumn2.setLabelProvider(new FileTableLabelProvider(2));

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
		updatePopUpMenu();
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
			String strMessage = null;
			if (isWarningDuplicateFile && (isWarningDuplicateFolder == false)) {
				strMessage = "You are about to delete all copies of at least a file";
			} else if ((isWarningDuplicateFile == false) && isWarningDuplicateFolder) {
				strMessage = "You are about to delete all copies of at least a folder";
			} else if (isWarningDuplicateFile && isWarningDuplicateFolder) {
				strMessage = "You are about to delete all copies of at least a folder and a file";
			}
			this.warningLabel.setMessage(strMessage, Type.WARNING);
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
			long selectedFileSize = Utils.calculateSelectedFilesSize(this.folderTableElements, this.fileTableElements);
			long totalFilesSize = getTotalFilesSize();
			float percentage = ((float) selectedFileSize / totalFilesSize) * 100;

			StringBuffer sb = new StringBuffer();
			String strSelectedFileSize = Utils.formatMemorySize(selectedFileSize);
			String strTotalFileSize = Utils.formatMemorySize(totalFilesSize);
			String strPercentage = Utils.formatPercentage(percentage);
			String strItems = (numItems == 1) ? "item" : "items";
			String strNumItems = Utils.formatCount(numItems);
			if (numFolders == 0) {
				String strFiles = (numFiles == 1) ? "file" : "files";
				String strNumFiles = Utils.formatCount(numFiles);
				sb.append(String.format("You have selected %s %s (%s %s) containing %s of %s (%s%%).", strNumItems, strItems, strNumFiles, strFiles, strSelectedFileSize, strTotalFileSize, strPercentage));
			} else if (numFiles == 0) {
				String strFolders = (numFolders == 1) ? "folder" : "folders";
				String strNumFolders = Utils.formatCount(numFolders);
				sb.append(String.format("You have selected %s %s (%s %s) containing %s of %s (%s%%).", strNumItems, strItems, strNumFolders, strFolders, strSelectedFileSize, strTotalFileSize, strPercentage));
			} else {
				String strFolders = (numFolders == 1) ? "folder" : "folders";
				String strNumFolders = Utils.formatCount(numFolders);
				String strFiles = (numFiles == 1) ? "file" : "files";
				String strNumFiles = Utils.formatCount(numFiles);
				sb.append(String.format("You have selected %s %s (%s %s and %s %s) containing %s of %s (%s%%).", strNumItems, strItems, strNumFolders, strFolders, strNumFiles, strFiles, strSelectedFileSize, strTotalFileSize, strPercentage));
			}
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
				clearViewers();
				searchDuplicates(DuplicateFilesAndFolders.this.rootNode);
				populateViewers();
				updateMessage();
			}
		});
	}

	//////////////////////////////////////////////////////////////////////////////

	private static class FolderTableLabelProvider extends ColumnLabelProvider {
		private int columnIndex;

		public FolderTableLabelProvider(int columnIndex) {
			this.columnIndex = columnIndex;
		}

		@Override
		public String getText(Object element) {
			FolderTableElement fElement = (FolderTableElement) element;
			switch (this.columnIndex) {
				case 1:
					return Utils.formatMemorySize(fElement.getTotalSize());
				case 2:
					return Utils.formatCount(fElement.getTotalChildrenCount());
				case 3:
					return fElement.getPath();
				default:
					return "";
			}
		}

		@Override
		public Image getImage(Object element) {
			FolderTableElement fElement = (FolderTableElement) element;
			if (this.columnIndex == 0) {
				if (fElement.isGrayed()) {
					return fElement.isWarning() ? IMG_FOLDER_WARNING_DIS : IMG_FOLDER_DIS;
				}
				return fElement.isWarning() ? IMG_FOLDER_WARNING : IMG_FOLDER;
			}
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			FolderTableElement fElement = (FolderTableElement) element;
			return ((fElement.getDuplicatesGroupIndex() % 2) == 0) ? COLOR_EVEN_LINE : COLOR_ODD_LINE;
		}

		@Override
		public Color getForeground(Object element) {
			FolderTableElement fElement = (FolderTableElement) element;
			return fElement.isGrayed() ? COLOR_GREY : COLOR_BLACK;
		}
	}

	//////////////////////////////////////////////////////////////////////////////

	private static class FolderTableViewerComparator extends ViewerComparator {
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

	protected static class TableElement {
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

	private static class FileTableLabelProvider extends ColumnLabelProvider {
		private int columnIndex;

		public FileTableLabelProvider(int columnIndex) {
			this.columnIndex = columnIndex;
		}

		@Override
		public String getText(Object element) {
			FileTableElement fElement = (FileTableElement) element;
			switch (this.columnIndex) {
				case 1:
					return Utils.formatMemorySize(fElement.getSize());
				case 2:
					return fElement.getPath();
				default:
					return "";
			}
		}

		@Override
		public Image getImage(Object element) {
			FileTableElement fElement = (FileTableElement) element;
			if (this.columnIndex == 0) {
				if (fElement.isGrayed()) {
					return fElement.isWarning() ? IMG_FILE_WARNING_DIS : IMG_FILE_DIS;
				}
				return fElement.isWarning() ? IMG_FILE_WARNING : IMG_FILE;
			}
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			FileTableElement fElement = (FileTableElement) element;
			return ((fElement.getDuplicatesGroupIndex() % 2) == 0) ? COLOR_EVEN_LINE : COLOR_ODD_LINE;
		}

		@Override
		public Color getForeground(Object element) {
			FileTableElement fElement = (FileTableElement) element;
			return fElement.isGrayed() ? COLOR_GREY : COLOR_BLACK;
		}
	}

	//////////////////////////////////////////////////////////////////////////////

	private static class FileTableViewerComparator extends ViewerComparator {
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
