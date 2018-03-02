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
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;

import de.lorenzwiest.duplicatefilesandfolders.DuplicateFilesAndFolders.TableElement;

public class Utils {
	final private static int MINUTE = 60;
	final private static int HOUR = 60 * 60;

	public static String formatTime(int millis) {
		int seconds = millis / 1000;

		StringBuffer sb = new StringBuffer();
		if (seconds < MINUTE) {
			sb.append(seconds);
			sb.append(" ");
			sb.append(seconds == 1 ? "second" : "seconds");
		} else if (seconds < (5 * MINUTE)) {
			// give minutes and 5 seconds units
			int minutes = seconds / MINUTE;
			sb.append(minutes);
			sb.append(" ");
			sb.append(minutes == 1 ? "minute" : "minutes");

			int remainingSeconds = ((seconds - (minutes * MINUTE)) / 5) * 5;
			if (remainingSeconds != 0) {
				sb.append(" and ");
				sb.append(remainingSeconds);
				sb.append(" seconds");
			}
		} else if (seconds < HOUR) {
			int minutes = seconds / MINUTE;
			sb.append(minutes);
			sb.append(" ");
			sb.append(minutes == 1 ? "minute" : "minutes");
		} else if (seconds < (2 * HOUR)) {
			// give hours and 5 minute units
			int hours = seconds / HOUR;
			sb.append(hours);
			sb.append(" ");
			sb.append(hours == 1 ? "hour" : "hours");

			int minutes = ((seconds - (hours * HOUR)) / MINUTE / 5) * 5;
			if (minutes != 0) {
				sb.append(" and ");
				sb.append(minutes);
				sb.append(" minutes");
			}
		} else if (seconds < (5 * HOUR)) {
			// give hours and quarters
			int hours = seconds / HOUR;
			sb.append(hours);
			sb.append(" hours");

			int minutes = ((seconds - (hours * HOUR)) / MINUTE / 15) * 15;
			if (minutes != 0) {
				sb.append(" and ");
				sb.append(minutes);
				sb.append(" minutes");
			}
		} else {
			// give full hours
			int hours = seconds / HOUR;
			sb.append(hours);
			sb.append(" hours");
		}
		return sb.toString();
	}

	final private static long KB = 1024L;
	final private static long MB = 1024L * 1024;
	final private static long GB = 1024L * 1024 * 1024;
	final private static long TB = 1024L * 1024 * 1024 * 1024;

	final private static DecimalFormat DECIMAL_FORMAT1 = new DecimalFormat(".0", DecimalFormatSymbols.getInstance(Locale.US));
	final private static DecimalFormat DECIMAL_FORMAT2 = new DecimalFormat(".00", DecimalFormatSymbols.getInstance(Locale.US));

	public static String formatMemorySize(long size) {
		DECIMAL_FORMAT1.setRoundingMode(RoundingMode.DOWN);
		DECIMAL_FORMAT2.setRoundingMode(RoundingMode.DOWN);

		if (size < KB) {
			return size + " Bytes";
		} else if (size < (10 * KB)) {
			return String.format("%s KB", DECIMAL_FORMAT2.format((double) size / KB));
		} else if (size < MB) {
			return String.format("%s KB", DECIMAL_FORMAT1.format((double) size / KB));
		} else if (size < (10 * MB)) {
			return String.format("%s MB", DECIMAL_FORMAT2.format((double) size / MB));
		} else if (size < GB) {
			return String.format("%s MB", DECIMAL_FORMAT1.format((double) size / MB));
		} else if (size < (10 * GB)) {
			return String.format("%s GB", DECIMAL_FORMAT2.format((double) size / GB));
		} else if (size < TB) {
			return String.format("%s GB", DECIMAL_FORMAT1.format((double) size / GB));
		} else if (size < (10 * TB)) {
			return String.format("%s TB", DECIMAL_FORMAT2.format((double) size / TB));
		} else {
			return String.format("%s TB", DECIMAL_FORMAT1.format((double) size / TB));
		}
	}

	final private static DecimalFormat COUNT_FORMAT = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));

	public static String formatCount(long count) {
		return String.format("%s", COUNT_FORMAT.format(count));
	}

	final private static DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.US));

	public static String formatPercentage(float percentage) {
		return String.format("%s", PERCENTAGE_FORMAT.format(percentage));
	}

	public static Point getButtonSize(Button button) {
		GC gc = new GC(button);
		gc.setFont(button.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		int widthHint = Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_WIDTH);
		gc.dispose();

		Point defaultSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		return new Point(Math.max(widthHint, defaultSize.x), SWT.DEFAULT);
	}

	public static String getHash(File file) {
		final String strEmptyMd5Hash = "--------------------------------";
		return getHashInternal(file, strEmptyMd5Hash);
	}

	public static String getHash(File file, MessageDigest messageDigest) {
		return getHashInternal(file, String.format("%032x", new BigInteger(1, messageDigest.digest())));
	}

	private static String getHashInternal(File file, String strMd5Hash) {
		String strHash = null;
		if (file.isFile()) {
			strHash = String.format("%016x%s", file.length(), strMd5Hash);
		} else {
			final String strEmptyLength = "----------------";
			strHash = String.format("%s%s", strEmptyLength, strMd5Hash);
		}
		return strHash;
	}

	public static long calculateSelectedFilesSize(TableElement[] folderTableElements, TableElement[] fileTableElements) {
		long size = 0;

		for (TableElement tableElement : folderTableElements) {
			if (tableElement.isGrayed()) {
				continue;
			}
			if (tableElement.isChecked()) {
				size += tableElement.getNode().getSize();
			}
		}

		for (TableElement tableElement : fileTableElements) {
			if (tableElement.isGrayed()) {
				continue;
			}
			if (tableElement.isChecked()) {
				size += tableElement.getNode().getSize();
			}
		}
		return size;
	}

	public static String getInitialFolderToScanPath() {
		// adjust this to your computer platform
		return "C:\\";
	}

	public static String getExplorerCommandLine(File file) {
		// adjust this to your computer platform
		if (file.isFile()) {
			return String.format("explorer /e,/select,\"%s\"", file.getAbsolutePath());
		} else {
			return String.format("explorer /e,\"%s\"", file.getAbsolutePath());
		}
	}
}