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

import java.util.LinkedHashSet;
import java.util.Map;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.lorenzwiest.duplicatefilesandfolders.DuplicateFilesAndFolders.FileTableElement;
import de.lorenzwiest.duplicatefilesandfolders.DuplicateFilesAndFolders.FolderTableElement;

public class DeleteConfirmationDialog extends AppDialog {
	private final static int INDENT = DuplicateFilesAndFolders.INDENT;
	private final static int INDENT2 = DuplicateFilesAndFolders.INDENT * 2;
	private final static String CR = System.getProperty("line.separator");

	private Node rootNode;
	private FolderTableElement[] folderTableElements;
	private FileTableElement[] fileTableElements;
	private Map<Node, FolderTableElement> nodeToFolderTableElement;
	private Map<Node, FileTableElement> nodeToFileTableElement;
	private long totalFilesSize;

	private boolean isCancelled = true;
	private LinkedHashSet<Node> itemsToDelete;

	private Button chkConfirmDeletion;
	private Button btnDelete;

	public DeleteConfirmationDialog(Shell shell, Node rootNode, FolderTableElement[] folderTableElements, FileTableElement[] fileTableElements, Map<Node, FolderTableElement> nodeToFolderTableElement, Map<Node, FileTableElement> nodeToFileTableElement) {
		super(shell);
		this.rootNode = rootNode;
		this.folderTableElements = folderTableElements;
		this.fileTableElements = fileTableElements;
		this.nodeToFolderTableElement = nodeToFolderTableElement;
		this.nodeToFileTableElement = nodeToFileTableElement;
		this.totalFilesSize = Utils.calcTotalFilesSizeRecursively(rootNode);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Confirm Deleting Items");
		shell.setImage(DuplicateFilesAndFolders.IMG_ICON);
		shell.setMinimumSize(new Point(350, 300));
	}

	@Override
	protected Point getPreferredSize() {
		return new Point(500, 500);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(INDENT2, INDENT2).spacing(INDENT2, INDENT).applyTo(composite);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(composite);

		Label lblIcon = new Label(composite, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.TOP).applyTo(lblIcon);
		lblIcon.setImage(Display.getCurrent().getSystemImage(SWT.ICON_QUESTION));

		createRightSide(composite);

		new Label(composite, SWT.NONE);

		createButtons(composite);

		return parent;
	}

	private void createRightSide(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().margins(0, 0).spacing(INDENT2, INDENT2).applyTo(composite);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(composite);

		Label lblText = new Label(composite, SWT.WRAP);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.BEGINNING, SWT.TOP).applyTo(lblText);
		lblText.setText(getInfoText());

		this.chkConfirmDeletion = new Button(composite, SWT.CHECK);
		this.chkConfirmDeletion.setText("Delete items on disk (cannot be undone)");
		this.chkConfirmDeletion.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				confirmDeletionSelected();
			}
		});

		createScrolledComposite(composite);
	}

	private void createScrolledComposite(Composite parent) {
		final ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(scrolledComposite);
		GridLayoutFactory.swtDefaults().margins(0, 0).applyTo(scrolledComposite);

		final Composite composite = new Composite(scrolledComposite, SWT.NONE);
		scrolledComposite.setContent(composite);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(composite);
		GridLayoutFactory.swtDefaults().margins(0, 0).applyTo(composite);

		this.itemsToDelete = new LinkedHashSet<Node>();
		addItemsToDeleteRecursively(this.rootNode, this.itemsToDelete);
		StringBuffer sb = new StringBuffer();
		for (Node itemToDelete : this.itemsToDelete) {
			sb.append(itemToDelete.getFile().getAbsolutePath());
			sb.append(CR);
		}
		StyledText text = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(text);
		text.setText(sb.toString());
	}

	private void createButtons(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).extendedMargins(0, 0, INDENT, 0).spacing(INDENT2, SWT.DEFAULT).applyTo(composite);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.END, SWT.CENTER).applyTo(composite);

		this.btnDelete = new Button(composite, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.END, SWT.CENTER).hint(Utils.getButtonSize(this.btnDelete)).applyTo(this.btnDelete);
		this.btnDelete.setText("Delete");
		this.btnDelete.setEnabled(false);
		this.btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteSelected();
			}
		});

		Button btnCancel = new Button(composite, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).hint(Utils.getButtonSize(btnCancel)).applyTo(btnCancel);
		btnCancel.setText("Cancel");
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cancelSelected();
			}
		});

		getShell().setDefaultButton(btnCancel);
	}

	private String getInfoText() {
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

		long selectedFileSize = Utils.calcSelectedFilesSizeRecursively(this.rootNode, this.nodeToFolderTableElement, this.nodeToFileTableElement);
		float percentage = ((float) selectedFileSize / this.totalFilesSize) * 100;
		int numItems = numFiles + numFolders;

		StringBuffer sb = new StringBuffer();
		sb.append("You are about to delete ");
		sb.append(String.format("%s selected %s ", Utils.formatCount(numItems), (numItems == 1) ? "item" : "items"));
		sb.append("(");
		if (numFolders == 0) {
			sb.append(String.format("%s %s", Utils.formatCount(numFiles), (numFiles == 1) ? "file" : "files"));
		} else if (numFiles == 0) {
			sb.append(String.format("%s %s", Utils.formatCount(numFolders), (numFolders == 1) ? "folder" : "folders"));
		} else {
			sb.append(String.format("%s %s and %s %s", Utils.formatCount(numFolders), (numFolders == 1) ? "folder" : "folders", Utils.formatCount(numFiles), (numFiles == 1) ? "file" : "files"));
		}
		sb.append(").");
		sb.append(CR);
		sb.append(CR);
		sb.append("This will recover ");
		String formatSelectedFileSize = Utils.formatMemorySize(selectedFileSize);
		String formatTotalFileSize = Utils.formatMemorySize(this.totalFilesSize);
		String formatPercentage = Utils.formatPercentage(percentage);
		sb.append(String.format("%s of %s (%s%%).", formatSelectedFileSize, formatTotalFileSize, formatPercentage));

		return sb.toString();
	}

	private void confirmDeletionSelected() {
		this.btnDelete.setEnabled(this.chkConfirmDeletion.getSelection());
	}

	private void addItemsToDeleteRecursively(Node node, LinkedHashSet<Node> itemsToDelete) {
		if (node.getFile().isFile()) {
			if (this.nodeToFileTableElement.containsKey(node)) {
				if (this.nodeToFileTableElement.get(node).isChecked()) {
					itemsToDelete.add(node);
				}
			}
			return;
		}

		for (Node childNode : node.getChildren()) {
			addItemsToDeleteRecursively(childNode, itemsToDelete);
		}

		if (this.nodeToFolderTableElement.containsKey(node)) {
			if (this.nodeToFolderTableElement.get(node).isChecked()) {
				itemsToDelete.add(node);
			}
		}
		return;
	}

	@Override
	public int open() {
		super.open();
		return this.isCancelled() ? Window.CANCEL : Window.OK;
	}

	private boolean isCancelled() {
		return this.isCancelled;
	}

	private void cancelSelected() {
		this.isCancelled = true;
		close();
	}

	private void deleteSelected() {
		close();

		DeleteDialog dialog = new DeleteDialog(this.getShell(), this.rootNode, this.itemsToDelete);
		dialog.open();

		LinkedHashSet<Node> itemsNotDeleted = dialog.getItemsNotDeleted();
		if (itemsNotDeleted.size() > 0) {
			new NotDeletedItemsDialog(this.getShell(), itemsNotDeleted).open();
		}

		this.itemsToDelete.removeAll(itemsNotDeleted);
		removeDeletedChildNodesRecursively(this.rootNode, this.itemsToDelete);

		this.isCancelled = false;
	}

	// itemsToDelete in delete-depth-first order!
	private void removeDeletedChildNodesRecursively(Node node, LinkedHashSet<Node> itemsToDelete) {
		if (node.getFile().isFile()) {
			return;
		}

		Node[] copyOfChildNodes = node.getChildren().toArray(new Node[0]);
		for (Node childNode : copyOfChildNodes) {
			removeDeletedChildNodesRecursively(childNode, itemsToDelete);
		}

		for (Node childNode : copyOfChildNodes) {
			if (itemsToDelete.contains(childNode)) {
				node.getChildren().remove(childNode);
			}
		}
	}
}