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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
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

public class DeleteDialog extends AppDialog {
	private final static int INDENT = DuplicateFilesAndFolders.INDENT;
	private final static int INDENT2 = DuplicateFilesAndFolders.INDENT * 2;

	private LinkedHashSet<Node> itemsToDelete; // items in delete-depth-first order
	private DeleteRunnable deleteRunnable;

	private Label lblItemName;
	private Label lblRemainingItems;
	private Label lblRemainingTime;
	private ProgressBar pbDeletedItems;

	public DeleteDialog(Shell shell, Node rootNode, LinkedHashSet<Node> itemsToDelete) {
		super(shell);
		this.itemsToDelete = itemsToDelete;
	}

	public LinkedHashSet<Node> getItemsNotDeleted() {
		return this.deleteRunnable.getItemsNotDeleted();
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setImage(DuplicateFilesAndFolders.IMG_ICON);
		shell.setText("Delete Items");
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

	private Composite createRightSide(Composite parent) {
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
		lblLabelRemainingItems.setText("Items to delete:");
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

		this.pbDeletedItems = new ProgressBar(composite4, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(this.pbDeletedItems);
		this.pbDeletedItems.setMinimum(0);

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

	private void cancelSelected() {
		this.deleteRunnable.stop();
		close();
	}

	@Override
	public int open() {
		this.deleteRunnable = new DeleteRunnable(this, this.itemsToDelete);
		new Thread(this.deleteRunnable).start();
		super.open();

		if (this.deleteRunnable.isRunning()) {
			this.deleteRunnable.stop();
		}
		return this.deleteRunnable.isStopped() ? Window.CANCEL : Window.OK;
	}

	private int numRemainingItemsToDelete;
	private long startTimerMillis;
	private long lastTimerMillis;

	protected void beginDeleteItems() {
		int numItemsToDelete = this.itemsToDelete.size();

		if (this.pbDeletedItems.isDisposed() == false) {
			this.pbDeletedItems.setMinimum(0);
			this.pbDeletedItems.setMaximum(numItemsToDelete);
			this.pbDeletedItems.setSelection(0);
		}

		this.numRemainingItemsToDelete = numItemsToDelete;
		this.startTimerMillis = System.currentTimeMillis();
		this.lastTimerMillis = this.startTimerMillis;
	}

	protected void updateDeleteItem(Node item) {
		String strNameOfDeletedItem = item.getFile().getAbsolutePath();
		if (this.lblItemName.isDisposed() == false) {
			this.lblItemName.setText(strNameOfDeletedItem);
		}

		String strRemainingItemsToDelete = String.format("%,d", this.numRemainingItemsToDelete);
		if (this.lblRemainingItems.isDisposed() == false) {
			this.lblRemainingItems.setText(strRemainingItemsToDelete);
		}
		this.numRemainingItemsToDelete--;

		int numItemsToDelete = this.itemsToDelete.size();

		final int UPDATE_INTERVAL_MILLIS = 1000;

		long currentMillis = System.currentTimeMillis();
		if ((currentMillis - this.lastTimerMillis) > UPDATE_INTERVAL_MILLIS) {
			this.lastTimerMillis = this.lastTimerMillis + UPDATE_INTERVAL_MILLIS;

			long elapsedTimeMillis = currentMillis - this.startTimerMillis;
			float remainingTimeMillis = (((float) numItemsToDelete / (numItemsToDelete - this.numRemainingItemsToDelete)) - 1) * elapsedTimeMillis;
			String remainingTime = Utils.formatTime((int) remainingTimeMillis);
			String strRemainingTime = String.format("About %s", remainingTime);

			if (this.lblRemainingTime.isDisposed() == false) {
				this.lblRemainingTime.setText(strRemainingTime);
			}
		}

		if (this.pbDeletedItems.isDisposed() == false) {
			this.pbDeletedItems.setSelection(this.pbDeletedItems.getSelection() + 1);
		}
	}

	protected void endDeleteItems() {
		if ((this.getShell() != null) && (this.getShell().isDisposed() == false)) {
			close();
		}
	}
}

////////////////////////////////////////////////////////////////////////////////

class DeleteRunnable implements Runnable {
	// never call UI code here!

	private DeleteDialog dialog;
	private boolean isStopped = false;
	private boolean isRunning = false;

	private LinkedHashSet<Node> itemsToDelete;
	private LinkedHashSet<Node> itemsNotDeleted;

	private Object lock = new Object();

	public DeleteRunnable(DeleteDialog dialog, LinkedHashSet<Node> itemsToDelete) {
		this.dialog = dialog;
		this.itemsToDelete = itemsToDelete;
		this.itemsNotDeleted = new LinkedHashSet<Node>(itemsToDelete);
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

	public LinkedHashSet<Node> getItemsNotDeleted() {
		synchronized (this.lock) {
			return this.itemsNotDeleted;
		}
	}

	@Override
	public void run() {
		this.isRunning = true;

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				DeleteRunnable.this.dialog.beginDeleteItems();
			}
		});

		for (final Node node : this.itemsToDelete) {
			if (this.isStopped()) {
				return;
			}

			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					DeleteRunnable.this.dialog.updateDeleteItem(node);
				}
			});

			deleteItem(node);
		}

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				DeleteRunnable.this.dialog.endDeleteItems();
			}
		});

		this.isRunning = false;
	}

	private void deleteItem(final Node node) {
		synchronized (this.lock) {
			boolean isDeleted = node.getFile().delete();
			if (isDeleted) {
				this.itemsNotDeleted.remove(node);
			}
		}
	}
}
