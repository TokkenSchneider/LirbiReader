package com.foobnix.pdf.info.widget;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.ebookdroid.common.settings.SettingsManager;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.pdf.info.ExportSettingsManager;
import com.foobnix.pdf.info.ExtFilter;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.reader.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.presentation.BrowserAdapter;
import com.foobnix.pdf.info.presentation.PathAdapter;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.ui2.MainTabs2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PrefDialogs {

	private static String lastPaht;

	private static String getLastPath() {
		try {
			return lastPaht = lastPaht != null && new File(lastPaht).isDirectory() ? lastPaht
					: Environment.getExternalStorageDirectory().getPath();
		} catch (Exception e) {
			return lastPaht = "/";
		}
	}

	public static void chooseFolderDialog(final FragmentActivity a, final Runnable onChanges, final Runnable onScan) {

		final PathAdapter recentAdapter = new PathAdapter();
		recentAdapter.setPaths(AppState.getInstance().searchPaths);

		final AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setTitle(R.string.scan_device_for_new_books);

		final ListView list = new ListView(a);

		list.setAdapter(recentAdapter);

		builder.setView(list);

        builder.setPositiveButton(R.string.search, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				onScan.run();
			}
		});

		builder.setNeutralButton(R.string.add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				ChooserDialogFragment.chooseFolder(a, AppState.get().dirLastPath).setOnSelectListener(new ResultResponse2<String, Dialog>() {
					@Override
					public boolean onResultRecive(String nPath, Dialog dialog) {
						boolean isExists = false;
						String existPath = "";
						for (String str : AppState.getInstance().searchPaths.split(",")) {
							if (str != null && str.trim().length() != 0 && nPath.equals(str)) {
								isExists = true;
								existPath = str;
								break;
							}
						}
						if (isExists) {
							Toast.makeText(a,
									String.format("[ %s == %s ] %s", nPath, existPath,
											a.getString(R.string.this_directory_is_already_in_the_list)),
									Toast.LENGTH_LONG).show();
						} else {
							if (AppState.getInstance().searchPaths.endsWith(",")) {
								AppState.getInstance().searchPaths = AppState.getInstance().searchPaths + "" + nPath;
							} else {
								AppState.getInstance().searchPaths = AppState.getInstance().searchPaths + "," + nPath;
							}
						}
						dialog.dismiss();
						onChanges.run();
						chooseFolderDialog(a, onChanges, onScan);
						return false;
					}

				});

			}
		});

		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});
		recentAdapter.setOnDeleClick(new ResultResponse<Uri>() {

			@Override
			public boolean onResultRecive(Uri result) {
				if (AppState.getInstance().searchPaths.split(",").length > 1) {
					String path = result.getPath();
					LOG.d("TEST", "Remove " + AppState.getInstance().searchPaths);
					LOG.d("TEST", "Remove " + path);
					StringBuilder builder = new StringBuilder();
					for (String str : AppState.getInstance().searchPaths.split(",")) {
						if (str != null && str.trim().length() > 0 && !str.equals(path)) {
							builder.append(str);
							builder.append(",");
						}
					}
					AppState.getInstance().searchPaths = builder.toString();
					LOG.d("TEST", "Remove " + AppState.getInstance().searchPaths);
					recentAdapter.setPaths(AppState.getInstance().searchPaths);
					onChanges.run();

				}
				return false;
			}
		});

		builder.show();
	}

	private void navigationDialog(final FragmentActivity a, File currentDirectory, final Runnable onChanges,
			final Runnable onScan) {

		if (true) {
			return;
		}

		final BrowserAdapter adapter = new BrowserAdapter(a, new ExtFilter(ExtUtils.browseExts));
		adapter.setCurrentDirectory(currentDirectory);

		final AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setTitle(R.string.choose_);

		final TextView text = new TextView(a);
		text.setText(currentDirectory.getPath());
		int p = Dips.dpToPx(5);
		text.setPadding(p, p, p, p);
		text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
		text.setTextSize(16);
		text.setSingleLine();
		text.setEllipsize(TruncateAt.END);

		final ListView list = new ListView(a);
		list.setMinimumHeight(1000);
		list.setMinimumWidth(600);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final File file = new File(adapter.getItem(position).getPath());
				if (file.isDirectory()) {
					lastPaht = file.getPath();
					adapter.setCurrentDirectory(file);
					text.setText(file.getPath());
					list.setSelection(0);

				}
			}
		});

		LinearLayout inflate = (LinearLayout) LayoutInflater.from(a).inflate(R.layout.frame_layout, null, false);

		list.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
		text.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));

		ImageView home = new ImageView(a);
		home.setImageResource(R.drawable.glyphicons_21_home);
		TintUtil.setTintImage(home);
		home.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final File file = Environment.getExternalStorageDirectory();
				lastPaht = file.getPath();
				adapter.setCurrentDirectory(file);
				text.setText(file.getPath());
				list.setSelection(0);
			}
		});

		inflate.addView(home);

		inflate.addView(text);
		inflate.addView(list);
		builder.setView(inflate);

		builder.setPositiveButton(R.string.select, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				String nPath = text.getText().toString();

				boolean isExists = false;
				String existPath = "";
				for (String str : AppState.getInstance().searchPaths.split(",")) {
					if (str != null && str.trim().length() != 0 && nPath.equals(str)) {
						isExists = true;
						existPath = str;
						break;
					}
				}
				if (isExists) {
					Toast.makeText(a,
							String.format("[ %s == %s ] %s", nPath, existPath,
									a.getString(R.string.this_directory_is_already_in_the_list)),
							Toast.LENGTH_LONG).show();
				} else {
					if (AppState.getInstance().searchPaths.endsWith(",")) {
						AppState.getInstance().searchPaths = AppState.getInstance().searchPaths + "" + nPath;
					} else {
						AppState.getInstance().searchPaths = AppState.getInstance().searchPaths + "," + nPath;
					}
				}

				chooseFolderDialog(a, onChanges, onScan);
			}
		});

		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();

	}

	public static void importDialog(final FragmentActivity activity) {
		String sampleName = ExportSettingsManager.getInstance(activity).getSampleJsonConfigName(activity, ".JSON.txt");

		ChooserDialogFragment.chooseFile(activity, sampleName)
				.setOnSelectListener(new ResultResponse2<String, Dialog>() {

					@Override
					public boolean onResultRecive(String result1, Dialog result2) {
						boolean result = ExportSettingsManager.getInstance(activity).importAll(new File(result1));

						if (result) {
							Toast.makeText(activity,
									activity.getString(R.string.import_) + " " + activity.getString(R.string.success),
									Toast.LENGTH_LONG).show();
							AppState.get().loadIn(activity);
                            BookCSS.get().load(activity);
							TintUtil.init();
							SettingsManager.clearCache();
							Intent i = new Intent(activity, MainTabs2.class);
							i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
							activity.startActivity(i);
						} else {
							Toast.makeText(activity,
									activity.getString(R.string.import_) + " " + activity.getString(R.string.fail),
									Toast.LENGTH_LONG).show();
						}
						result2.dismiss();
						return false;
					}
				});

	}

	public static void exportDialog(final FragmentActivity activity) {
		String sampleName = ExportSettingsManager.getInstance(activity).getSampleJsonConfigName(activity,
				"Export-All.JSON.txt");
		ChooserDialogFragment.createFile(activity, sampleName)
				.setOnSelectListener(new ResultResponse2<String, Dialog>() {

					@Override
					public boolean onResultRecive(String result1, Dialog result2) {
						File toFile = new File(result1);
						if (toFile == null || toFile.getName().trim().length() == 0) {
							Toast.makeText(activity, "Invalid File name", Toast.LENGTH_SHORT).show();
							return false;
						}

						boolean result = ExportSettingsManager.getInstance(activity).exportAll(toFile);

						if (result) {
							Toast.makeText(activity,
									activity.getString(R.string.export_) + " " + activity.getString(R.string.success),
									Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(activity,
									activity.getString(R.string.export_) + " " + activity.getString(R.string.fail),
									Toast.LENGTH_LONG).show();
						}
						result2.dismiss();
						return false;
					}
				});

	}

	private void imExDialog(final Activity a, final int resSelectId, final String sampleName,
			final ResultResponse<File> onResult) {
		List<String> browseexts = Arrays.asList(".json", ".txt", ".ttf", ".otf");

		final BrowserAdapter adapter = new BrowserAdapter(a, new ExtFilter(browseexts));
		adapter.setCurrentDirectory(new File(getLastPath()));

		final AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setTitle(R.string.choose_);

		final EditText text = new EditText(a);

		text.setText(sampleName);
		int p = Dips.dpToPx(5);
		text.setPadding(p, p, p, p);
		text.setSingleLine();
		text.setEllipsize(TruncateAt.END);
		if (resSelectId == R.string.export_) {
			text.setEnabled(true);
		} else {
			text.setEnabled(false);
		}

		final TextView pathText = new TextView(a);
		pathText.setText(new File(getLastPath()).toString());
		pathText.setPadding(p, p, p, p);
		pathText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
		pathText.setTextSize(16);
		pathText.setSingleLine();
		pathText.setEllipsize(TruncateAt.END);

		final ListView list = new ListView(a);
		list.setMinimumHeight(1000);
		list.setMinimumWidth(600);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final File file = new File(adapter.getItem(position).getPath());
				if (file.isDirectory()) {
					lastPaht = file.getPath();
					adapter.setCurrentDirectory(file);
					pathText.setText(file.getPath());
					list.setSelection(0);
				} else {
					pathText.setText(file.getName());
				}
			}
		});

		LinearLayout inflate = (LinearLayout) LayoutInflater.from(a).inflate(R.layout.frame_layout, null, false);

		list.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
		pathText.setLayoutParams(
				new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));

		ImageView home = new ImageView(a);
		home.setImageResource(R.drawable.glyphicons_21_home);
		TintUtil.setTintImage(home);
		home.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final File file = Environment.getExternalStorageDirectory();
				if (file.isDirectory()) {
					lastPaht = file.getPath();
					adapter.setCurrentDirectory(file);
					pathText.setText(file.getPath());
					list.setSelection(0);
				} else {
					pathText.setText(file.getName());
				}

			}
		});

		inflate.addView(home);
		inflate.addView(pathText);
		inflate.addView(list);
		inflate.addView(text);
		builder.setView(inflate);

		builder.setPositiveButton(resSelectId, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				File toFile = null;
				if (resSelectId == R.string.export_) {
					toFile = new File(lastPaht, text.getText().toString());
				} else {
					toFile = new File(lastPaht, pathText.getText().toString());
				}
				onResult.onResultRecive(toFile);
			}
		});

		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	public static void selectFileDialog(final Context a, List<String> browseexts, File path,
			final com.foobnix.android.utils.ResultResponse<String> onChoose) {

		final BrowserAdapter adapter = new BrowserAdapter(a, new ExtFilter(browseexts));
		if (path.isFile()) {
			String absolutePath = path.getAbsolutePath();
			String filePath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
			adapter.setCurrentDirectory(new File(filePath));
		} else {
			adapter.setCurrentDirectory(path);
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setTitle(R.string.choose_);

		final EditText text = new EditText(a);

		text.setText(path.getName());
		int p = Dips.dpToPx(5);
		text.setPadding(p, p, p, p);
		text.setSingleLine();
		text.setEllipsize(TruncateAt.END);
		text.setEnabled(true);

		final TextView pathText = new TextView(a);
		pathText.setText(path.getPath());
		pathText.setPadding(p, p, p, p);
		pathText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
		pathText.setTextSize(16);
		pathText.setSingleLine();
		pathText.setEllipsize(TruncateAt.END);

		final ListView list = new ListView(a);
		list.setMinimumHeight(1000);
		list.setMinimumWidth(600);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final File file = new File(adapter.getItem(position).getPath());
				if (file.isDirectory()) {
					adapter.setCurrentDirectory(file);
					pathText.setText(file.getPath());
					list.setSelection(0);
				} else {
					text.setText(file.getName());
				}
			}
		});

		LinearLayout inflate = (LinearLayout) LayoutInflater.from(a).inflate(R.layout.frame_layout, null, false);

		list.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
		pathText.setLayoutParams(
				new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));

		inflate.addView(pathText);
		inflate.addView(list);
		inflate.addView(text);
		builder.setView(inflate);

		builder.setPositiveButton(R.string.select, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
			}
		});

		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
			}
		});

		final AlertDialog dialog = builder.create();
		dialog.show();
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = text.getText().toString();
				if (name == null || name.trim().length() == 0) {
					Toast.makeText(a, "Invalid File name", Toast.LENGTH_SHORT).show();
					return;
				}
				File toFile = new File(adapter.getCurrentDirectory(), name);

				onChoose.onResultRecive(toFile.getAbsolutePath());
				dialog.dismiss();

			}
		});

	}

}
