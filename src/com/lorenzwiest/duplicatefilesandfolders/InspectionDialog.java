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

package com.lorenzwiest.duplicatefilesandfolders;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

public class InspectionDialog extends AppDialog {
	final private static int INDENT = DuplicateFilesAndFolders.INDENT;
	final private static int INDENT2 = DuplicateFilesAndFolders.INDENT * 2;

	private int numItems;
	private Node rootNode;
	private InspectionRunnable inspectionRunnable;

	private Composite compositeStackLayout;
	private StackLayout stackLayout;
	private Composite compositePage1;
	private Label lblCountedItems;
	private Composite compositePage2;
	private Label lblItemName;
	private Label lblRemainingItems;
	private Label lblRemainingTime;
	private ProgressBar pbHashedItems;

	public InspectionDialog(Shell shell, String srcFolder) {
		super(shell);
		this.rootNode = new Node(new File(srcFolder));
	}

	public Node getRootNode() {
		return this.rootNode;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setImage(DuplicateFilesAndFolders.IMG_ICON);
	}

	@Override
	protected Point getPreferredSize() {
		return new Point(500, SWT.DEFAULT);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(INDENT2, INDENT2).spacing(INDENT2, INDENT).applyTo(composite);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite);

		Label lblIcon = new Label(composite, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.TOP).applyTo(lblIcon);
		lblIcon.setImage(Display.getCurrent().getSystemImage(SWT.ICON_WORKING));

		createRightSide(composite);

		return composite;
	}

	private void createRightSide(Composite parent) {
		this.compositeStackLayout = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(this.compositeStackLayout);

		this.stackLayout = new StackLayout();
		this.compositeStackLayout.setLayout(this.stackLayout);

		this.compositePage1 = createPage1(this.compositeStackLayout);
		this.compositePage2 = createPage2(this.compositeStackLayout);
		this.stackLayout.topControl = this.compositePage1;
	}

	private Composite createPage1(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().margins(0, 0).applyTo(composite);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite);

		Composite composite1 = new Composite(composite, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(composite1);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite1);

		Label lblLabelCountedItems = new Label(composite1, SWT.NONE);
		lblLabelCountedItems.setText("Items counted:");
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(lblLabelCountedItems);

		this.lblCountedItems = new Label(composite1, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(this.lblCountedItems);

		Composite composite2 = new Composite(composite, SWT.NONE);
		GridLayoutFactory.swtDefaults().margins(0, 0).applyTo(composite2);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite2);

		new Label(composite2, SWT.NONE);

		Composite composite3 = new Composite(composite, SWT.NONE);
		GridLayoutFactory.swtDefaults().margins(0, 0).applyTo(composite3);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite3);

		new Label(composite3, SWT.NONE);

		Composite composite4 = new Composite(composite, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).spacing(INDENT2, SWT.DEFAULT).applyTo(composite4);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite4);

		ProgressBar pbCountedItems = new ProgressBar(composite4, SWT.INDETERMINATE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(pbCountedItems);

		Button btnCancel = new Button(composite4, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(Utils.getButtonSize(btnCancel)).applyTo(btnCancel);
		btnCancel.setText("Cancel");
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cancelSelected();
			}
		});

		getShell().setDefaultButton(btnCancel);

		return composite;
	}

	private Composite createPage2(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().margins(0, 0).applyTo(composite);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite);

		Composite composite1 = new Composite(composite, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(composite1);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite1);

		Label lblLabelItemName = new Label(composite1, SWT.NONE);
		lblLabelItemName.setText("Name:");
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(lblLabelItemName);

		this.lblItemName = new Label(composite1, SWT.LEFT);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(this.lblItemName);

		Composite composite2 = new Composite(composite, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(composite2);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite2);

		Label lblLabelRemainingItems = new Label(composite2, SWT.NONE);
		lblLabelRemainingItems.setText("Items remaining:");
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(lblLabelRemainingItems);

		this.lblRemainingItems = new Label(composite2, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(this.lblRemainingItems);

		Composite composite3 = new Composite(composite, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(composite3);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite3);

		Label lblLabelRemainingTime = new Label(composite3, SWT.NONE);
		lblLabelRemainingTime.setText("Time remaining:");
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(lblLabelRemainingTime);

		this.lblRemainingTime = new Label(composite3, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(this.lblRemainingTime);

		Composite composite4 = new Composite(composite, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).spacing(INDENT2, SWT.DEFAULT).applyTo(composite4);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(composite4);

		this.pbHashedItems = new ProgressBar(composite4, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(this.pbHashedItems);
		this.pbHashedItems.setMinimum(0);

		Button btnCancel = new Button(composite4, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(Utils.getButtonSize(btnCancel)).applyTo(btnCancel);
		btnCancel.setText("Cancel");
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cancelSelected();
			}
		});

		return composite;
	}

	private void cancelSelected() {
		this.inspectionRunnable.stop();
		close();
	}

	@Override
	public int open() {
		this.inspectionRunnable = new InspectionRunnable(this, this.rootNode);
		new Thread(this.inspectionRunnable).start();
		super.open();

		if (this.inspectionRunnable.isRunning()) {
			this.inspectionRunnable.stop();
		}
		return this.inspectionRunnable.isStopped() ? Window.CANCEL : Window.OK;
	}

	void beginCountItems() {
		if (getShell().isDisposed() == false) {
			getShell().setText("Count Items");
		}
	}

	void updateCountItems(Node item) {
		this.numItems++;
		String strCountedItems = String.format("%,d", this.numItems);
		if (this.lblCountedItems.isDisposed() == false) {
			this.lblCountedItems.setText(strCountedItems);
		}
	}

	void endCountItems() {
		// unused
	}

	private int numRemainingItems;
	private long startTimerMillis;
	private long lastTimerMillis;
	private long remainingSize;

	void beginHashItems() {
		if ((getShell() != null) && (getShell().isDisposed() == false)) {
			getShell().setText("Inspect Items");
		}

		if (this.pbHashedItems.isDisposed() == false) {
			this.pbHashedItems.setMinimum(0);
			this.pbHashedItems.setMaximum(this.numItems);
			this.pbHashedItems.setSelection(0);
		}

		if (this.compositePage2.isDisposed() == false) {
			this.stackLayout.topControl = this.compositePage2;
		}
		if (this.compositeStackLayout.isDisposed() == false) {
			this.compositeStackLayout.layout();
		}

		this.numRemainingItems = this.numItems;
		this.startTimerMillis = System.currentTimeMillis();
		this.lastTimerMillis = this.startTimerMillis;
		this.remainingSize = this.inspectionRunnable.getTotalFilesSize();
	}

	void updateHashItems(Node item) {
		String strNameHashedItem = item.getFile().getAbsolutePath();
		if (this.lblItemName.isDisposed() == false) {
			this.lblItemName.setText(strNameHashedItem);
		}

		String strRemainingItems = String.format("%,d (%s)", this.numRemainingItems, Utils.formatMemorySize(this.remainingSize));
		if (this.lblRemainingItems.isDisposed() == false) {
			this.lblRemainingItems.setText(strRemainingItems);
		}
		this.numRemainingItems--;
		if (item.getFile().isFile()) {
			this.remainingSize -= item.getSize();
		}

		final int UPDATE_INTERVAL_MILLIS = 1000;

		long currentMillis = System.currentTimeMillis();
		if ((currentMillis - this.lastTimerMillis) > UPDATE_INTERVAL_MILLIS) {
			this.lastTimerMillis = this.lastTimerMillis + UPDATE_INTERVAL_MILLIS;

			long elapsedTimeMillis = currentMillis - this.startTimerMillis;
			float remainingTimeMillis = (((float) this.numItems / (this.numItems - this.numRemainingItems)) - 1) * elapsedTimeMillis;
			String remainingTime = Utils.formatTime((int) remainingTimeMillis);
			String strRemainingTime = String.format("About %s", remainingTime);

			if (this.lblRemainingTime.isDisposed() == false) {
				this.lblRemainingTime.setText(strRemainingTime);
			}
		}

		if (this.pbHashedItems.isDisposed() == false) {
			this.pbHashedItems.setSelection(this.pbHashedItems.getSelection() + 1);
		}
	}

	void endHashItems() {
		if ((this.getShell() != null) && (this.getShell().isDisposed() == false)) {
			close();
		}
	}
}

////////////////////////////////////////////////////////////////////////////////

class InspectionRunnable implements Runnable {
	// never call UI code here!

	private InspectionDialog dialog;
	private Node rootNode;
	private long totalFilesSize = 0;
	private MessageDigest messageDigest;
	private boolean isStopped = false;
	private boolean isRunning = false;

	public InspectionRunnable(InspectionDialog dialog, Node rootNode) {
		this.dialog = dialog;
		this.rootNode = rootNode;
		try {
			this.messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public long getTotalFilesSize() {
		return this.totalFilesSize;
	}

	public void stop() {
		this.isStopped = true;
	}

	public boolean isStopped() {
		return this.isStopped;
	}

	public boolean isRunning() {
		return this.isRunning;
	}

	@Override
	public void run() {
		this.isRunning = true;

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				InspectionRunnable.this.dialog.beginCountItems();
			}
		});

		traverseNodesRecursively(this.rootNode);
		if (this.isStopped()) {
			return;
		}

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				InspectionRunnable.this.dialog.endCountItems();
				InspectionRunnable.this.dialog.beginHashItems();
			}
		});

		hashFilesRecursively(this.rootNode);
		if (this.isStopped()) {
			return;
		}

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				InspectionRunnable.this.dialog.endHashItems();
			}
		});

		this.isRunning = false;
	}

	private Node traverseNodesRecursively(final Node node) {
		if (isStopped()) {
			return null;
		}

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				InspectionRunnable.this.dialog.updateCountItems(node);
			}
		});

		File file = node.getFile();
		if (file.isFile()) {
			long size = file.length();
			node.setSize(size);
			this.totalFilesSize += size;
			return node;
		}

		List<File> folders = new ArrayList<File>();
		List<File> files = new ArrayList<File>();

		File[] filesAndFolders = file.listFiles();
		if (filesAndFolders != null) { // may happen for some folders...
			for (File tmpFile : filesAndFolders) {
				if (tmpFile.isDirectory()) {
					folders.add(tmpFile);
				} else if (tmpFile.isFile()) {
					files.add(tmpFile);
				}
			}
		}

		final Comparator<File> filenameComparator = new Comparator<File>() {
			final private Collator collator = Collator.getInstance();

			@Override
			public int compare(File file1, File file2) {
				return this.collator.compare(file1.getName(), file2.getName());
			}
		};

		Collections.sort(folders, filenameComparator);
		Collections.sort(files, filenameComparator);

		List<File> sortedFilesAndFolders = new ArrayList<File>();
		sortedFilesAndFolders.addAll(folders);
		sortedFilesAndFolders.addAll(files);

		for (File sortedTmpFile : sortedFilesAndFolders) {
			Node childNode = new Node(sortedTmpFile);
			node.addChild(childNode);
			traverseNodesRecursively(childNode);
		}
		return node;
	}

	private void hashFilesRecursively(final Node node) {
		if (isStopped()) {
			return;
		}

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				InspectionRunnable.this.dialog.updateHashItems(node);
			}
		});

		File file = node.getFile();
		if (file.isFile()) {
			node.setMd5Hash(hashFile(file));
			return;
		}

		for (Node child : node.getChildren()) {
			hashFilesRecursively(child);
		}
	}

	private String hashFile(File file) {
		final int SIZE = 4096;

		InputStream in = null;
		try {
			this.messageDigest.reset();
			in = new BufferedInputStream(new FileInputStream(file.getAbsolutePath()));
			byte[] buffer = new byte[SIZE];
			while (true) {
				if (isStopped()) {
					return null;
				}

				int b = in.read(buffer);
				if (b == -1) {
					break;
				}
				this.messageDigest.update(buffer, 0, buffer.length);
			}
		} catch (FileNotFoundException e) {
			// should never happen
		} catch (IOException e) {
			System.out.println("IOException with file " + file.getAbsolutePath());
		} finally {
			closeGracefully(in);
		}

		return Utils.digestToString(this.messageDigest);
	}

	private static void closeGracefully(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}