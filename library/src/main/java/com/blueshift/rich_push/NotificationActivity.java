package com.blueshift.rich_push;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;

import com.blueshift.Blueshift;

public class NotificationActivity extends AppCompatActivity {

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mContext = this;

        Message message = (Message) getIntent().getSerializableExtra(RichPushConstants.EXTRA_MESSAGE);

        if (message != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(message.getBody());
            builder.setTitle(message.getTitle());

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

            builder = setActions(builder, message);

            builder.create().show();
        } else {
            finish();
        }
    }


    private AlertDialog.Builder setActions(AlertDialog.Builder builder, final Message message) {
        switch (message.getCategory()) {
            case AlertBoxOpenDismiss:
                builder.setPositiveButton("Open", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        PackageManager packageManager = getPackageManager();
                        Intent launcherIntent  = packageManager.getLaunchIntentForPackage(getPackageName());
                        launcherIntent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
                        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launcherIntent);

                        Blueshift.getInstance(mContext).trackNotificationPageOpen(message.id, true);

                        finish();
                    }
                });

                builder.setNegativeButton("Dismiss", null);

                break;

            case AlertBoxDismiss:
                builder.setNegativeButton("Dismiss", null);

                break;
        }

        return builder;
    }
}
