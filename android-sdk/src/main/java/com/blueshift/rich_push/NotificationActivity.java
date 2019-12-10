package com.blueshift.rich_push;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Window;

import com.blueshift.Blueshift;
import com.blueshift.R;
import com.blueshift.model.Configuration;

/**
 * Created by Rahul Raveendran
 * <p>
 * This activity is responsible for creating dialog notifications.
 * Currently supports two types of dialogues.
 */
public class NotificationActivity extends AppCompatActivity {

    private Context mContext;
    private Message mMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mContext = this;

        try {
            mMessage = (Message) getIntent().getSerializableExtra(RichPushConstants.EXTRA_MESSAGE);
        } catch (Exception ignore) {
        }

        if (mMessage != null) {
            int theme = 0;

            /**
             * Check if there is a user defined theme, if yes assign it to `theme` variable.
             */
            Configuration configuration = Blueshift.getInstance(mContext).getConfiguration();
            if (configuration != null) {
                theme = configuration.getDialogTheme();
            }

            AlertDialog.Builder builder;

            /**
             * check if a valid `theme` is available, else use `BlueshiftDialogTheme`
             */
            if (theme == 0) {
                builder = new AlertDialog.Builder(mContext, R.style.BlueshiftDialogTheme);
            } else {
                builder = new AlertDialog.Builder(mContext, theme);
            }

            builder.setTitle(mMessage.getContentTitle());
            builder.setMessage(mMessage.getContentText());

            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });

            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            });

            builder = setActions(builder);

            builder.create().show();

            // Tracking the notification display.
            Blueshift.getInstance(mContext).trackNotificationView(mMessage);
        } else {
            finish();
        }
    }

    /**
     * This method adds action buttons to the builder based on available category
     *
     * @param builder builder on which the atcions to be attached.
     * @return updated builder object
     */
    private AlertDialog.Builder setActions(AlertDialog.Builder builder) {
        switch (mMessage.getCategory()) {
            case AlertBoxOpenDismiss:
                builder.setPositiveButton(R.string.dialog_button_open, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Blueshift.getInstance(mContext).trackNotificationClick(mMessage);

                        PackageManager packageManager = getPackageManager();
                        Intent launcherIntent = packageManager.getLaunchIntentForPackage(getPackageName());
                        launcherIntent.putExtra(RichPushConstants.EXTRA_MESSAGE, mMessage);
                        startActivity(launcherIntent);

                        Blueshift.getInstance(mContext).trackNotificationPageOpen(mMessage, false);

                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton(R.string.dialog_button_dismiss, mOnDismissClickListener);

                break;

            case AlertBoxDismiss:
                builder.setNegativeButton(R.string.dialog_button_dismiss, mOnDismissClickListener);

                break;
        }

        return builder;
    }

    private DialogInterface.OnClickListener mOnDismissClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Blueshift.getInstance(mContext).trackAlertDismiss(mMessage, false);

            dialog.dismiss();
        }
    };
}
